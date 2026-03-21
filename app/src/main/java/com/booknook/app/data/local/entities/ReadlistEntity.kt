package com.booknook.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "readlist")
data class ReadlistEntity(
    @PrimaryKey val key: String,
    val userId: String,
    val bookId: String,
    val title: String,
    val author: String,
    val thumbnail: String?,
    val addedAt: Long,
    val genre: String? = null,
    val publishedDate: String? = null,
    val pageCount: Int? = null,
    val description: String? = null
)
