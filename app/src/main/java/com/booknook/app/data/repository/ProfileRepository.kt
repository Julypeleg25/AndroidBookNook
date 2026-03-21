package com.booknook.app.data.repository

import android.net.Uri
import androidx.lifecycle.LiveData
import com.booknook.app.data.local.entities.UserEntity
import com.booknook.app.model.firebase.FirebaseModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProfileRepository(
    private val local: AppLocalRepository,
    private val firebase: FirebaseModel
) {
    fun observeLocalUser(): LiveData<UserEntity?> = local.observeUser()

    suspend fun ensureLocalProfile() {
        val profile = firebase.fetchProfile() ?: return
        withContext(Dispatchers.IO) {
            local.upsertUser(profile)
        }
    }

    suspend fun updateProfile(username: String, email: String, avatarUri: Uri?): UserEntity {
        val updated = firebase.updateProfile(username, email, avatarUri)
        withContext(Dispatchers.IO) {
            local.upsertUser(updated)
        }
        return updated
    }

    suspend fun clearLocalProfile() {
        withContext(Dispatchers.IO) {
            local.clearUser()
        }
    }
}
