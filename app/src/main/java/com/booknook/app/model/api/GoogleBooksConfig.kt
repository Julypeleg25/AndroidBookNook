package com.booknook.app.model.api

import com.booknook.app.BuildConfig

object GoogleBooksConfig {
    val apiKey: String = BuildConfig.GOOGLE_BOOKS_API_KEY.trim()

    fun apiKeyOrNull(): String? = apiKey.takeIf { it.isNotBlank() }
}
