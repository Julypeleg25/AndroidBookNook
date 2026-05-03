package com.booknook.app.model.cloudinary

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class CloudinaryService(
    private val context: Context
) {
    private val api: CloudinaryApi by lazy {
        CloudinaryClient.createApi(CloudinaryConfig.cloudName)
    }

    suspend fun uploadImageToCloudinary(uri: Uri): String? {
        return withContext(Dispatchers.IO) {
            val cloudName = CloudinaryConfig.cloudName
            if (cloudName.isBlank()) {
                Log.e(TAG, "Cloudinary cloud name is missing")
                return@withContext null
            }

            val uploadPreset = CloudinaryConfig.uploadPreset
            if (uploadPreset.isBlank()) {
                Log.e(TAG, "Cloudinary upload preset is missing")
                return@withContext null
            }

            val file = getFileFromUri(uri)
            if (file == null) {
                Log.e(TAG, "Failed to create file from Uri")
                return@withContext null
            }

            try {
                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val multipartBody = MultipartBody.Part.createFormData("file", file.name, requestFile)
                val presetBody = uploadPreset.toRequestBody("text/plain".toMediaTypeOrNull())
                val response = api.uploadImage(multipartBody, presetBody)

                if (response.isSuccessful) {
                    response.body()?.secure_url
                } else {
                    Log.e(TAG, "Cloudinary upload failed: ${response.errorBody()?.string()}")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading image", e)
                null
            } finally {
                file.delete()
            }
        }
    }

    private fun getFileFromUri(uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val file = File(context.cacheDir, "upload_image_${System.currentTimeMillis()}.jpg")
            inputStream.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file
        } catch (e: Exception) {
            Log.e(TAG, "Error creating temp file from Uri", e)
            null
        }
    }

    companion object {
        private const val TAG = "CloudinaryService"
    }
}
