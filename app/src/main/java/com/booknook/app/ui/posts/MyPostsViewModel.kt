package com.booknook.app.ui.posts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.booknook.app.data.local.entities.PostEntity
import com.booknook.app.model.Model
import kotlinx.coroutines.launch

class MyPostsViewModel : ViewModel() {

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun observeMyPosts(): LiveData<List<PostEntity>> {
        val userId = Model.currentUserId() ?: return MutableLiveData(emptyList())
        return Model.observeMyPosts(userId)
    }

    fun refreshPosts() {
        _loading.value = true
        viewModelScope.launch {
            try {
                Model.refreshPosts()
            } catch (e: Exception) {
                _error.value = "Failed to refresh posts"
            } finally {
                _loading.value = false
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            try {
                Model.deletePost(postId)
            } catch (e: Exception) {
                _error.value = "Failed to delete post"
            }
        }
    }
}
