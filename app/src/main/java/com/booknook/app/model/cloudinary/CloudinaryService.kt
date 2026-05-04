package com.booknook.app.model.cloudinary

import android.content.Context
import android.net.Uri
import com.booknook.app.util.Logger
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

    suspend fun uploadImage(uri: Uri, uploadContext: String): String {
        return withContext(Dispatchers.IO) {
            validateConfig()
            val file = createTempUploadFile(uri)

            try {
                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val multipartBody = MultipartBody.Part.createFormData("file", file.name, requestFile)
                val presetBody = CloudinaryConfig.uploadPreset.toRequestBody("text/plain".toMediaTypeOrNull())
                val response = api.uploadImage(multipartBody, presetBody)

                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string().orEmpty()
                    throw IllegalStateException("Image upload failed: ${response.code()} $errorBody")
                }

                response.body()?.secure_url?.takeIf { it.isNotBlank() }
                    ?: throw IllegalStateException("Image upload failed: missing URL")
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to upload $uploadContext", e)
                throw IllegalStateException("Image upload failed", e)
            } finally {
                file.delete()
            }
        }
    }

    private fun validateConfig() {
        if (!CloudinaryConfig.isCloudNameConfigured()) {
            throw IllegalStateException("Image upload failed: Cloudinary cloud name is missing")
        }
        if (!CloudinaryConfig.isUploadPresetConfigured()) {
            throw IllegalStateException("Image upload failed: Cloudinary upload preset is missing")
        }
    }

    private fun createTempUploadFile(uri: Uri): File {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Image upload failed: cannot read selected file")
            val file = File(context.cacheDir, "$TEMP_FILE_PREFIX${System.currentTimeMillis()}$TEMP_FILE_EXTENSION")
            inputStream.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to create upload temp file", e)
            throw IllegalStateException("Image upload failed", e)
        }
    }

    companion object {
        private const val TAG = "CloudinaryService"
        private const val TEMP_FILE_PREFIX = "upload_image_"
        private const val TEMP_FILE_EXTENSION = ".jpg"
    }
}
