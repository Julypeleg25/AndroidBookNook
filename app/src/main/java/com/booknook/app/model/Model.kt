package com.booknook.app.model

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import com.booknook.app.data.local.AppDatabase
import com.booknook.app.data.local.entities.PostEntity
import com.booknook.app.data.local.entities.ReadlistEntity
import com.booknook.app.data.local.entities.UserEntity
import com.booknook.app.data.local.entities.WishlistEntity
import com.booknook.app.data.repository.AppLocalRepository
import com.booknook.app.domain.Book
import com.booknook.app.model.api.ApiModel
import com.booknook.app.model.firebase.FirebaseModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

object Model {

    lateinit var local: AppLocalRepository
        private set

    private var lastRefreshTime = 0L
    private val REFRESH_INTERVAL = 300_000L

    val firebase = FirebaseModel()
    val api = ApiModel()

    fun init(context: Context) {
        val db = AppDatabase.getInstance(context)
        local = AppLocalRepository(
            db.postDao(),
            db.wishlistDao(),
            db.readlistDao(),
            db.userDao(),
            db.cachedBookDao()
        )
    }

    fun currentUserId(): String? = firebase.currentUserId()

    fun observeLocalUser(): LiveData<UserEntity?> = local.observeUser()

    suspend fun ensureLocalProfile() {
        val profile = firebase.fetchProfile() ?: return
        withContext(Dispatchers.IO) {
            local.upsertUser(profile)
        }
    }

    suspend fun updateProfile(username: String, email: String, avatarUri: Uri?): UserEntity {
        val updated = firebase.updateProfile(username, email, avatarUri)
        withContext(Dispatchers.IO) {
            local.upsertUser(updated)
        }
        return updated
    }

    suspend fun logoutAsync() {
        firebase.logout()
        withContext(Dispatchers.IO) {
            local.clearUser()
        }
    }

    suspend fun searchBooks(query: String, startIndex: Int = 0): List<Book> = api.searchBooks(query, startIndex)

    fun observePosts(): LiveData<List<PostEntity>> = local.observePosts()
    fun observeMyPosts(userId: String): LiveData<List<PostEntity>> = local.observeMyPosts(userId)
    fun observePost(postId: String): LiveData<PostEntity?> = local.observePost(postId)
    suspend fun getPost(postId: String): PostEntity? = local.getPost(postId)

    fun searchPosts(title: String?, author: String?, minRating: Int?, minComments: Int?) =
        local.searchPosts(title, author, minRating, minComments)

    fun searchPostsByQuery(query: String) = local.searchPostsByQuery(query)

    suspend fun isOwnPost(postId: String): Boolean {
        val uid = currentUserId() ?: return false
        val post = withContext(Dispatchers.IO) { local.getPost(postId) }
        return post?.userId == uid
    }

    suspend fun refreshPosts(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastRefreshTime < REFRESH_INTERVAL) return

