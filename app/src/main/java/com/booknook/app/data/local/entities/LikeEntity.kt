package com.booknook.app.data.local.entities

import androidx.room.Entity

@Entity(tableName = "likes", primaryKeys = ["userId", "postId"])
data class LikeEntity(
    val userId: String,
    val postId: String,
    val createdAt: Long = System.currentTimeMillis()
)
