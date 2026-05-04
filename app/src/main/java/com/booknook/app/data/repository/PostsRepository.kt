package com.booknook.app.data.repository

import android.net.Uri
import androidx.lifecycle.LiveData
import com.booknook.app.data.local.LocalCacheDataSource
import com.booknook.app.data.local.entities.CommentEntity
import com.booknook.app.data.local.entities.LikeEntity
import com.booknook.app.data.local.entities.PostEntity
import com.booknook.app.model.Book
import com.booknook.app.model.StorageModel
import com.booknook.app.model.firebase.FirebaseAuthModel
import com.booknook.app.model.firebase.FirebaseCommentsModel
import com.booknook.app.model.firebase.FirebasePostsCursor
import com.booknook.app.model.firebase.FirebasePostsPage
import com.booknook.app.model.firebase.FirebasePostsModel
import com.booknook.app.model.firebase.FirebaseProfileModel
import com.booknook.app.util.nullIfBlank
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class PostsRepository(
    private val local: LocalCacheDataSource,
    private val auth: FirebaseAuthModel,
    private val profileModel: FirebaseProfileModel,
    private val postsModel: FirebasePostsModel,
    private val commentsModel: FirebaseCommentsModel,
    private val storageModel: StorageModel
) {
    private var lastRefreshTime = 0L
    private var lastCursor: FirebasePostsCursor? = null
    private var canLoadMorePosts = true

    fun observePosts(currentUserId: String): LiveData<List<PostEntity>> = local.observePosts(currentUserId)

    fun observeMyPosts(userId: String, currentUserId: String): LiveData<List<PostEntity>> {
        return local.observeMyPosts(userId, currentUserId)
    }

    fun observePost(postId: String, currentUserId: String): LiveData<PostEntity?> {
        return local.observePost(postId, currentUserId)
    }

    fun searchPostsByQuery(query: String, currentUserId: String): LiveData<List<PostEntity>> {
        return local.searchPostsByQuery(query, currentUserId)
    }

    suspend fun refreshPosts(force: Boolean = false): PostsPageResult {
        val now = System.currentTimeMillis()
        if (shouldSkipRefresh(force, now)) {
            return PostsPageResult(endReached = !canLoadMorePosts)
        }

        resetPagination()
        val currentUserId = auth.currentUserId()
        val page = postsModel.fetchPostsPage(PostRepositoryConstants.PAGE_SIZE, null)

        updatePagination(page)
        cachePostsPage(page.posts, currentUserId, replaceUserLikes = true)
        lastRefreshTime = now
        return PostsPageResult(endReached = !canLoadMorePosts)
    }

    suspend fun refreshMyPosts(userId: String) {
        val posts = postsModel.fetchPostsByUser(userId)
        withContext(Dispatchers.IO) {
            local.upsertPosts(posts)
            local.deleteStalePostsForUser(userId, posts.map { it.id })
        }
    }

    suspend fun createPost(book: Book, rating: Int, review: String, imageUri: Uri?) {
        val userId = auth.requireUserId()
        val username = profileModel.fetchProfile()?.username ?: PostRepositoryConstants.DEFAULT_USERNAME
        val postId = UUID.randomUUID().toString()
        val imageUrl = uploadPostImage(imageUri)
        val post = buildNewPost(postId, userId, username, book, rating, review, imageUrl)

        postsModel.createPost(post)
        withContext(Dispatchers.IO) {
            local.upsertPost(post)
        }
    }

    suspend fun updatePost(postId: String, rating: Int, review: String, imageUri: Uri?) {
        val userId = auth.requireUserId()
        val existing = postsModel.getPost(postId) ?: return
        if (existing.userId != userId) return

        val uploadedImageUrl = uploadPostImage(imageUri)
        val imageUrl = uploadedImageUrl ?: existing.imageUrl.nullIfBlank()
        val updated = existing.copy(rating = rating, review = review, imageUrl = imageUrl)
        postsModel.updatePost(updated)
        withContext(Dispatchers.IO) {
            local.upsertPost(updated)
        }
    }

    suspend fun deletePost(postId: String) {
        postsModel.deletePost(postId)
        withContext(Dispatchers.IO) {
            local.deleteLikesByPost(postId)
            local.deletePost(postId)
        }
    }

    suspend fun toggleLike(postId: String) {
        val userId = auth.currentUserId() ?: return
        val post = withContext(Dispatchers.IO) { local.getPost(postId, userId) } ?: return
        if (post.userId == userId) {
            return
        }

        val isLiked = withContext(Dispatchers.IO) { local.isLikedSync(userId, postId) }
        val newIsLiked = !isLiked
        val optimisticPost = post.withOptimisticLikeCount(newIsLiked)

        withContext(Dispatchers.IO) {
            updateCachedLike(userId, postId, newIsLiked)
            local.upsertPost(optimisticPost)
        }

        try {
            postsModel.toggleLike(postId)
            refreshCachedPost(postId)
        } catch (e: Exception) {
            withContext(Dispatchers.IO) {
                updateCachedLike(userId, postId, isLiked)
                local.upsertPost(post)
            }
            throw e
        }
    }

    fun observeComments(postId: String): LiveData<List<CommentEntity>> = local.observeComments(postId)

    suspend fun refreshComments(postId: String) {
        val comments = commentsModel.fetchComments(postId)
        withContext(Dispatchers.IO) {
            local.replaceComments(postId, comments)
        }
    }

    suspend fun addComment(postId: String, text: String) {
        val userId = auth.currentUserId() ?: return
        val post = withContext(Dispatchers.IO) { local.getPost(postId, userId) } ?: return
        val optimisticPost = post.copy(commentsCount = post.commentsCount + 1)

        withContext(Dispatchers.IO) {
            local.upsertPost(optimisticPost)
        }

        try {
            commentsModel.addComment(postId, text)
            refreshCachedPost(postId)
            refreshComments(postId)
        } catch (e: Exception) {
            withContext(Dispatchers.IO) {
                local.upsertPost(post)
            }
            throw e
        }
    }

    suspend fun loadMorePosts(): PostsPageResult {
        if (!canLoadMorePosts) {
            return PostsPageResult(endReached = true)
        }
        val currentUserId = auth.currentUserId()
        val page = postsModel.fetchPostsPage(PostRepositoryConstants.PAGE_SIZE, lastCursor)
        if (page.posts.isEmpty()) {
            canLoadMorePosts = false
            return PostsPageResult(endReached = true)
        }
        updatePagination(page)
        cachePostsPage(page.posts, currentUserId, replaceUserLikes = false)
        return PostsPageResult(endReached = !canLoadMorePosts)
    }

    private fun shouldSkipRefresh(force: Boolean, now: Long): Boolean {
        return !force && now - lastRefreshTime < PostRepositoryConstants.REFRESH_INTERVAL_MS
    }

    private fun updatePagination(page: FirebasePostsPage) {
        lastCursor = page.cursor
        canLoadMorePosts = page.posts.size.toLong() >= PostRepositoryConstants.PAGE_SIZE
    }

    private suspend fun cachePostsPage(
        posts: List<PostEntity>,
        currentUserId: String?,
        replaceUserLikes: Boolean
    ) = withContext(Dispatchers.IO) {
        local.upsertPosts(posts)
        cacheLikedPosts(posts, currentUserId, replaceUserLikes)
    }

    private suspend fun cacheLikedPosts(
        posts: List<PostEntity>,
        currentUserId: String?,
        replaceUserLikes: Boolean
    ) {
        currentUserId ?: return
        val likes = posts.toLikeEntities(currentUserId)
        if (replaceUserLikes) {
            local.replaceUserLikes(currentUserId, likes)
        } else if (likes.isNotEmpty()) {
            local.upsertLikes(likes)
        }
    }

    private fun List<PostEntity>.toLikeEntities(userId: String): List<LikeEntity> {
        return filter { it.isLikedByUser }.map { post ->
            LikeEntity(userId = userId, postId = post.id)
        }
    }

    private suspend fun uploadPostImage(imageUri: Uri?): String? {
        val imageUrl = imageUri?.let { storageModel.uploadPostImage(it).nullIfBlank() }
        if (imageUri != null && imageUrl == null) {
            throw IllegalStateException("Photo upload failed")
        }
        return imageUrl
    }

    private fun buildNewPost(
        postId: String,
        userId: String,
        username: String,
        book: Book,
        rating: Int,
        review: String,
        imageUrl: String?
    ): PostEntity {
        return PostEntity(
            id = postId,
            userId = userId,
            username = username,
            bookId = book.id,
            bookTitle = book.title,
            bookAuthor = book.author,
            bookThumbnail = book.thumbnail,
            rating = rating,
            review = review,
            imageUrl = imageUrl,
            createdAt = System.currentTimeMillis(),
            likesCount = 0,
            commentsCount = 0,
            bookPublishedDate = book.publishedDate,
            bookGenre = book.genre,
            bookPageCount = book.pageCount,
            bookDescription = book.description
        )
    }

    private fun PostEntity.withOptimisticLikeCount(isLiked: Boolean): PostEntity {
        val likesCount = if (isLiked) likesCount + 1 else (likesCount - 1).coerceAtLeast(0)
        return copy(likesCount = likesCount)
    }

    private suspend fun updateCachedLike(userId: String, postId: String, isLiked: Boolean) {
        if (isLiked) {
            local.upsertLike(LikeEntity(userId, postId))
        } else {
            local.deleteLike(userId, postId)
        }
    }

    private suspend fun refreshCachedPost(postId: String) {
        val freshPost = postsModel.getPost(postId) ?: return
        withContext(Dispatchers.IO) {
            local.upsertPost(freshPost)
        }
    }

    private fun resetPagination() {
        lastCursor = null
        canLoadMorePosts = true
    }
}

data class PostsPageResult(
    val endReached: Boolean
)
