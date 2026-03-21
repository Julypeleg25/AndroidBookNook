package com.booknook.app.ui.posts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.booknook.app.data.local.entities.PostEntity
import com.booknook.app.data.local.entities.UserEntity
import com.booknook.app.model.Model
import kotlinx.coroutines.launch

class PostsViewModel : ViewModel() {

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    val user: LiveData<UserEntity?> = Model.observeLocalUser()
    val posts: LiveData<List<PostEntity>> = Model.observePosts()

    init {
        refreshPosts()
    }

    fun refreshPosts() {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                Model.refreshPosts()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to refresh posts"
            } finally {
                _loading.value = false
            }
        }
    }
}
