package com.booknook.app.data.repository

import android.net.Uri
import androidx.lifecycle.LiveData
import com.booknook.app.data.local.LocalCacheDataSource
import com.booknook.app.data.local.entities.UserEntity
import com.booknook.app.model.firebase.FirebaseModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProfileRepository(
    private val local: LocalCacheDataSource,
    private val firebase: FirebaseModel,
    private val storageModel: com.booknook.app.model.StorageModel
) {
    fun observeProfile(): LiveData<UserEntity?> = local.observeUser()

    suspend fun refreshProfile() {
        val profile = firebase.fetchProfile() ?: return
        withContext(Dispatchers.IO) {
            local.upsertUser(profile)
            local.updateUserContentProfile(profile.id, profile.username, profile.avatarUrl)
        }
    }

    suspend fun updateProfile(username: String, email: String, avatarUri: Uri?): UserEntity {
        val uid = firebase.requireUserId()
        val avatarUrl = avatarUri
            ?.let { storageModel.uploadAvatar(uid, it) }
            ?.takeIf { it.isNotBlank() }
            ?: firebase.fetchProfile()?.avatarUrl
        val updated = firebase.updateProfile(username, email, avatarUrl)
        withContext(Dispatchers.IO) {
            local.upsertUser(updated)
            local.updateUserContentProfile(updated.id, updated.username, updated.avatarUrl)
        }
        return updated
    }

    suspend fun clearLocalProfile() {
        withContext(Dispatchers.IO) {
            local.clearUser()
        }
    }
}
