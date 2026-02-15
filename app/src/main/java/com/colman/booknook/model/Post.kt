package com.booknook.app.model

data class Post(
    val id: String,
    val userId: String,
    val username: String,
    val bookId: String,
    val bookTitle: String,
    val bookAuthor: String,
    val bookThumbnail: String?,
    val rating: Int,
    val review: String,
    val imageUrl: String?,
    val createdAt: Long,
    val likesCount: Int,
    val commentsCount: Int
)
