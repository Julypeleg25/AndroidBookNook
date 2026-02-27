package com.colman.booknook.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wishlist")
data class WishlistEntity(
    @PrimaryKey val key: String, // userId|bookId
    val userId: String,
    val bookId: String,
    val title: String,
    val author: String,
    val thumbnail: String?,
    val addedAt: Long
)
