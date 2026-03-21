package com.booknook.app.data.repository

import android.net.Uri
import com.booknook.app.model.firebase.FirebaseModel

class AuthRepository(
    private val firebase: FirebaseModel,
    private val profileRepository: ProfileRepository
) {
    fun currentUserId(): String? = firebase.currentUserId()

    suspend fun login(email: String, password: String) {
        firebase.login(email, password)
        profileRepository.ensureLocalProfile()
    }

    suspend fun register(email: String, password: String, username: String, avatarUri: Uri?) {
        firebase.register(email, password, username, avatarUri)
        profileRepository.ensureLocalProfile()
    }

    suspend fun logout() {
        firebase.logout()
        profileRepository.clearLocalProfile()
    }
}
