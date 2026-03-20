package com.booknook.app.model

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import com.booknook.app.data.local.AppDatabase
import com.booknook.app.data.local.entities.*
import com.booknook.app.data.repository.AppLocalRepository
import com.booknook.app.domain.Book
import com.booknook.app.domain.Post
import com.booknook.app.domain.UserProfile
import com.booknook.app.model.api.ApiModel
import com.booknook.app.model.firebase.FirebaseModel
import java.util.UUID

object Model {

    lateinit var local: AppLocalRepository
        private set

    val firebase = FirebaseModel()
    val api = ApiModel()

    fun init(context: Context) {
        val db = AppDatabase.getInstance(context)
        local = AppLocalRepository(db.postDao(), db.wishlistDao(), db.userDao(), db.cachedBookDao())
    }

    // -------- Auth / User --------
    fun currentUserId(): String? = firebase.currentUserId()

    fun observeLocalUser(): LiveData<UserEntity?> = local.observeUser()

    fun logout() {
        firebase.logout()
        local.clearUser()
    }

    fun updateLocalUser(user: UserEntity) = local.upsertUser(user)

    // -------- Books (external REST) --------
    suspend fun searchBooks(query: String): List<Book> = api.searchBooks(query)

    // -------- Posts (remote DB + local cache) --------
    fun observePosts(): LiveData<List<PostEntity>> = local.observePosts()
    fun observeMyPosts(userId: String): LiveData<List<PostEntity>> = local.observeMyPosts(userId)
    fun observePost(postId: String): LiveData<PostEntity?> = local.observePost(postId)

    fun searchPosts(title: String?, author: String?, minRating: Int?, minComments: Int?) =
        local.searchPosts(title, author, minRating, minComments)

    suspend fun refreshPosts() {
        val posts = firebase.fetchAllPosts()
        local.upsertPosts(posts)
    }

    suspend fun createPost(
        bookId: String,
        bookTitle: String,
        bookAuthor: String,
        bookThumb: String?,
        rating: Int,
        review: String,
        imageUri: Uri?
    ) {
        val uid = firebase.requireUserId()
        val username = firebase.requireUsernameFallback()
        val postId = UUID.randomUUID().toString()
        val imageUrl = if (imageUri != null) firebase.uploadPostImage(postId, imageUri) else null
        val now = System.currentTimeMillis()

        val post = PostEntity(
            id = postId,
            userId = uid,
            username = username,
            bookId = bookId,
            bookTitle = bookTitle,
            bookAuthor = bookAuthor,
            bookThumbnail = bookThumb,
            rating = rating,
            review = review,
            imageUrl = imageUrl,
            createdAt = now,
            likesCount = 0,
            commentsCount = 0
        )

        firebase.createPost(post)
        local.upsertPost(post)
    }

    suspend fun updatePost(postId: String, rating: Int, review: String, imageUri: Uri?) {
        val existing = firebase.getPost(postId) ?: return
        val imageUrl = if (imageUri != null) firebase.uploadPostImage(postId, imageUri) else existing.imageUrl
        val updated = existing.copy(rating = rating, review = review, imageUrl = imageUrl)
        firebase.updatePost(updated)
        local.upsertPost(updated)
    }

    suspend fun deletePost(postId: String) {
        firebase.deletePost(postId)
        local.deletePost(postId)
    }

    // -------- Wishlist --------
    fun observeWishlist(userId: String) = local.observeWishlist(userId)

    fun addToWishlist(userId: String, book: Book) {
        val key = "$userId|${book.id}"
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
        // optional remote mirror could be added in firebase
    }

    fun removeFromWishlist(userId: String, bookId: String) {
        val key = "$userId|$bookId"
        local.deleteWishlist(key)
    }
}
