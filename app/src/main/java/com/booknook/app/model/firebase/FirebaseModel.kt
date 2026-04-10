package com.booknook.app.model.firebase

import android.net.Uri
import com.booknook.app.data.local.entities.PostEntity
import com.booknook.app.data.local.entities.UserEntity
import com.booknook.app.data.local.entities.CommentEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.firebase.auth.userProfileChangeRequest
import java.util.UUID

class FirebaseModel {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun currentUserId(): String? = auth.currentUser?.uid
    fun requireUserId(): String = currentUserId() ?: throw IllegalStateException("Not logged in")

    fun logout() { auth.signOut() }

    suspend fun register(email: String, password: String, username: String): String {
        val res = auth.createUserWithEmailAndPassword(email, password).await()
        val user = res.user ?: throw IllegalStateException("Registration failed: No User")
        val uid = user.uid
        val profileUpdates = userProfileChangeRequest {
            displayName = username
        }
        user.updateProfile(profileUpdates).await()
        return uid
    }

    suspend fun createUserProfile(uid: String, username: String, email: String, avatarUrl: String?) {
        withTimeout(15000) {
            withContext(Dispatchers.IO) {
                db.collection("users").document(uid).set(mapOf(
                    "username" to username,
                    "email" to email,
                    "avatarUrl" to avatarUrl
                )).await()
            }
        }
    }

    suspend fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    suspend fun fetchProfile(): UserEntity? {
        val uid = currentUserId() ?: return null
        val doc = db.collection("users").document(uid).get().await()
        if (!doc.exists()) return null
        return UserEntity(
            id = uid,
            username = doc.getString("username") ?: "User",
            email = doc.getString("email") ?: "",
            avatarUrl = doc.getString("avatarUrl")
        )
    }

    suspend fun updateProfile(username: String, email: String, avatarUrl: String?): UserEntity {
        val uid = requireUserId()
        val user = auth.currentUser ?: throw IllegalStateException("No auth user")
        if (user.displayName != username) {
            val profileUpdates = userProfileChangeRequest { displayName = username }
            user.updateProfile(profileUpdates).await()
        }
        val data = mapOf("username" to username, "avatarUrl" to avatarUrl)
        db.collection("users").document(uid).update(data).await()
        return UserEntity(id = uid, username = username, email = email, avatarUrl = avatarUrl)
    }

    suspend fun updatePost(post: PostEntity) {
        val uid = requireUserId()
        if (post.userId != uid) throw IllegalStateException("Unauthorized")
        db.collection("posts").document(post.id).set(postToMap(post)).await()
    }

    suspend fun createPost(post: PostEntity) {
        db.collection("posts").document(post.id).set(postToMap(post)).await()
    }

    suspend fun deletePost(postId: String) {
        val post = getPost(postId) ?: return
        val uid = requireUserId()
        if (post.userId != uid) throw Exception("Unauthorized")
        db.collection("posts").document(postId).delete().await()
    }

    suspend fun getPost(postId: String): PostEntity? {
        val doc = db.collection("posts").document(postId).get().await()
        if (!doc.exists()) return null
        val uid = currentUserId()
        val isLiked = if (uid != null) {
            db.collection("posts").document(postId).collection("likes").document(uid).get().await().exists()
        } else false
        return documentToPostEntity(doc, isLiked)
    }

    suspend fun fetchAllPosts(): List<PostEntity> {
        val snap = db.collection("posts").orderBy("createdAt").get().await()
        val posts = snap.documents.mapNotNull { documentToPostEntity(it, false) }
        val uid = currentUserId() ?: return posts.sortedByDescending { it.createdAt }
        val likedPostIds = db.collectionGroup("likes")
                .whereEqualTo(FieldPath.documentId(), uid)
                .get()
                .await()
                .documents
                .mapNotNull { it.reference.parent.parent?.id }
                .toSet()
        return posts.map { post ->
            if (likedPostIds.contains(post.id)) post.copy(isLikedByUser = true) else post
        }.sortedByDescending { it.createdAt }
    }

