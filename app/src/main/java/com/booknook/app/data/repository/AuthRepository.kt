package com.booknook.app.data.repository

import android.net.Uri
import com.booknook.app.model.firebase.FirebaseModel
import com.booknook.app.model.StorageModel

class AuthRepository(
    private val firebase: FirebaseModel,
    private val profileRepository: ProfileRepository,
    private val storageModel: StorageModel
) {
    fun currentUserId(): String? = firebase.currentUserId()

    suspend fun login(email: String, password: String) {
        firebase.login(email, password)
        profileRepository.ensureLocalProfile()
    }

    suspend fun register(email: String, password: String, username: String, avatarUri: Uri?) {
        val uid = firebase.register(email, password, username)
        
        val avatarUrl = avatarUri?.let { storageModel.uploadAvatar(uid, it) }
        
        firebase.createUserProfile(uid, username, email, avatarUrl)
        
        profileRepository.ensureLocalProfile()
    }

    suspend fun logout() {
        firebase.logout()
        profileRepository.clearLocalProfile()
    }
}