        val posts = firebase.fetchAllPosts()
        withContext(Dispatchers.IO) {
            local.upsertPosts(posts)
        }
        lastRefreshTime = now
    }

    suspend fun createPost(book: Book, rating: Int, review: String, imageUri: Uri?) {
        val uid = firebase.requireUserId()
        val username = firebase.fetchProfile()?.username ?: "User"
        val postId = UUID.randomUUID().toString()
        val imageUrl = if (imageUri != null) firebase.uploadPostImage(postId, imageUri) else null
        val now = System.currentTimeMillis()

        val post = PostEntity(
            id = postId,
            userId = uid,
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
            commentsCount = 0
        )

        firebase.createPost(post)
        withContext(Dispatchers.IO) {
            local.upsertPost(post)
        }
    }

    suspend fun updatePost(postId: String, rating: Int, review: String, imageUri: Uri?) {
        val existing = firebase.getPost(postId) ?: return
        val imageUrl = if (imageUri != null) firebase.uploadPostImage(postId, imageUri) else existing.imageUrl
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
        val uid = currentUserId() ?: return
        val post = withContext(Dispatchers.IO) { local.getPost(postId) } ?: return
        
        if (post.userId == uid) {
            com.booknook.app.util.Logger.d("Likes", "User $uid attempted to like their own post $postId - Blocked.")
            return
        }

        // Optimistic update
        val newIsLiked = !post.isLikedByUser
        val newCount = if (newIsLiked) post.likesCount + 1 else (post.likesCount - 1).coerceAtLeast(0)
        val optimisticPost = post.copy(isLikedByUser = newIsLiked, likesCount = newCount)
        
        withContext(Dispatchers.IO) { local.upsertPost(optimisticPost) }
        com.booknook.app.util.Logger.d("Likes", "Optimistic update for $postId: Liked=$newIsLiked, Count=$newCount")

        try {
            firebase.toggleLike(postId)
            
            // Sync again with network source of truth
            val fresh = firebase.getPost(postId)
            if (fresh != null) {
                withContext(Dispatchers.IO) { local.upsertPost(fresh) }
                com.booknook.app.util.Logger.d("Likes", "Backend sync successful for $postId. Final count: ${fresh.likesCount}")
            }
        } catch (e: Exception) {
            com.booknook.app.util.Logger.e("Likes", "Backend sync failed for $postId. Reverting local state.", e)
            // Revert optimistic update
            withContext(Dispatchers.IO) { local.upsertPost(post) }
            throw e
        }
    }

    suspend fun addComment(postId: String, text: String) {
        val post = withContext(Dispatchers.IO) { local.getPost(postId) } ?: return

        // Optimistic update
        val optimisticPost = post.copy(commentsCount = post.commentsCount + 1)
        withContext(Dispatchers.IO) { local.upsertPost(optimisticPost) }
        com.booknook.app.util.Logger.d("Comments", "Optimistic count update for $postId: ${optimisticPost.commentsCount}")

        try {
            firebase.addComment(postId, text)
            // Sync with network source of truth
            val fresh = firebase.getPost(postId)
            if (fresh != null) {
                withContext(Dispatchers.IO) { local.upsertPost(fresh) }
            }
        } catch (e: Exception) {
            com.booknook.app.util.Logger.e("Comments", "Comment sync failed", e)
            // Revert optimistic update
            withContext(Dispatchers.IO) { local.upsertPost(post) }
            throw e
        }
    }

    fun observeWishlist(userId: String): LiveData<List<WishlistEntity>> = local.observeWishlist(userId)
    fun observeWishlistExists(userId: String, bookId: String): LiveData<Boolean> = 
        local.observeWishlistExists("$userId|$bookId")

    suspend fun toggleWishlist(userId: String, book: Book): Boolean {
        val key = "$userId|${book.id}"
        return withContext(Dispatchers.IO) {
            if (local.existsInWishlist(key)) {
                local.deleteWishlist(key)
                false
            } else {
                local.upsertWishlist(
                    WishlistEntity(
                        key = key,
                        userId = userId,
                        bookId = book.id,
                        title = book.title,
                        author = book.author,
                        thumbnail = book.thumbnail,
                        addedAt = System.currentTimeMillis()
                    )
                )
                true
            }
        }
    }

    suspend fun removeFromWishlist(userId: String, bookId: String) {
        val key = "$userId|$bookId"
        withContext(Dispatchers.IO) { local.deleteWishlist(key) }
    }

    fun observeReadlist(userId: String): LiveData<List<ReadlistEntity>> = local.observeReadlist(userId)
    fun observeReadlistExists(userId: String, bookId: String): LiveData<Boolean> = 
        local.observeReadlistExists("$userId|$bookId")

    suspend fun toggleReadlist(userId: String, book: Book): Boolean {
        val key = "$userId|${book.id}"
        return withContext(Dispatchers.IO) {
            if (local.existsInReadlist(key)) {
                local.deleteReadlist(key)
                false
            } else {
                local.upsertReadlist(
                    ReadlistEntity(
                        key = key,
                        userId = userId,
                        bookId = book.id,
                        title = book.title,
                        author = book.author,
                        thumbnail = book.thumbnail,
                        addedAt = System.currentTimeMillis()
                    )
                )
                true
            }
        }
    }

    suspend fun removeFromReadlist(userId: String, bookId: String) {
        val key = "$userId|$bookId"
        withContext(Dispatchers.IO) { local.deleteReadlist(key) }
    }
}
