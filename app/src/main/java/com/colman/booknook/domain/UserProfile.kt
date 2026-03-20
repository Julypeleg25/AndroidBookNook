package com.colman.booknook.domain

data class UserProfile(
    val id: String,
    val username: String,
    val email: String,
    val avatarUrl: String?
)
