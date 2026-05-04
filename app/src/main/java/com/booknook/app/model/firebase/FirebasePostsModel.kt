package com.booknook.app.model.firebase

import com.booknook.app.data.local.entities.PostEntity
import com.booknook.app.util.Logger
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class FirebasePostsModel(
    private val db: FirebaseFirestore,
    private val authModel: FirebaseAuthModel
) {
    suspend fun createPost(post: PostEntity) {
        val uid = authModel.requireUserId()
        if (post.userId != uid) throw IllegalStateException("Unauthorized")
        postsCollection().document(post.id).set(post.toFirestorePostMap()).await()
    }

    suspend fun updatePost(post: PostEntity) {
        val uid = authModel.requireUserId()
        if (post.userId != uid) throw IllegalStateException("Unauthorized")
        postsCollection().document(post.id).set(post.toFirestorePostMap()).await()
    }

    suspend fun deletePost(postId: String) {
        val post = getPost(postId) ?: return
        val uid = authModel.requireUserId()
        if (post.userId != uid) throw IllegalStateException("Unauthorized")

        deletePostSubcollection(postId, FirebaseCollections.LIKES)
        deletePostSubcollection(postId, FirebaseCollections.COMMENTS)
        postsCollection().document(postId).delete().await()
    }

    suspend fun getPost(postId: String): PostEntity? {
        val doc = postsCollection().document(postId).get().await()
        if (!doc.exists()) return null
        return doc.toPostEntity(isPostLikedByCurrentUser(postId))
    }

    suspend fun fetchPostsByUser(userId: String): List<PostEntity> {
        val currentUserId = authModel.currentUserId()
        return fetchPostsByUserDocuments(userId)
            .mapNotNull { doc ->
                val post = doc.toPostEntity(false) ?: return@mapNotNull null
                if (currentUserId != null && currentUserId != userId) {
                    post.copy(isLikedByUser = isLiked(post.id, currentUserId))
                } else {
                    post
                }
            }
            .sortedByDescending { it.createdAt }
    }

    suspend fun fetchPostsPage(
        limit: Long,
        cursor: FirebasePostsCursor?
    ): FirebasePostsPage {
        var query = postsCollection()
            .orderBy(FirebaseFields.CREATED_AT, Query.Direction.DESCENDING)
            .limit(limit)
        if (cursor != null) {
            query = query.startAfter(cursor.document)
        }

        val snap = query.get().await()
        val uid = authModel.currentUserId()
        val posts = snap.documents.mapNotNull { doc ->
            val post = doc.toPostEntity(false) ?: return@mapNotNull null
            if (uid != null) post.copy(isLikedByUser = isLiked(post.id, uid)) else post
        }
        return FirebasePostsPage(
            posts = posts,
            cursor = snap.documents.lastOrNull()?.let(::FirebasePostsCursor)
        )
    }

    suspend fun toggleLike(postId: String) {
        val uid = authModel.requireUserId()
        val likeRef = postsCollection().document(postId)
            .collection(FirebaseCollections.LIKES)
            .document(uid)
        val postRef = postsCollection().document(postId)

        db.runTransaction { transaction ->
            val post = transaction.get(postRef)
            if (!post.exists()) throw IllegalStateException("Post not found")
            if (post.getString(FirebaseFields.USER_ID) == uid) throw IllegalStateException("Unauthorized")

            val like = transaction.get(likeRef)
            if (like.exists()) {
                transaction.delete(likeRef)
                transaction.update(postRef, FirebaseFields.LIKES_COUNT, FieldValue.increment(-1))
            } else {
                transaction.set(likeRef, mapOf(FirebaseFields.CREATED_AT to System.currentTimeMillis()))
                transaction.update(postRef, FirebaseFields.LIKES_COUNT, FieldValue.increment(1))
            }
        }.await()
    }

    suspend fun isLiked(postId: String, userId: String): Boolean {
        return postsCollection().document(postId)
            .collection(FirebaseCollections.LIKES)
            .document(userId)
            .get()
            .await()
            .exists()
    }

    private suspend fun isPostLikedByCurrentUser(postId: String): Boolean {
        val uid = authModel.currentUserId() ?: return false
        return isLiked(postId, uid)
    }

    private suspend fun fetchPostsByUserDocuments(userId: String): List<DocumentSnapshot> {
        return try {
            postsCollection()
                .whereEqualTo(FirebaseFields.USER_ID, userId)
                .orderBy(FirebaseFields.CREATED_AT, Query.Direction.DESCENDING)
                .get()
                .await()
                .documents
        } catch (e: FirebaseFirestoreException) {
            val requiresIndex = e.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION &&
                e.message?.contains("requires an index", ignoreCase = true) == true
            if (!requiresIndex) throw e

            Logger.e(
                "Firestore",
                "Missing composite index for posts-by-user query. Falling back to client-side sorting.",
                e
            )

            postsCollection()
                .whereEqualTo(FirebaseFields.USER_ID, userId)
                .get()
                .await()
                .documents
        }
    }

    private suspend fun deletePostSubcollection(postId: String, collectionName: String) {
        val collection = postsCollection().document(postId).collection(collectionName)
        while (true) {
            val documents = collection
                .limit(FirebaseLimits.MAX_BATCH_WRITE_SIZE.toLong())
                .get()
                .await()
                .documents
            if (documents.isEmpty()) return

            db.runBatch { batch ->
                documents.forEach { document -> batch.delete(document.reference) }
            }.await()
        }
    }

    private fun postsCollection() = db.collection(FirebaseCollections.POSTS)
}

class FirebasePostsCursor internal constructor(
    internal val document: DocumentSnapshot
)

data class FirebasePostsPage(
    val posts: List<PostEntity>,
    val cursor: FirebasePostsCursor?
)
