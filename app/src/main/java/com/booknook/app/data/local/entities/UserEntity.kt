package com.booknook.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user")
data class UserEntity(
    @PrimaryKey val id: String = "",
    val username: String = "",
    val email: String = "",
    val avatarUrl: String? = null
)
