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

    fun updateProfile(username: String, email: String, avatarUri: Uri?) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                Model.updateProfile(username, email, avatarUri)
                _updateSuccess.value = true
            } catch (e: Exception) {
                _error.value = e.message ?: "Update failed"
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
}
