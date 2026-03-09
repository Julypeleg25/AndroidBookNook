package com.booknook.app.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.booknook.app.model.Model
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _registrationSuccess = MutableLiveData<Boolean>()
    val registrationSuccess: LiveData<Boolean> = _registrationSuccess

    private val _loginSuccess = MutableLiveData<Boolean>()
    val loginSuccess: LiveData<Boolean> = _loginSuccess

    fun login(email: String, password: String) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                Model.firebase.login(email, password)
                Model.ensureLocalProfile()
                _loginSuccess.value = true
            } catch (e: Exception) {
                _error.value = e.message ?: "Login failed"
            } finally {
                _loading.value = false
            }
        }
    }

    fun register(email: String, password: String, username: String) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                Model.firebase.register(email, password, username)
                Model.ensureLocalProfile()
                _registrationSuccess.value = true
            } catch (e: Exception) {
                _error.value = e.message ?: "Registration failed"
            } finally {
                _loading.value = false
            }
        }
    }

    fun resetError() {
        _error.value = null
    }
}
