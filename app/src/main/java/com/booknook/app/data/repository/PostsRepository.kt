package com.booknook.app.data.repository

import android.net.Uri
import androidx.lifecycle.LiveData
import com.booknook.app.data.local.entities.CommentEntity
import com.booknook.app.data.local.entities.LikeEntity
import com.booknook.app.data.local.entities.PostEntity
import com.booknook.app.domain.Book
import com.booknook.app.model.firebase.FirebaseModel
import com.booknook.app.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class PostsRepository(
    private val local: AppLocalRepository,
    private val firebase: FirebaseModel
) {
    private var lastRefreshTime = 0L

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

    fun searchPosts(title: String?, author: String?, minRating: Int?, minComments: Int?, currentUserId: String) =
        local.searchPosts(title, author, minRating, minComments, currentUserId)

    fun searchPostsByQuery(query: String, currentUserId: String) = local.searchPostsByQuery(query, currentUserId)

    suspend fun isOwnPost(postId: String, currentUserId: String): Boolean {
        val post = withContext(Dispatchers.IO) { local.getPost(postId, currentUserId) }
        return post?.userId == currentUserId
    }

    suspend fun refreshPosts(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastRefreshTime < REFRESH_INTERVAL) return

        val posts = firebase.fetchAllPosts()
        val currentUserId = firebase.currentUserId()

        withContext(Dispatchers.IO) {
            local.upsertPosts(posts)
            if (currentUserId != null) {
                val likes = posts.filter { it.isLikedByUser }.map {
                    LikeEntity(userId = currentUserId, postId = it.id)
                }
                likes.forEach { like ->
                    local.upsertLike(like)
                }
            }
        }
        lastRefreshTime = now
    }

    suspend fun createPost(book: Book, rating: Int, review: String, imageUri: Uri?) {
        val userId = firebase.requireUserId()
        val username = firebase.fetchProfile()?.username ?: DEFAULT_USERNAME
        val postId = UUID.randomUUID().toString()
        val imageUrl = imageUri?.let { firebase.uploadPostImage(userId, postId, it) }
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

        val imageUrl = imageUri?.let { firebase.uploadPostImage(userId, postId, it) } ?: existing.imageUrl
        val updated = existing.copy(rating = rating, review = review, imageUrl = imageUrl)
        firebase.updatePost(updated)
        withContext(Dispatchers.IO) {
            local.upsertPost(updated)
        }
    }

    suspend fun deletePost(postId: String) {
        firebase.deletePost(postId)
        withContext(Dispatchers.IO) {
            local.deletePost(postId)
        }
    }

    suspend fun toggleLike(postId: String) {
        val userId = firebase.currentUserId() ?: return
        val post = withContext(Dispatchers.IO) { local.getPost(postId, userId) } ?: return
        if (post.userId == userId) {
            Logger.d("Likes", "User $userId attempted to like their own post $postId - blocked.")
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
            Logger.e("Likes", "Backend sync failed for $postId. Reverting local state.", e)
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
            local.deleteCommentsByPost(postId)
            local.upsertComments(comments)
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
            Logger.e("Comments", "Comment sync failed", e)
            withContext(Dispatchers.IO) {
                local.upsertPost(post)
            }
            throw e
        }
    }

    companion object {
        private const val REFRESH_INTERVAL = 300_000L
        private const val DEFAULT_USERNAME = "User"
    }
}
