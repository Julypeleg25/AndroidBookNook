package com.booknook.app.ui.auth

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.booknook.app.model.Model
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<Event<String>>()
    val error: LiveData<Event<String>> = _error

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
                Model.firebase.login(email, password)
                // Proactively sync profile before navigation for better UI consistency
                Model.ensureLocalProfile()
                _loginSuccess.value = Event(Unit)
            } catch (e: Exception) {
                _error.value = Event(mapErrorMessage(e))
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
                Model.firebase.register(email, password, username, avatarUri)
                // Proactively sync profile
                Model.ensureLocalProfile()
                _registrationSuccess.value = Event(Unit)
            } catch (e: Exception) {
                _error.value = Event(mapErrorMessage(e))
            } finally {
                _loading.value = false
                isProcessing = false
            }
        }
    }

    private fun mapErrorMessage(e: Exception): String {
        val msg = e.message ?: return "An unexpected error occurred"
        return when {
            msg.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) -> "Invalid email or password"
            msg.contains("network", ignoreCase = true) || msg.contains("offline", ignoreCase = true) -> "Device is offline. Please check your internet connection."
            msg.contains("email address is badly formatted", ignoreCase = true) -> "Invalid email format"
            msg.contains("at least 6 characters", ignoreCase = true) -> "Password must be at least 6 characters"
            msg.contains("email address is already", ignoreCase = true) -> "This email is already registered"
            msg.contains("PERMISSION_DENIED", ignoreCase = true) || msg.contains("API has not been used", ignoreCase = true) -> 
                "Project error: Firestore API is disabled in Firebase Console."
            msg.contains("Timed out", ignoreCase = true) -> 
                "Connection timed out. If your internet is fine, check if Firestore API is enabled in Firebase Console."
            else -> msg
        }
    }
}
