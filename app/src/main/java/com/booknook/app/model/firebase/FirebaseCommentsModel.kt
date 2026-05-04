package com.booknook.app.model.firebase

import com.booknook.app.data.local.entities.CommentEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseCommentsModel(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val authModel: FirebaseAuthModel
) {
    suspend fun addComment(postId: String, text: String) {
        val uid = authModel.requireUserId()
        val profile = fetchCurrentAuthorProfile(uid)
        val commentId = UUID.randomUUID().toString()
        val commentData = mapOf(
            FirebaseFields.ID to commentId,
            FirebaseFields.USER_ID to uid,
            FirebaseFields.USERNAME to (profile.username ?: FirebaseDefaults.USERNAME),
            FirebaseFields.USER_AVATAR_URL to profile.avatarUrl,
            FirebaseFields.TEXT to text,
            FirebaseFields.CREATED_AT to System.currentTimeMillis()
        )

        val postRef = postsCollection().document(postId)
        val commentRef = postRef
            .collection(FirebaseCollections.COMMENTS)
            .document(commentId)

        db.runBatch { batch ->
            batch.set(commentRef, commentData)
            batch.update(postRef, FirebaseFields.COMMENTS_COUNT, FieldValue.increment(1))
        }.await()
    }

    suspend fun fetchComments(postId: String): List<CommentEntity> {
        val snap = postsCollection().document(postId)
            .collection(FirebaseCollections.COMMENTS)
            .orderBy(FirebaseFields.CREATED_AT)
            .get()
            .await()
        val authorProfiles = fetchCommentAuthorProfiles(
            snap.documents.mapNotNull { document ->
                document.getString(FirebaseFields.USER_ID)?.takeIf { it.isNotBlank() }
            }.distinct()
        )

        return snap.documents.map { doc ->
            val commentUserId = doc.getString(FirebaseFields.USER_ID).orEmpty()
            val authorProfile = authorProfiles[commentUserId]
            CommentEntity(
                id = doc.getString(FirebaseFields.ID).orEmpty(),
                postId = postId,
                userId = commentUserId,
                username = authorProfile?.username
                    ?: doc.getString(FirebaseFields.USERNAME)
                    ?: FirebaseDefaults.USERNAME,
                text = doc.getString(FirebaseFields.TEXT).orEmpty(),
                userAvatarUrl = authorProfile?.avatarUrl ?: doc.getString(FirebaseFields.USER_AVATAR_URL),
                createdAt = doc.getLong(FirebaseFields.CREATED_AT) ?: 0L
            )
        }
    }

    private suspend fun fetchCurrentAuthorProfile(userId: String): CommentAuthorProfile {
        val doc = usersCollection().document(userId).get().await()
        return CommentAuthorProfile(
            username = doc.getString(FirebaseFields.USERNAME)?.takeIf { it.isNotBlank() }
                ?: auth.currentUser?.displayName,
            avatarUrl = doc.getString(FirebaseFields.AVATAR_URL)
        )
    }

    private suspend fun fetchCommentAuthorProfiles(userIds: List<String>): Map<String, CommentAuthorProfile> {
        if (userIds.isEmpty()) return emptyMap()

        val authUser = auth.currentUser
        val profiles = mutableMapOf<String, CommentAuthorProfile>()

        userIds.chunked(FirebaseLimits.USER_QUERY_CHUNK_SIZE).forEach { chunk ->
            usersCollection()
                .whereIn(FieldPath.documentId(), chunk)
                .get()
                .await()
                .documents
                .forEach { document ->
                    profiles[document.id] = CommentAuthorProfile(
                        username = document.getString(FirebaseFields.USERNAME)?.takeIf { it.isNotBlank() },
                        avatarUrl = document.getString(FirebaseFields.AVATAR_URL)
                    )
                }
        }

        val currentUserId = authUser?.uid
        if (currentUserId != null && userIds.contains(currentUserId)) {
            val currentProfile = profiles[currentUserId]
            profiles[currentUserId] = CommentAuthorProfile(
                username = currentProfile?.username ?: authUser.displayName,
                avatarUrl = currentProfile?.avatarUrl
            )
        }

        return profiles
    }

    private fun usersCollection() = db.collection(FirebaseCollections.USERS)

    private fun postsCollection() = db.collection(FirebaseCollections.POSTS)
}

private data class CommentAuthorProfile(
    val username: String?,
    val avatarUrl: String?
)
