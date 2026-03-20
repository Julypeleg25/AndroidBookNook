package com.booknook.app.ui.profile

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.booknook.app.data.local.entities.UserEntity
import com.booknook.app.model.Model
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _updateSuccess = MutableLiveData<Boolean>()
    val updateSuccess: LiveData<Boolean> = _updateSuccess

    val user: LiveData<UserEntity?> = Model.observeLocalUser()

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
                Model.ensureLocalProfile()
            } catch (_: Exception) { }
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
                Model.updateProfile(username, email, avatarUri)
                _updateSuccess.value = true
                inputAvatarUri.value = null 
            } catch (e: Exception) {
                _error.value = mapErrorMessage(e)
            } finally {
                _loading.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            Model.logoutAsync()
        }
    }

    fun resetUpdateSuccess() {
        _updateSuccess.value = false
    }

    private fun mapErrorMessage(e: Exception): String {
        val msg = e.message ?: return "Update failed"
        return when {
            msg.contains("network", ignoreCase = true) || msg.contains("offline", ignoreCase = true) -> 
                "Device is offline. Changes will sync when online."
            msg.contains("Timed out", ignoreCase = true) -> "Connection timed out."
            else -> msg
        }
    }
}
