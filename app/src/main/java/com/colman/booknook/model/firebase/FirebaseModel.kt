package com.booknook.app.model.firebase

import android.net.Uri
import com.booknook.app.data.local.entities.PostEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class FirebaseModel {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    fun currentUserId(): String? = auth.currentUser?.uid

    fun requireUserId(): String = currentUserId() ?: throw IllegalStateException("Not logged in")

    fun logout() { auth.signOut() }

    suspend fun register(email: String, password: String, username: String) {
        val res = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = res.user?.uid ?: throw IllegalStateException("Registration failed")
        db.collection("users").document(uid).set(mapOf(
            "username" to username,
            "email" to email,
            "avatarUrl" to null
        )).await()
    }

    suspend fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    suspend fun requireUsernameFallback(): String {
        val uid = requireUserId()
        val doc = db.collection("users").document(uid).get().await()
        return (doc.getString("username") ?: "User")
    }

    suspend fun uploadPostImage(postId: String, uri: Uri): String {
        val ref = storage.reference.child("posts/$postId.jpg")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun createPost(post: PostEntity) {
        db.collection("posts").document(post.id).set(post.toMap()).await()
    }

    suspend fun updatePost(post: PostEntity) {
        db.collection("posts").document(post.id).set(post.toMap()).await()
    }

    suspend fun deletePost(postId: String) {
        db.collection("posts").document(postId).delete().await()
        // best-effort delete image
        try { storage.reference.child("posts/$postId.jpg").delete().await() } catch (_: Exception) {}
    }

    suspend fun getPost(postId: String): PostEntity? {
        val doc = db.collection("posts").document(postId).get().await()
        if (!doc.exists()) return null
        return doc.toPostEntity()
    }

    suspend fun fetchAllPosts(): List<PostEntity> {
        val snap = db.collection("posts").orderBy("createdAt").get().await()
        return snap.documents.mapNotNull { it.toPostEntity() }
            .sortedByDescending { it.createdAt }
    }
}

private fun PostEntity.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "userId" to userId,
    "username" to username,
    "bookId" to bookId,
    "bookTitle" to bookTitle,
    "bookAuthor" to bookAuthor,
    "bookThumbnail" to bookThumbnail,
    "rating" to rating,
    "review" to review,
    "imageUrl" to imageUrl,
    "createdAt" to createdAt,
    "likesCount" to likesCount,
    "commentsCount" to commentsCount
)

private fun com.google.firebase.firestore.DocumentSnapshot.toPostEntity(): PostEntity? {
    val id = getString("id") ?: return null
    return PostEntity(
        id = id,
        userId = getString("userId") ?: "",
        username = getString("username") ?: "",
        bookId = getString("bookId") ?: "",
        bookTitle = getString("bookTitle") ?: "",
        bookAuthor = getString("bookAuthor") ?: "",
        bookThumbnail = getString("bookThumbnail"),
        rating = (getLong("rating") ?: 0).toInt(),
        review = getString("review") ?: "",
        imageUrl = getString("imageUrl"),
        createdAt = getLong("createdAt") ?: 0L,
        likesCount = (getLong("likesCount") ?: 0).toInt(),
        commentsCount = (getLong("commentsCount") ?: 0).toInt()
    )
}
