package com.booknook.app.domain

data class Book(
    val id: String,
    val title: String,
    val author: String,
    val thumbnail: String?,
    val publishedDate: String? = null,
    val genre: String? = null,
    val pageCount: Int? = null,
    val description: String? = null
)
