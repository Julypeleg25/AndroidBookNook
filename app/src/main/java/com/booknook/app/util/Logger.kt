package com.booknook.app.util

import android.util.Log
import com.booknook.app.BuildConfig

object Logger {
    private const val TAG = "BookNook_Action"

    fun d(action: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "[$action] $message")
        }
    }

    fun e(action: String, message: String, throwable: Throwable? = null) {
        Log.e(TAG, "[$action] ERROR: $message", throwable)
    }

    fun i(action: String, message: String) {
        Log.i(TAG, "[$action] $message")
    }
}
