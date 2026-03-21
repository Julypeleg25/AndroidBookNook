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
                Model.refreshPosts(force = true)
            } catch (e: Exception) {
                _error.value = mapErrorMessage(e, "Failed to refresh posts")
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
                _error.value = mapErrorMessage(e, "Failed to delete post")
            }
        }
    }

    private val _isLikeProcessing = MutableLiveData<Boolean>(false)

    fun toggleLike(postId: String) {
        if (_isLikeProcessing.value == true) return
        _isLikeProcessing.value = true
        com.booknook.app.util.Logger.d("MyPostsVM", "User toggled like for $postId")
        
        viewModelScope.launch {
            try {
                Model.toggleLike(postId)
            } catch (e: Exception) {
                com.booknook.app.util.Logger.e("MyPostsVM", "Like toggle failed for $postId", e)
                _error.value = "Failed to update like"
            } finally {
                _isLikeProcessing.value = false
            }
        }
    }


    private fun mapErrorMessage(e: Exception, default: String): String {
        val msg = e.message ?: return default
        return when {
            msg.contains("network", ignoreCase = true) || msg.contains("offline", ignoreCase = true) -> 
                "Device is offline. Please check your connection."
            msg.contains("Timed out", ignoreCase = true) -> "Connection timed out."
            else -> msg
        }
    }
}
