package com.booknook.app.ui.profile

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.booknook.app.R
import com.booknook.app.data.repository.AuthRepository
import com.booknook.app.data.repository.ProfileRepository
import com.booknook.app.data.local.entities.UserEntity
import com.booknook.app.util.Event
import com.booknook.app.util.toUserFriendlyMessageRes
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MediatorLiveData(ProfileUiState())
    val uiState: LiveData<ProfileUiState> = _uiState

    private val _event = MutableLiveData<Event<ProfileEvent>>()
    val event: LiveData<Event<ProfileEvent>> = _event

    private var currentUser: UserEntity? = null
    private var currentUserId: String? = authRepository.currentUserId()
    private var draftUsername: String = ""
    private var selectedAvatarUri: Uri? = null

    init {
        _uiState.value = ProfileUiState()
        _uiState.addSource(profileRepository.observeProfile()) { user ->
            val previousUser = currentUser
            currentUser = user
            val shouldSyncDraft = draftUsername.isBlank() ||
                previousUser?.id != user?.id ||
                draftUsername.trim() == previousUser?.username?.trim().orEmpty()

            if (shouldSyncDraft && user != null) {
                draftUsername = user.username
            }

            publishState()
        }

        viewModelScope.launch {
            try {
                profileRepository.refreshProfile()
            } catch (e: Exception) {
                com.booknook.app.util.Logger.e("ProfileVM", "Failed to refresh local profile", e)
                _event.value = Event(ProfileEvent.ShowMessage(e.toUserFriendlyMessageRes(R.string.profile_update_failed)))
            }
        }

        viewModelScope.launch {
            authRepository.authState
                .collect { userId ->
                    if (userId == currentUserId) return@collect
                    currentUserId = userId
                    resetForAuthChange()
                    if (userId != null) {
                        runCatching { profileRepository.refreshProfile() }
                    }
                }
        }
    }

    fun onUsernameChanged(username: String) {
        draftUsername = username
        publishState()
    }

    fun onAvatarSelected(uri: Uri?) {
        selectedAvatarUri = uri
        publishState()
    }

    fun onSaveRequested() {
        val user = currentUser ?: return
        val username = draftUsername.trim()
        if (username.isEmpty()) {
            _event.value = Event(ProfileEvent.ShowMessage(R.string.profile_required_error))
            return
        }
        if (_uiState.value?.isSaving == true) return

        _uiState.value = _uiState.value?.copy(isSaving = true)
        viewModelScope.launch {
            try {
                profileRepository.updateProfile(username, user.email, selectedAvatarUri)
                selectedAvatarUri = null
                draftUsername = username
                _event.value = Event(ProfileEvent.ShowMessage(R.string.profile_updated))
            } catch (e: Exception) {
                _event.value = Event(ProfileEvent.ShowMessage(e.toUserFriendlyMessageRes(R.string.profile_update_failed)))
            } finally {
                _uiState.value = _uiState.value?.copy(isSaving = false)
                publishState()
            }
        }
    }

    fun onLogoutRequested() {
        if (_uiState.value?.isLoggingOut == true) return

        _uiState.value = _uiState.value?.copy(isLoggingOut = true)
        viewModelScope.launch {
            try {
                authRepository.logout()
                _event.value = Event(ProfileEvent.NavigateToLogin)
            } catch (e: Exception) {
                _event.value = Event(ProfileEvent.ShowMessage(e.toUserFriendlyMessageRes(R.string.error_generic)))
            } finally {
                _uiState.value = _uiState.value?.copy(isLoggingOut = false)
            }
        }
    }

    private fun publishState() {
        val user = currentUser
        val username = when {
            draftUsername.isNotBlank() -> draftUsername
            user != null -> user.username
            else -> ""
        }
        val hasChanges = user != null &&
            ((username.trim() != user.username.trim()) || selectedAvatarUri != null)

        _uiState.value = ProfileUiState(
            user = user,
            draftUsername = username,
            selectedAvatarUri = selectedAvatarUri,
            isSaving = _uiState.value?.isSaving == true,
            isLoggingOut = _uiState.value?.isLoggingOut == true,
            canSave = hasChanges
        )
    }

    private fun resetForAuthChange() {
        currentUser = null
        draftUsername = ""
        selectedAvatarUri = null
        publishState()
    }

    companion object {
        fun factory(
            profileRepository: ProfileRepository,
            authRepository: AuthRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                        return ProfileViewModel(profileRepository, authRepository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}

data class ProfileUiState(
    val user: UserEntity? = null,
    val draftUsername: String = "",
    val selectedAvatarUri: Uri? = null,
    val isSaving: Boolean = false,
    val isLoggingOut: Boolean = false,
    val canSave: Boolean = false
)

sealed interface ProfileEvent {
    data object NavigateToLogin : ProfileEvent
    data class ShowMessage(@StringRes val messageRes: Int) : ProfileEvent
}
