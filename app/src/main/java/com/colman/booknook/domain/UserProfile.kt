package com.booknook.app.domain

data class UserProfile(
    val id: String,
    val username: String,
    val email: String,
    val avatarUrl: String?
)
