package com.colman.booknook.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_books")
data class CachedBookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val thumbnail: String?,
    val lastFetchedAt: Long
)
