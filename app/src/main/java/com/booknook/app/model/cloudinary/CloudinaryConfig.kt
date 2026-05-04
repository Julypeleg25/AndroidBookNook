package com.booknook.app.model.cloudinary

import com.booknook.app.BuildConfig

object CloudinaryConfig {
    val cloudName: String = BuildConfig.CLOUDINARY_CLOUD_NAME.trim()
    val uploadPreset: String = BuildConfig.CLOUDINARY_UPLOAD_PRESET.trim()

    fun isCloudNameConfigured(): Boolean = cloudName.isNotBlank()
    fun isUploadPresetConfigured(): Boolean = uploadPreset.isNotBlank()
}
