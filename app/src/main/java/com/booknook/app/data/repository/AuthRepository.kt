package com.booknook.app.data.repository

import android.net.Uri
import com.booknook.app.data.local.LocalCacheDataSource
import com.booknook.app.model.StorageModel
import com.booknook.app.model.firebase.FirebaseModel
import com.booknook.app.util.Logger
import kotlinx.coroutines.flow.StateFlow

class AuthRepository(
    private val firebase: FirebaseModel,
    private val profileRepository: ProfileRepository,
    private val listsRepository: ListsRepository,
    private val storageModel: StorageModel,
    private val local: LocalCacheDataSource
) {
    val authState: StateFlow<String?> = firebase.observeAuthUserId()

    fun currentUserId(): String? = firebase.currentUserId()

    fun isLoggedIn(): Boolean = currentUserId() != null

    suspend fun login(email: String, password: String) {
        firebase.login(email, password)
        local.clearUserScopedData()
        profileRepository.refreshProfile()
        refreshListsAfterAuth()
    }

    suspend fun register(email: String, password: String, username: String, avatarUri: Uri?) {
        val uid = firebase.register(email, password, username)

        val avatarUrl = avatarUri
            ?.let { storageModel.uploadAvatar(uid, it) }
            ?.takeIf { it.isNotBlank() }

        firebase.createUserProfile(uid, username, email, avatarUrl)

        local.clearUserScopedData()
        profileRepository.refreshProfile()
        refreshListsAfterAuth()
    }

    suspend fun logout() {
        local.clearUserScopedData()
        firebase.logout()
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
