package com.booknook.app.ui.auth

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.booknook.app.R
import com.booknook.app.model.Model
import com.booknook.app.util.toUserFriendlyMessageRes
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<Event<Int>>()
    val error: LiveData<Event<Int>> = _error

    private val _registrationSuccess = MutableLiveData<Event<Unit>>()
    val registrationSuccess: LiveData<Event<Unit>> = _registrationSuccess

    private val _loginSuccess = MutableLiveData<Event<Unit>>()
    val loginSuccess: LiveData<Event<Unit>> = _loginSuccess

    private var isProcessing = false

    fun login(email: String, password: String) {
        if (isProcessing) return
        
        isProcessing = true
        _loading.value = true
        
        viewModelScope.launch {
            try {
                Model.authRepository.login(email, password)
                _loginSuccess.value = Event(Unit)
            } catch (e: Exception) {
                _error.value = Event(e.toUserFriendlyMessageRes(R.string.error_generic))
            } finally {
                _loading.value = false
                isProcessing = false
            }
        }
    }

    fun register(email: String, password: String, username: String, avatarUri: Uri? = null) {
        if (isProcessing) return

        isProcessing = true
        _loading.value = true
        
        viewModelScope.launch {
            try {
                Model.authRepository.register(email, password, username, avatarUri)
                _registrationSuccess.value = Event(Unit)
            } catch (e: Exception) {
                _error.value = Event(e.toUserFriendlyMessageRes(R.string.error_generic))
            } finally {
                _loading.value = false
                isProcessing = false
            }
        }
    }
}
