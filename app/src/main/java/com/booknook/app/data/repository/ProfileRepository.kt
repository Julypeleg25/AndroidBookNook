package com.booknook.app.data.repository

import android.net.Uri
import androidx.lifecycle.LiveData
import com.booknook.app.data.local.LocalCacheDataSource
import com.booknook.app.data.local.entities.UserEntity
import com.booknook.app.model.StorageModel
import com.booknook.app.model.firebase.FirebaseAuthModel
import com.booknook.app.model.firebase.FirebaseProfileModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProfileRepository(
    private val local: LocalCacheDataSource,
    private val auth: FirebaseAuthModel,
    private val profileModel: FirebaseProfileModel,
    private val storageModel: StorageModel
) {
    fun observeProfile(): LiveData<UserEntity?> = local.observeUser()

    suspend fun refreshProfile() {
        val profile = profileModel.fetchProfile() ?: return
        withContext(Dispatchers.IO) {
            local.upsertUser(profile)
            local.updateUserContentProfile(profile.id, profile.username, profile.avatarUrl)
        }
    }

    suspend fun updateProfile(username: String, email: String, avatarUri: Uri?): UserEntity {
        auth.requireUserId()
        val avatarUrl = avatarUri
            ?.let { storageModel.uploadAvatar(it) }
            ?.takeIf { it.isNotBlank() }
            ?: profileModel.fetchProfile()?.avatarUrl
        val updated = profileModel.updateProfile(username, email, avatarUrl)
        withContext(Dispatchers.IO) {
            local.upsertUser(updated)
            local.updateUserContentProfile(updated.id, updated.username, updated.avatarUrl)
        }
        return updated
    }

}
