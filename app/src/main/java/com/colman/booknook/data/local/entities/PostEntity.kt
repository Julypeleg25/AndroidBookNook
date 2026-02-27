package com.colman.booknook.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "posts",
    indices = [Index("bookTitle"), Index("bookAuthor"), Index("rating"), Index("commentsCount")]
)
data class PostEntity(
    @PrimaryKey val id: String,
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
