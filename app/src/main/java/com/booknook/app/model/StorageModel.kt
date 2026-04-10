package com.booknook.app.model

import android.net.Uri

class StorageModel {

    enum class StorageAPI {
        FIREBASE,
        CLOUDINARY
    }

    private val firebaseStorage = com.booknook.app.model.firebase.FirebaseStorageModel()
    private val cloudinaryStorage = com.booknook.app.model.CloudinaryStorageModel()

    suspend fun uploadAvatar(uid: String, uri: Uri, api: StorageAPI = StorageAPI.FIREBASE): String {
        return when (api) {
            StorageAPI.FIREBASE -> firebaseStorage.uploadAvatar(uid, uri)
            StorageAPI.CLOUDINARY -> cloudinaryStorage.uploadAvatar(uid, uri)
        }
    }

    suspend fun uploadPostImage(userId: String, postId: String, uri: Uri, api: StorageAPI = StorageAPI.FIREBASE): String {
        return when (api) {
            StorageAPI.FIREBASE -> firebaseStorage.uploadPostImage(userId, postId, uri)
            StorageAPI.CLOUDINARY -> cloudinaryStorage.uploadPostImage(userId, postId, uri)
        }
    }

    suspend fun deletePostImage(userId: String, postId: String, api: StorageAPI = StorageAPI.FIREBASE) {
        when (api) {
            StorageAPI.FIREBASE -> firebaseStorage.deletePostImage(userId, postId)
            StorageAPI.CLOUDINARY -> { }
        }
    }
}
