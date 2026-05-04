package com.booknook.app.model.firebase

import com.booknook.app.data.local.entities.UserEntity
import com.booknook.app.util.Logger
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class FirebaseProfileModel(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val authModel: FirebaseAuthModel
) {
    suspend fun createUserProfile(uid: String, username: String, email: String, avatarUrl: String?) {
        val data = mapOf(
            FirebaseFields.USERNAME to username,
            FirebaseFields.EMAIL to email,
            FirebaseFields.AVATAR_URL to avatarUrl
        )
        withTimeout(FirebaseLimits.PROFILE_CREATE_TIMEOUT_MS) {
            withContext(Dispatchers.IO) {
                usersCollection().document(uid).set(data).await()
            }
        }
    }

    suspend fun fetchProfile(): UserEntity? {
        val user = auth.currentUser ?: return null
        val doc = usersCollection().document(user.uid).get().await()
        return UserEntity(
            id = user.uid,
            username = doc.getString(FirebaseFields.USERNAME)?.takeIf { it.isNotBlank() }
                ?: user.displayName
                ?: FirebaseDefaults.USERNAME,
            email = doc.getString(FirebaseFields.EMAIL)?.takeIf { it.isNotBlank() }
                ?: user.email.orEmpty(),
            avatarUrl = doc.getString(FirebaseFields.AVATAR_URL)
        )
    }

    suspend fun updateProfile(username: String, email: String, avatarUrl: String?): UserEntity {
        val uid = authModel.requireUserId()
        val user = auth.currentUser ?: throw IllegalStateException("No auth user")
        if (user.displayName != username) {
            user.updateProfile(userProfileChangeRequest { displayName = username }).await()
        }

        val resolvedEmail = user.email ?: email
        usersCollection().document(uid).set(
            mapOf(
                FirebaseFields.USERNAME to username,
                FirebaseFields.EMAIL to resolvedEmail,
                FirebaseFields.AVATAR_URL to avatarUrl
            ),
            SetOptions.merge()
        ).await()

        runCatching {
            syncUserContentProfile(uid, username, avatarUrl)
        }.onFailure { error ->
            Logger.e(
                "Profile",
                "Profile saved, but syncing historical posts/comments failed. Keeping the profile update.",
                error
            )
        }

        return UserEntity(id = uid, username = username, email = resolvedEmail, avatarUrl = avatarUrl)
    }

    private suspend fun syncUserContentProfile(userId: String, username: String, avatarUrl: String?) {
        val postDocuments = postsCollection()
            .whereEqualTo(FirebaseFields.USER_ID, userId)
            .get()
            .await()
            .documents
        commitBatchedUpdates(postDocuments) { batch, document ->
            batch.update(document.reference, FirebaseFields.USERNAME, username)
        }

        val commentDocuments = db.collectionGroup(FirebaseCollections.COMMENTS)
            .whereEqualTo(FirebaseFields.USER_ID, userId)
            .get()
            .await()
            .documents
        commitBatchedUpdates(commentDocuments) { batch, document ->
            batch.update(document.reference, FirebaseFields.USERNAME, username)
            if (avatarUrl.isNullOrBlank()) {
                batch.update(document.reference, FirebaseFields.USER_AVATAR_URL, FieldValue.delete())
            } else {
                batch.update(document.reference, FirebaseFields.USER_AVATAR_URL, avatarUrl)
            }
        }
    }

    private suspend fun commitBatchedUpdates(
        documents: List<DocumentSnapshot>,
        applyUpdate: (WriteBatch, DocumentSnapshot) -> Unit
    ) {
        documents.chunked(FirebaseLimits.MAX_BATCH_WRITE_SIZE).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { document -> applyUpdate(batch, document) }
            batch.commit().await()
        }
    }

    private fun usersCollection() = db.collection(FirebaseCollections.USERS)

    private fun postsCollection() = db.collection(FirebaseCollections.POSTS)
}
