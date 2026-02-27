package com.colman.booknook.model

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import com.colman.booknook.data.local.AppDatabase
import com.colman.booknook.data.local.entities.PostEntity
import com.colman.booknook.data.local.entities.UserEntity
import com.colman.booknook.data.local.entities.WishlistEntity
import com.colman.booknook.data.repository.AppLocalRepository
import com.colman.booknook.domain.Book
import com.colman.booknook.model.api.ApiModel
import com.colman.booknook.model.firebase.FirebaseModel
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

    fun currentUserId(): String? = firebase.currentUserId()

    suspend fun ensureLocalProfile() {
        val profile = firebase.fetchProfile() ?: return
        local.upsertUser(profile)
    }

    fun observeLocalUser(): LiveData<UserEntity?> = local.observeUser()

    suspend fun logout() {
        firebase.logout()
        local.clearUser()
    }

    suspend fun updateProfile(username: String, email: String, avatarUri: Uri?): UserEntity {
        val updated = firebase.updateProfile(username, email, avatarUri)
        local.upsertUser(updated)
        return updated
    }

    // external REST
    suspend fun searchBooks(query: String): List<Book> = api.searchBooks(query)

    // posts
    fun observePosts(): LiveData<List<PostEntity>> = local.observePosts()
    fun observeMyPosts(userId: String): LiveData<List<PostEntity>> = local.observeMyPosts(userId)
    fun observePost(postId: String): LiveData<PostEntity?> = local.observePost(postId)
    fun searchPosts(title: String?, author: String?, minRating: Int?, minComments: Int?) =
        local.searchPosts(title, author, minRating, minComments)

    suspend fun refreshPosts() {
        val posts = firebase.fetchAllPosts()
        local.upsertPosts(posts)
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

    suspend fun toggleLike(postId: String) {
        firebase.toggleLike(postId)
        // refresh single post from remote to update counts
        val updated = firebase.getPost(postId) ?: return
        local.upsertPost(updated)
    }

    suspend fun addComment(postId: String, text: String) {
        firebase.addComment(postId, text)
        val updated = firebase.getPost(postId) ?: return
        local.upsertPost(updated)
    }

    // wishlist (local required)
    fun observeWishlist(userId: String) = local.observeWishlist(userId)

    suspend fun addToWishlist(userId: String, book: Book) {
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
    }

    suspend fun removeFromWishlist(userId: String, bookId: String) {
        val key = "$userId|$bookId"
        local.deleteWishlist(key)
    }
}