    suspend fun fetchPostsPage(
        limit: Long,
        lastDocument: DocumentSnapshot?
    ): Pair<List<PostEntity>, DocumentSnapshot?> {
        var query = db.collection("posts")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit)
        if (lastDocument != null) {
            query = query.startAfter(lastDocument)
        }
        val snap = query.get().await()
        val newLastDoc = snap.documents.lastOrNull()
        val uid = currentUserId()
        val posts = snap.documents.mapNotNull { doc ->
            val post = documentToPostEntity(doc, false) ?: return@mapNotNull null
            if (uid != null) {
                val isLiked = db.collection("posts").document(post.id)
                    .collection("likes").document(uid).get().await().exists()
                post.copy(isLikedByUser = isLiked)
            } else {
                post
            }
        }
        return Pair(posts, newLastDoc)
    }

    suspend fun toggleLike(postId: String) {
        val uid = requireUserId()
        val likeRef = db.collection("posts").document(postId).collection("likes").document(uid)
        val likeDoc = likeRef.get().await()
        val postRef = db.collection("posts").document(postId)
        if (likeDoc.exists()) {
            db.runBatch { batch ->
                batch.delete(likeRef)
                batch.update(postRef, "likesCount", FieldValue.increment(-1))
            }.await()
        } else {
            db.runBatch { batch ->
                batch.set(likeRef, mapOf("createdAt" to System.currentTimeMillis()))
                batch.update(postRef, "likesCount", FieldValue.increment(1))
            }.await()
        }
    }

    suspend fun addComment(postId: String, text: String) {
        val uid = requireUserId()
        val profile = fetchProfile()
        val commentId = UUID.randomUUID().toString()
        val commentData = mapOf(
            "id" to commentId,
            "userId" to uid,
            "username" to (profile?.username ?: "User"),
            "userAvatarUrl" to profile?.avatarUrl,
            "text" to text,
            "createdAt" to System.currentTimeMillis()
        )
        db.collection("posts").document(postId)
            .collection("comments").document(commentId)
            .set(commentData).await()
        db.collection("posts").document(postId)
            .update("commentsCount", FieldValue.increment(1)).await()
    }

    suspend fun fetchComments(postId: String): List<CommentEntity> {
        val snap = db.collection("posts").document(postId)
            .collection("comments").orderBy("createdAt").get().await()
        return snap.documents.mapNotNull { doc ->
            CommentEntity(
                id = doc.getString("id") ?: "",
                postId = postId,
                userId = doc.getString("userId") ?: "",
                username = doc.getString("username") ?: "User",
                text = doc.getString("text") ?: "",
                userAvatarUrl = doc.getString("userAvatarUrl"),
                createdAt = doc.getLong("createdAt") ?: 0L
            )
        }
    }

    private fun postToMap(post: PostEntity): Map<String, Any?> = mapOf(
        "id" to post.id,
        "userId" to post.userId,
        "username" to post.username,
        "bookId" to post.bookId,
        "bookTitle" to post.bookTitle,
        "bookAuthor" to post.bookAuthor,
        "bookThumbnail" to post.bookThumbnail,
        "rating" to post.rating,
        "review" to post.review,
        "imageUrl" to post.imageUrl,
        "createdAt" to post.createdAt,
        "likesCount" to post.likesCount,
        "commentsCount" to post.commentsCount,
        "bookPublishedDate" to post.bookPublishedDate,
        "bookGenre" to post.bookGenre,
        "bookPageCount" to post.bookPageCount,
        "bookDescription" to post.bookDescription
    )

    private fun documentToPostEntity(doc: DocumentSnapshot, isLiked: Boolean): PostEntity? {
        val id = doc.getString("id") ?: return null
        return PostEntity(
            id = id,
            userId = doc.getString("userId") ?: "",
            username = doc.getString("username") ?: "",
            bookId = doc.getString("bookId") ?: "",
            bookTitle = doc.getString("bookTitle") ?: "",
            bookAuthor = doc.getString("bookAuthor") ?: "",
            bookThumbnail = doc.getString("bookThumbnail"),
            rating = (doc.getLong("rating") ?: 0).toInt(),
            review = doc.getString("review") ?: "",
            imageUrl = doc.getString("imageUrl"),
            createdAt = doc.getLong("createdAt") ?: 0L,
            likesCount = (doc.getLong("likesCount") ?: 0).toInt(),
            commentsCount = (doc.getLong("commentsCount") ?: 0).toInt(),
            bookPublishedDate = doc.getString("bookPublishedDate"),
            bookGenre = doc.getString("bookGenre"),
            bookPageCount = doc.getLong("bookPageCount")?.toInt(),
            bookDescription = doc.getString("bookDescription"),
            isLikedByUser = isLiked
        )
    }
}
