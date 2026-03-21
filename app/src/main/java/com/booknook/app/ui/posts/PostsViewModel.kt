package com.booknook.app.ui.posts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.booknook.app.R
import com.booknook.app.data.local.entities.PostEntity
import com.booknook.app.data.local.entities.UserEntity
import com.booknook.app.model.Model
import com.booknook.app.util.toUserFriendlyMessageRes
import kotlinx.coroutines.launch

class PostsViewModel : ViewModel() {

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<Int?>()
    val error: LiveData<Int?> = _error

    val user: LiveData<UserEntity?> = Model.profileRepository.observeLocalUser()

    private val allPosts: LiveData<List<PostEntity>> =
        Model.postsRepository.observePosts(Model.authRepository.currentUserId().orEmpty())
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
                Model.postsRepository.refreshPosts(force = true)
            } catch (e: Exception) {
                _error.value = e.toUserFriendlyMessageRes(R.string.error_posts_refresh)
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

        val source = Model.postsRepository.searchPostsByQuery(query, Model.authRepository.currentUserId().orEmpty())
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
                Model.postsRepository.toggleLike(postId)
            } catch (e: Exception) {
                com.booknook.app.util.Logger.e("PostsVM", "Like toggle failed for $postId", e)
                _error.value = e.toUserFriendlyMessageRes(R.string.error_like_update)
            } finally {
                _isLikeProcessing.value = false
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
}
