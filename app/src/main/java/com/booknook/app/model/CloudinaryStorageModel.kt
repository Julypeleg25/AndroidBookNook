package com.booknook.app.model

import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.booknook.app.base.MyApplication
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class CloudinaryStorageModel {

    init {
        try {
            val config = mapOf(
                "cloud_name" to "duutna6lt",
                "api_key" to "646421153716863",
                "api_secret" to "zbfAoiCiwRSW0_Hj39GzHerfnbU"
            )
            MyApplication.appContext?.let {
                MediaManager.init(it, config)
            }
        } catch (e: Exception) {
        }
    }

    suspend fun uploadAvatar(uid: String, uri: Uri): String = suspendCoroutine { continuation ->
        val context = MyApplication.appContext ?: run {
            continuation.resume("")
            return@suspendCoroutine
        }
        
        MediaManager.get().upload(uri)
            .option("folder", "avatars")
            .option("public_id", uid)
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    continuation.resume(resultData["secure_url"] as? String ?: "")
                }
                override fun onError(requestId: String, error: ErrorInfo) {
                    continuation.resume("")
                }
                override fun onReschedule(requestId: String, error: ErrorInfo) {}
            })
            .dispatch()
    }

    suspend fun uploadPostImage(userId: String, postId: String, uri: Uri): String = suspendCoroutine { continuation ->
        MediaManager.get().upload(uri)
            .option("folder", "posts/$userId")
            .option("public_id", postId)
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    continuation.resume(resultData["secure_url"] as? String ?: "")
                }
                override fun onError(requestId: String, error: ErrorInfo) {
                    continuation.resume("")
                }
                override fun onReschedule(requestId: String, error: ErrorInfo) {}
            })
            .dispatch()
    }
}
