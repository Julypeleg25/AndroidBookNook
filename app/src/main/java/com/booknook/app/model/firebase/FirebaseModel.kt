package com.booknook.app.model.firebase

import android.net.Uri
import com.booknook.app.data.local.entities.PostEntity
import com.booknook.app.data.local.entities.UserEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
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
    private val storage = FirebaseStorage.getInstance()

    fun currentUserId(): String? = auth.currentUser?.uid
    fun requireUserId(): String = currentUserId() ?: throw IllegalStateException("Not logged in")

    fun logout() { auth.signOut() }

    suspend fun register(email: String, password: String, username: String, avatarUri: Uri? = null) {
        com.booknook.app.util.Logger.d("Auth", "Registering user: $email")
        val res = auth.createUserWithEmailAndPassword(email, password).await()
        val user = res.user ?: throw IllegalStateException("Registration failed: No User")
        val uid = user.uid

        val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
            displayName = username
        }
        user.updateProfile(profileUpdates).await()
        
        var avatarUrl: String? = null
        if (avatarUri != null) {
            try {
                avatarUrl = uploadAvatar(uid, avatarUri)
            } catch (_: Exception) {}
        }

        try {
            withTimeout(15000) {
                withContext(Dispatchers.IO) {
                    db.collection("users").document(uid).set(mapOf(
                        "username" to username,
                        "email" to email,
                        "avatarUrl" to avatarUrl
                    )).await()
                }
            }
        } catch (e: FirebaseFirestoreException) {
            com.booknook.app.util.Logger.e("Firestore", "Registration profile sync failed: ${e.code}", e)
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                throw Exception("PERMISSION_DENIED: Check Firestore Rules or if API is enabled in Console.")
            }
            throw e
        } catch (e: Exception) {
            com.booknook.app.util.Logger.e("Firestore", "Unexpected registration error", e)
            throw e
        }
    }

    suspend fun login(email: String, password: String) {
        com.booknook.app.util.Logger.d("Auth", "Attempting login for: $email")
        auth.signInWithEmailAndPassword(email, password).await()
        com.booknook.app.util.Logger.d("Auth", "Login successful")
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


    suspend fun updateProfile(username: String, email: String, avatarUri: Uri?): UserEntity {
        val uid = requireUserId()
        val user = auth.currentUser ?: throw IllegalStateException("No auth user")

        if (user.displayName != username) {
            val profileUpdates = userProfileChangeRequest { displayName = username }
            user.updateProfile(profileUpdates).await()
        }

        val avatarUrl = if (avatarUri != null) {
            uploadAvatar(uid, avatarUri)
        } else {
            fetchProfile()?.avatarUrl
        }

        val data = mapOf("username" to username, "avatarUrl" to avatarUrl)
        db.collection("users").document(uid).update(data).await()

        return UserEntity(id = uid, username = username, email = email, avatarUrl = avatarUrl)
    }

    private suspend fun uploadAvatar(uid: String, uri: Uri): String {
        val ref = storage.reference.child("avatars/$uid.jpg")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun uploadPostImage(userId: String, postId: String, uri: Uri): String {
        val ref = storage.reference.child("posts/$userId/$postId.jpg")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun updatePost(post: PostEntity) {
        val uid = requireUserId()

        if (post.userId != uid) {
            throw IllegalStateException("Unauthorized: You do not own this post.")
        }

        com.booknook.app.util.Logger.d("Firestore", "Updating post: ${post.id}")

        db.collection("posts")
            .document(post.id)
            .set(post.toMap())
            .await()

        com.booknook.app.util.Logger.d("Firestore", "Post ${post.id} updated successfully")
    }

    suspend fun createPost(post: PostEntity) {
        db.collection("posts").document(post.id).set(post.toMap()).await()
    }

    suspend fun deletePost(postId: String) {
        val post = getPost(postId) ?: return
        val uid = requireUserId()

        if (post.userId != uid) throw Exception("Unauthorized deletion attempt")

        db.collection("posts").document(postId).delete().await()

        if (!post.imageUrl.isNullOrEmpty()) {
            try {
                storage.reference.child("posts/$uid/$postId.jpg").delete().await()
            } catch (e: Exception) {
                com.booknook.app.util.Logger.d("Firebase", "Storage file already gone or missing")
            }
        }
    }

    suspend fun getPost(postId: String): PostEntity? {
        val doc = db.collection("posts").document(postId).get().await()
        if (!doc.exists()) return null
        
        val uid = currentUserId()
        val isLiked = if (uid != null) {
            db.collection("posts").document(postId).collection("likes").document(uid).get().await().exists()
        } else false
        
        return doc.toPostEntity(isLiked)
    }

    suspend fun fetchAllPosts(): List<PostEntity> {
        val snap = db.collection("posts").orderBy("createdAt").get().await()
        val posts = snap.documents.mapNotNull { it.toPostEntity(false) }
        
        val uid = currentUserId() ?: return posts.sortedByDescending { it.createdAt }
        
        val likedPostIds = try {
            db.collectionGroup("likes")
                .whereEqualTo(FieldPath.documentId(), uid)
                .get()
                .await()
                .documents
                .mapNotNull { it.reference.parent.parent?.id }
                .toSet()
        } catch (e: Exception) {
            emptySet()
        }

        return posts.map { post ->
            if (likedPostIds.contains(post.id)) post.copy(isLikedByUser = true) else post
        }.sortedByDescending { it.createdAt }
    }

    suspend fun toggleLike(postId: String) {
        val uid = requireUserId()
        com.booknook.app.util.Logger.d("Firestore", "User $uid toggling like for post $postId")
        val likeRef = db.collection("posts").document(postId).collection("likes").document(uid)
        val likeDoc = likeRef.get().await()
        val postRef = db.collection("posts").document(postId)

        if (likeDoc.exists()) {
            db.runBatch { batch ->
                batch.delete(likeRef)
                batch.update(postRef, "likesCount", FieldValue.increment(-1))
            }.await()
            com.booknook.app.util.Logger.d("Firestore", "Like removed for post $postId")
        } else {
            db.runBatch { batch ->
                batch.set(likeRef, mapOf("createdAt" to System.currentTimeMillis()))
                batch.update(postRef, "likesCount", FieldValue.increment(1))
            }.await()
            com.booknook.app.util.Logger.d("Firestore", "Like added for post $postId")
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
            "text" to text,
            "createdAt" to System.currentTimeMillis()
        )
        
        db.collection("posts").document(postId)
            .collection("comments").document(commentId)
            .set(commentData).await()

        db.collection("posts").document(postId)
            .update("commentsCount", FieldValue.increment(1)).await()
    }

    suspend fun fetchComments(postId: String): List<com.booknook.app.data.local.entities.CommentEntity> {
        val snap = db.collection("posts").document(postId)
            .collection("comments").orderBy("createdAt").get().await()
        return snap.documents.mapNotNull { doc ->
            com.booknook.app.data.local.entities.CommentEntity(
                id = doc.getString("id") ?: "",
                postId = postId,
                userId = doc.getString("userId") ?: "",
                username = doc.getString("username") ?: "User",
                text = doc.getString("text") ?: "",
                createdAt = doc.getLong("createdAt") ?: 0L
            )
        }
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

private fun com.google.firebase.firestore.DocumentSnapshot.toPostEntity(isLiked: Boolean): PostEntity? {
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
        commentsCount = (getLong("commentsCount") ?: 0).toInt(),
        isLikedByUser = isLiked
    )
}
