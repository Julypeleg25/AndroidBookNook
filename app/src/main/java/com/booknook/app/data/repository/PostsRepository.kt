package com.booknook.app.data.repository

import android.net.Uri
import androidx.lifecycle.LiveData
import com.booknook.app.data.local.LocalCacheDataSource
import com.booknook.app.data.local.entities.CommentEntity
import com.booknook.app.data.local.entities.LikeEntity
import com.booknook.app.data.local.entities.PostEntity
import com.booknook.app.model.Book
import com.booknook.app.model.StorageModel
import com.booknook.app.model.firebase.FirebaseModel
import com.booknook.app.util.nullIfBlank
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class PostsRepository(
    private val local: LocalCacheDataSource,
    private val firebase: FirebaseModel,
    private val storageModel: StorageModel
) {
    private var lastRefreshTime = 0L
    private var lastDocument: DocumentSnapshot? = null
    private var canLoadMorePosts = true

    fun observePosts(currentUserId: String): LiveData<List<PostEntity>> = local.observePosts(currentUserId)

    fun observeMyPosts(userId: String, currentUserId: String): LiveData<List<PostEntity>> {
        return local.observeMyPosts(userId, currentUserId)
    }

    fun observePost(postId: String, currentUserId: String): LiveData<PostEntity?> {
        return local.observePost(postId, currentUserId)
    }

    suspend fun getPost(postId: String, currentUserId: String): PostEntity? {
        return local.getPost(postId, currentUserId)
    }

    fun searchPostsByQuery(query: String, currentUserId: String): LiveData<List<PostEntity>> {
        return local.searchPostsByQuery(query, currentUserId)
    }

    suspend fun isOwnPost(postId: String, currentUserId: String): Boolean {
        val post = withContext(Dispatchers.IO) { local.getPost(postId, currentUserId) }
        return post?.userId == currentUserId
    }

    suspend fun refreshPosts(force: Boolean = false): PostsPageResult {
        val now = System.currentTimeMillis()
        if (!force && now - lastRefreshTime < REFRESH_INTERVAL) {
            return PostsPageResult(endReached = !canLoadMorePosts)
        }

        resetPagination()
        val currentUserId = firebase.currentUserId()
        val (posts, newLastDoc) = firebase.fetchPostsPage(PAGE_SIZE, null)

        lastDocument = newLastDoc
        canLoadMorePosts = posts.size.toLong() >= PAGE_SIZE

        withContext(Dispatchers.IO) {
            local.upsertPosts(posts)
            if (currentUserId != null) {
                val likes = posts.filter { it.isLikedByUser }.map {
                    LikeEntity(userId = currentUserId, postId = it.id)
                }
                local.replaceUserLikes(currentUserId, likes)
            }
        }
        lastRefreshTime = now
        return PostsPageResult(endReached = !canLoadMorePosts)
    }

    suspend fun refreshMyPosts(userId: String) {
        val posts = firebase.fetchPostsByUser(userId)
        withContext(Dispatchers.IO) {
            local.upsertPosts(posts)
            local.deleteUserPostsNotIn(userId, posts.map { it.id })
        }
    }

    suspend fun createPost(book: Book, rating: Int, review: String, imageUri: Uri?) {
        val userId = firebase.requireUserId()
        val username = firebase.fetchProfile()?.username ?: DEFAULT_USERNAME
        val postId = UUID.randomUUID().toString()
        val imageUrl = imageUri?.let { storageModel.uploadPostImage(userId, postId, it)?.nullIfBlank() }
        if (imageUri != null && imageUrl == null) {
            throw IllegalStateException("Photo upload failed")
        }
        val now = System.currentTimeMillis()

        val post = PostEntity(
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
            createdAt = now,
            likesCount = 0,
            commentsCount = 0,
            bookPublishedDate = book.publishedDate,
            bookGenre = book.genre,
            bookPageCount = book.pageCount,
            bookDescription = book.description
        )

        firebase.createPost(post)
        withContext(Dispatchers.IO) {
            local.upsertPost(post)
        }
    }

    suspend fun updatePost(postId: String, rating: Int, review: String, imageUri: Uri?) {
        val userId = firebase.requireUserId()
        val existing = firebase.getPost(postId) ?: return
        if (existing.userId != userId) return

        val uploadedImageUrl = imageUri?.let { storageModel.uploadPostImage(userId, postId, it)?.nullIfBlank() }
        if (imageUri != null && uploadedImageUrl == null) {
            throw IllegalStateException("Photo upload failed")
        }

        val imageUrl = uploadedImageUrl ?: existing.imageUrl.nullIfBlank()
        val updated = existing.copy(rating = rating, review = review, imageUrl = imageUrl)
        firebase.updatePost(updated)
        withContext(Dispatchers.IO) {
            local.upsertPost(updated)
        }
    }

    suspend fun deletePost(postId: String) {
        firebase.deletePost(postId)
        withContext(Dispatchers.IO) {
            local.deleteLikesByPost(postId)
            local.deletePost(postId)
        }
    }

    suspend fun toggleLike(postId: String) {
        val userId = firebase.currentUserId() ?: return
        val post = withContext(Dispatchers.IO) { local.getPost(postId, userId) } ?: return
        if (post.userId == userId) {
            return
        }

        val isLiked = withContext(Dispatchers.IO) { local.isLikedSync(userId, postId) }
        val newIsLiked = !isLiked
        val newCount = if (newIsLiked) post.likesCount + 1 else (post.likesCount - 1).coerceAtLeast(0)

        withContext(Dispatchers.IO) {
            if (newIsLiked) {
                local.upsertLike(LikeEntity(userId, postId))
            } else {
                local.deleteLike(userId, postId)
            }
            local.upsertPost(post.copy(likesCount = newCount))
        }

        try {
            firebase.toggleLike(postId)
            val fresh = firebase.getPost(postId)
            if (fresh != null) {
                withContext(Dispatchers.IO) {
                    local.upsertPost(fresh)
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.IO) {
                if (isLiked) {
                    local.upsertLike(LikeEntity(userId, postId))
                } else {
                    local.deleteLike(userId, postId)
                }
                local.upsertPost(post)
            }
            throw e
        }
    }

    fun observeComments(postId: String): LiveData<List<CommentEntity>> = local.observeComments(postId)

    suspend fun refreshComments(postId: String) {
        val comments = firebase.fetchComments(postId)
        withContext(Dispatchers.IO) {
            local.replaceComments(postId, comments)
        }
    }

    suspend fun addComment(postId: String, text: String) {
        val userId = firebase.currentUserId() ?: return
        val post = withContext(Dispatchers.IO) { local.getPost(postId, userId) } ?: return
        val optimisticPost = post.copy(commentsCount = post.commentsCount + 1)

        withContext(Dispatchers.IO) {
            local.upsertPost(optimisticPost)
        }

        try {
            firebase.addComment(postId, text)
            val fresh = firebase.getPost(postId)
            if (fresh != null) {
                withContext(Dispatchers.IO) {
                    local.upsertPost(fresh)
                }
            }
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
        val currentUserId = firebase.currentUserId()
        val (posts, newLastDoc) = firebase.fetchPostsPage(PAGE_SIZE, lastDocument)
        if (posts.isEmpty()) {
            canLoadMorePosts = false
            return PostsPageResult(endReached = true)
        }
        lastDocument = newLastDoc
        withContext(Dispatchers.IO) {
            local.upsertPosts(posts)
            if (currentUserId != null) {
                val likes = posts.filter { it.isLikedByUser }.map {
                    LikeEntity(userId = currentUserId, postId = it.id)
                }
                if (likes.isNotEmpty()) {
                    local.upsertLikes(likes)
                }
            }
        }
        canLoadMorePosts = posts.size.toLong() >= PAGE_SIZE
        return PostsPageResult(endReached = !canLoadMorePosts)
    }

    private fun resetPagination() {
        lastDocument = null
        canLoadMorePosts = true
    }

    companion object {
        private const val REFRESH_INTERVAL = 300_000L
        private const val DEFAULT_USERNAME = "User"
        private const val PAGE_SIZE = 15L
    }
}

data class PostsPageResult(
    val endReached: Boolean
)
