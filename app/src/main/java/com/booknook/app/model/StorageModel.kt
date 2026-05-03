package com.booknook.app.model

import android.content.Context
import android.net.Uri
import com.booknook.app.model.cloudinary.CloudinaryService

class StorageModel(context: Context) {

    private val cloudinaryService = CloudinaryService(context.applicationContext)

    suspend fun uploadAvatar(_uid: String, uri: Uri): String? {
        return cloudinaryService.uploadImageToCloudinary(uri)
    }

    suspend fun uploadPostImage(_userId: String, _postId: String, uri: Uri): String? {
        return cloudinaryService.uploadImageToCloudinary(uri)
    }
}
