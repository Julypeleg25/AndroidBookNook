package com.booknook.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_books")
data class CachedBookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val thumbnail: String?,
    val publishedDate: String?,
    val genre: String?,
    val pageCount: Int?,
    val description: String?,
    val lastFetchedAt: Long
)
