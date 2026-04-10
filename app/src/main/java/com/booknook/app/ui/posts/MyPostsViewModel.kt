package com.booknook.app.ui.posts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.booknook.app.data.local.entities.PostEntity
import com.booknook.app.model.Model
import com.booknook.app.util.toUserFriendlyMessageRes
import com.booknook.app.R
import kotlinx.coroutines.launch

class MyPostsViewModel : ViewModel() {

    val currentUserId: String? = Model.authRepository.currentUserId()

    init {
        refreshPosts()
    }

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<Int?>()
    val error: LiveData<Int?> = _error

    fun observeMyPosts(): LiveData<List<PostEntity>> {
        val userId = Model.authRepository.currentUserId() ?: return MutableLiveData(emptyList())
        return Model.postsRepository.observeMyPosts(userId, userId)
    }

    fun refreshPosts() {
        _loading.value = true
        viewModelScope.launch {
            try {
                Model.postsRepository.refreshPosts(force = true)
            } catch (e: Exception) {
            } finally {
                _loading.value = false
            }
        }
    }

    private val _deleteSuccess = MutableLiveData<Boolean>()
    val deleteSuccess: LiveData<Boolean> = _deleteSuccess

    fun deletePost(postId: String) {
        viewModelScope.launch {
            try {
                Model.postsRepository.deletePost(postId)
                _deleteSuccess.value = true
            } catch (e: Exception) {
                _error.value = R.string.error_post_delete
            }
        }
    }

    fun resetDeleteSuccess() {
        _deleteSuccess.value = false
    }

    private val _isLikeProcessing = MutableLiveData<Boolean>(false)

    fun toggleLike(postId: String) {
        if (_isLikeProcessing.value == true) return
        _isLikeProcessing.value = true
        
        viewModelScope.launch {
            try {
                Model.postsRepository.toggleLike(postId)
            } catch (e: Exception) {
            } finally {
                _isLikeProcessing.value = false
            }
        }
    }
}
