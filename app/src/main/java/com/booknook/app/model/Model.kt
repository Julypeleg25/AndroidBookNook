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

    suspend fun searchBooks(query: String): List<Book> = api.searchBooks(query)

    fun observePosts(): LiveData<List<PostEntity>> = local.observePosts()
    fun observeMyPosts(userId: String): LiveData<List<PostEntity>> = local.observeMyPosts(userId)
    fun observePost(postId: String): LiveData<PostEntity?> = local.observePost(postId)
    suspend fun getPost(postId: String): PostEntity? = local.getPost(postId)

    fun searchPosts(title: String?, author: String?, minRating: Int?, minComments: Int?) =
        local.searchPosts(title, author, minRating, minComments)

    suspend fun refreshPosts() {
        val now = System.currentTimeMillis()
        if (now - lastRefreshTime < REFRESH_INTERVAL) return

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
        firebase.toggleLike(postId)
        val updated = firebase.getPost(postId) ?: return
        withContext(Dispatchers.IO) {
            local.upsertPost(updated)
        }
    }

    suspend fun addComment(postId: String, text: String) {
        firebase.addComment(postId, text)
        val updated = firebase.getPost(postId) ?: return
        withContext(Dispatchers.IO) {
            local.upsertPost(updated)
        }
    }

    fun observeWishlist(userId: String): LiveData<List<WishlistEntity>> = local.observeWishlist(userId)

    suspend fun addToWishlist(userId: String, book: Book) {
        val key = "$userId|${book.id}"
        withContext(Dispatchers.IO) {
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
        }
    }

    suspend fun removeFromWishlist(userId: String, bookId: String) {
        val key = "$userId|$bookId"
        withContext(Dispatchers.IO) {
            local.deleteWishlist(key)
        }
    }

    fun observeReadlist(userId: String): LiveData<List<ReadlistEntity>> = local.observeReadlist(userId)

    suspend fun addToReadlist(userId: String, book: Book) {
        val key = "$userId|${book.id}"
        withContext(Dispatchers.IO) {
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
        }
    }

    suspend fun removeFromReadlist(userId: String, bookId: String) {
        val key = "$userId|$bookId"
        withContext(Dispatchers.IO) {
            local.deleteReadlist(key)
        }
    }
}
