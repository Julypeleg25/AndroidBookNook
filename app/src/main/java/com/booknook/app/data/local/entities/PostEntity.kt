package com.booknook.app.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "posts",
    indices = [Index("bookTitle"), Index("bookAuthor"), Index("rating"), Index("commentsCount")]
)
data class PostEntity(
    @PrimaryKey val id: String = "",
    val userId: String = "",
    val username: String = "",
    val bookId: String = "",
    val bookTitle: String = "",
    val bookAuthor: String = "",
    val bookThumbnail: String? = null,
    val rating: Int = 0,
    val review: String = "",
    val imageUrl: String? = null,
    val createdAt: Long = 0,
    val likesCount: Int = 0,
    val commentsCount: Int = 0
)
