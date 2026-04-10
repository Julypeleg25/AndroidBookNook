package com.booknook.app.model.firebase

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class FirebaseStorageModel {
    private val storage = FirebaseStorage.getInstance()

    suspend fun uploadAvatar(uid: String, uri: Uri): String {
        val ref = storage.reference.child("avatars/$uid.jpg")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun uploadPostImage(userId: String, postId: String, uri: Uri): String {
        val ref = storage.reference.child("posts/$userId/$postId.jpg")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun deletePostImage(userId: String, postId: String) {
        try {
            storage.reference.child("posts/$userId/$postId.jpg").delete().await()
        } catch (e: Exception) {
        }
    }
}
