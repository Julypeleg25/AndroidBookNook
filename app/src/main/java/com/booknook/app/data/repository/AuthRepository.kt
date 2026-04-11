package com.booknook.app.data.repository

import android.net.Uri
import com.booknook.app.data.local.LocalCacheDataSource
import com.booknook.app.model.StorageModel
import com.booknook.app.model.firebase.FirebaseModel
import com.booknook.app.util.Logger

class AuthRepository(
    private val firebase: FirebaseModel,
    private val profileRepository: ProfileRepository,
    private val listsRepository: ListsRepository,
    private val storageModel: StorageModel,
    private val local: LocalCacheDataSource
) {
    fun currentUserId(): String? = firebase.currentUserId()

    fun isLoggedIn(): Boolean = currentUserId() != null

    suspend fun login(email: String, password: String) {
        firebase.login(email, password)
        profileRepository.refreshProfile()
        refreshListsAfterAuth()
    }

    suspend fun register(email: String, password: String, username: String, avatarUri: Uri?) {
        val uid = firebase.register(email, password, username)

        val avatarUrl = avatarUri
            ?.let { storageModel.uploadAvatar(uid, it) }
            ?.takeIf { it.isNotBlank() }

        firebase.createUserProfile(uid, username, email, avatarUrl)

        profileRepository.refreshProfile()
        refreshListsAfterAuth()
    }

    suspend fun logout() {
        firebase.logout()
        local.clearAllData()
    }

    private suspend fun refreshListsAfterAuth() {
        val userId = currentUserId() ?: return
        runCatching {
            listsRepository.refreshLists(userId)
        }.onFailure { error ->
            Logger.e("Lists", "Failed to sync saved lists after auth", error)
        }
    }
}
