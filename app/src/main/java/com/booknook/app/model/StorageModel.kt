package com.booknook.app.model

import android.content.Context
import android.net.Uri
import com.booknook.app.model.cloudinary.CloudinaryService
import com.booknook.app.model.storage.StorageConstants

class StorageModel(context: Context) {

    private val cloudinaryService = CloudinaryService(context.applicationContext)

    suspend fun uploadAvatar(uri: Uri): String {
        return cloudinaryService.uploadImage(uri, StorageConstants.AVATAR_UPLOAD_CONTEXT)
    }

    suspend fun uploadPostImage(uri: Uri): String {
        return cloudinaryService.uploadImage(uri, StorageConstants.POST_IMAGE_UPLOAD_CONTEXT)
    }
}
