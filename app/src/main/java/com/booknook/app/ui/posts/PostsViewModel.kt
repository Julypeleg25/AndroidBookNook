package com.booknook.app.ui.posts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
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

    private val allPosts: LiveData<List<PostEntity>> = Model.observePosts()
    private var currentSearchSource: LiveData<List<PostEntity>>? = null

    private val _posts = MediatorLiveData<List<PostEntity>>()
    val posts: LiveData<List<PostEntity>> = _posts

    init {
        _posts.addSource(allPosts) { _posts.value = it }
        refreshPosts()
    }

    fun refreshPosts() {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                Model.refreshPosts(force = true)
            } catch (e: Exception) {
                _error.value = mapErrorMessage(e)
            } finally {
                _loading.value = false
            }
        }
    }

    fun filterPosts(query: String) {
        currentSearchSource?.let { _posts.removeSource(it) }

        if (query.isBlank()) {
            currentSearchSource = null
            _posts.removeSource(allPosts)
            _posts.addSource(allPosts) { _posts.value = it }
            return
        }

        val source = Model.searchPostsByQuery(query)
        currentSearchSource = source
        _posts.removeSource(allPosts)
        _posts.addSource(source) { _posts.value = it }
    }

    private val _isLikeProcessing = MutableLiveData<Boolean>(false)

    fun toggleLike(postId: String) {
        if (_isLikeProcessing.value == true) return
        _isLikeProcessing.value = true
        com.booknook.app.util.Logger.d("PostsVM", "User toggled like for $postId")
        
        viewModelScope.launch {
            try {
                Model.toggleLike(postId)
            } catch (e: Exception) {
                com.booknook.app.util.Logger.e("PostsVM", "Like toggle failed for $postId", e)
                _error.value = "Failed to update like"
            } finally {
                _isLikeProcessing.value = false
            }
        }
    }

    private fun mapErrorMessage(e: Exception): String {
        val msg = e.message ?: return "Failed to refresh posts"
        return when {
            msg.contains("network", ignoreCase = true) || msg.contains("offline", ignoreCase = true) ->
                "Device is offline. Showing cached posts."
            msg.contains("Timed out", ignoreCase = true) -> "Connection timed out."
            else -> msg
        }
    }
}
