package com.booknook.app.ui.auth

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.booknook.app.R
import com.booknook.app.data.repository.AuthRepository
import com.booknook.app.util.Event
import com.booknook.app.util.toUserFriendlyMessageRes
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableLiveData(AuthUiState())
    val uiState: LiveData<AuthUiState> = _uiState

    private val _event = MutableLiveData<Event<AuthEvent>>()
    val event: LiveData<Event<AuthEvent>> = _event

    private var isProcessing = false

    fun onLoginSubmitted(email: String, password: String) {
        if (isProcessing) return

        updateLoading(true)
        
        viewModelScope.launch {
            try {
                authRepository.login(email, password)
                _event.value = Event(AuthEvent.NavigateToPosts(R.string.login_success_message))
            } catch (e: Exception) {
                _event.value = Event(AuthEvent.ShowMessage(e.toUserFriendlyMessageRes(R.string.error_generic)))
            } finally {
                updateLoading(false)
            }
        }
    }

    fun onRegisterSubmitted(email: String, password: String, username: String, avatarUri: Uri? = null) {
        if (isProcessing) return

        updateLoading(true)
        
        viewModelScope.launch {
            try {
                authRepository.register(email, password, username, avatarUri)
                _event.value = Event(AuthEvent.NavigateToPosts(R.string.register_success_message))
            } catch (e: Exception) {
                _event.value = Event(AuthEvent.ShowMessage(e.toUserFriendlyMessageRes(R.string.error_generic)))
            } finally {
                updateLoading(false)
            }
        }
    }

    fun onRegisterLinkClicked() {
        _event.value = Event(AuthEvent.NavigateToRegister)
    }

    fun onBackToLoginClicked() {
        _event.value = Event(AuthEvent.NavigateBackToLogin)
    }

    private fun updateLoading(isLoading: Boolean) {
        isProcessing = isLoading
        _uiState.value = _uiState.value?.copy(isLoading = isLoading)
    }

    companion object {
        fun factory(authRepository: AuthRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                        return AuthViewModel(authRepository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}

data class AuthUiState(
    val isLoading: Boolean = false
)

sealed interface AuthEvent {
    data object NavigateToRegister : AuthEvent
    data object NavigateBackToLogin : AuthEvent
    data class NavigateToPosts(@StringRes val messageRes: Int) : AuthEvent
    data class ShowMessage(@StringRes val messageRes: Int) : AuthEvent
}
