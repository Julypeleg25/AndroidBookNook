package com.booknook.app.ui.posts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.booknook.app.R
import com.booknook.app.data.local.entities.PostEntity
import com.booknook.app.model.Model
import com.booknook.app.util.toUserFriendlyMessageRes
import kotlinx.coroutines.launch

class MyPostsViewModel : ViewModel() {

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
                _error.value = e.toUserFriendlyMessageRes(R.string.error_posts_refresh)
            } finally {
                _loading.value = false
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            try {
                Model.postsRepository.deletePost(postId)
            } catch (e: Exception) {
                _error.value = e.toUserFriendlyMessageRes(R.string.error_post_delete)
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
                Model.postsRepository.toggleLike(postId)
            } catch (e: Exception) {
                com.booknook.app.util.Logger.e("MyPostsVM", "Like toggle failed for $postId", e)
                _error.value = e.toUserFriendlyMessageRes(R.string.error_like_update)
            } finally {
                _isLikeProcessing.value = false
            }
        }
    }
}
