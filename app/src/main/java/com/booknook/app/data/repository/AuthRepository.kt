package com.booknook.app.data.repository

import android.net.Uri
import com.booknook.app.data.local.LocalCacheDataSource
import com.booknook.app.model.StorageModel
import com.booknook.app.model.firebase.FirebaseAuthModel
import com.booknook.app.model.firebase.FirebaseProfileModel
import com.booknook.app.util.Logger
import kotlinx.coroutines.flow.StateFlow

class AuthRepository(
    private val auth: FirebaseAuthModel,
    private val profileModel: FirebaseProfileModel,
    private val profileRepository: ProfileRepository,
    private val listsRepository: ListsRepository,
    private val storageModel: StorageModel,
    private val local: LocalCacheDataSource
) {
    val authState: StateFlow<String?> = auth.observeAuthUserId()

    fun currentUserId(): String? = auth.currentUserId()

    fun isLoggedIn(): Boolean = currentUserId() != null

    suspend fun login(email: String, password: String) {
        auth.login(email, password)
        local.clearUserScopedData()
        profileRepository.refreshProfile()
        refreshListsAfterAuth()
    }

    suspend fun register(email: String, password: String, username: String, avatarUri: Uri?) {
        val uid = auth.register(email, password, username)

        val avatarUrl = avatarUri
            ?.let { storageModel.uploadAvatar(it) }
            ?.takeIf { it.isNotBlank() }

        profileModel.createUserProfile(uid, username, email, avatarUrl)

        local.clearUserScopedData()
        profileRepository.refreshProfile()
        refreshListsAfterAuth()
    }

    suspend fun logout() {
        local.clearUserScopedData()
        auth.logout()
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
