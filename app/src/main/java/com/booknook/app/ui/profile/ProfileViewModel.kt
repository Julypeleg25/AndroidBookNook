package com.booknook.app.ui.profile

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.booknook.app.R
import com.booknook.app.data.local.entities.UserEntity
import com.booknook.app.model.Model
import com.booknook.app.util.toUserFriendlyMessageRes
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<Int?>()
    val error: LiveData<Int?> = _error

    private val _updateSuccess = MutableLiveData<Boolean>()
    val updateSuccess: LiveData<Boolean> = _updateSuccess

    val user: LiveData<UserEntity?> = Model.profileRepository.observeLocalUser()

    val inputUsername = MutableLiveData<String>()
    val inputAvatarUri = MutableLiveData<Uri?>()

    val isChanged: LiveData<Boolean> = androidx.lifecycle.MediatorLiveData<Boolean>().apply {
        addSource(user) { value = checkChanged() }
        addSource(inputUsername) { value = checkChanged() }
        addSource(inputAvatarUri) { value = checkChanged() }
    }

    init {
        viewModelScope.launch {
            try {
                Model.profileRepository.ensureLocalProfile()
            } catch (e: Exception) {
                com.booknook.app.util.Logger.e("ProfileVM", "Failed to refresh local profile", e)
            }
        }
    }

    private fun checkChanged(): Boolean {
        val initial = user.value ?: return false
        val currentName = inputUsername.value ?: initial.username
        val currentAvatar = inputAvatarUri.value
        return (currentName.trim() != initial.username.trim()) || (currentAvatar != null)
    }

    fun updateProfile(username: String, email: String, avatarUri: Uri?) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                Model.profileRepository.updateProfile(username, email, avatarUri)
                _updateSuccess.value = true
                inputAvatarUri.value = null 
            } catch (e: Exception) {
                _error.value = e.toUserFriendlyMessageRes(R.string.profile_update_failed)
            } finally {
                _loading.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            Model.authRepository.logout()
        }
    }

    fun resetUpdateSuccess() {
        _updateSuccess.value = false
    }
}
