package com.booknook.app.data.local.entities

import androidx.room.*

@Entity(
    tableName = "comments",
    foreignKeys = [
        ForeignKey(
            entity = PostEntity::class,
            parentColumns = ["id"],
            childColumns = ["postId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("postId")]
)
data class CommentEntity(
    @PrimaryKey val id: String,
    val postId: String,
    val userId: String,
    val username: String,
    val text: String,
    val createdAt: Long,
    val userAvatarUrl: String? = null
)
