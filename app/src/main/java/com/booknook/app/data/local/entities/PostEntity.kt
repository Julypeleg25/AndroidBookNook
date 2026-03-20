package com.booknook.app.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Ignore

@Entity(
    tableName = "posts",
    indices = [Index("bookTitle"), Index("bookAuthor"), Index("rating")]
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
    val commentsCount: Int = 0,
    val bookPublishedDate: String? = null,
    val bookGenre: String? = null,
    val bookPageCount: Int? = null,
    val bookDescription: String? = null,
    val isLikedByUser: Boolean = false
)
