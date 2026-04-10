package com.booknook.app.ui.posts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.booknook.app.R
import com.booknook.app.data.local.entities.PostEntity
import com.booknook.app.data.local.entities.UserEntity
import com.booknook.app.model.Model
import com.booknook.app.util.toUserFriendlyMessageRes
class PostsViewModel : ViewModel() {

    val currentUserId: String? = Model.authRepository.currentUserId()

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<Int?>()
    val error: LiveData<Int?> = _error

    private val _loadingMore = MutableLiveData(false)
    val loadingMore: LiveData<Boolean> = _loadingMore

    val user: LiveData<UserEntity?> = Model.profileRepository.observeLocalUser()

    private val _posts = MediatorLiveData<List<PostEntity>>()
    val posts: LiveData<List<PostEntity>> = _posts

    private val allPosts: LiveData<List<PostEntity>> =
        Model.postsRepository.observePosts(Model.authRepository.currentUserId().orEmpty())
    private var currentSearchSource: LiveData<List<PostEntity>>? = null

    init {
        _posts.addSource(allPosts) { _posts.value = it }
        refreshPosts(isManual = false)
    }

    fun refreshPosts(isManual: Boolean = true) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                Model.postsRepository.resetPagination()
                Model.postsRepository.refreshPosts(force = true)
            } catch (e: Exception) {
                if (isManual || posts.value.isNullOrEmpty()) {
                    _error.value = e.toUserFriendlyMessageRes(R.string.error_posts_refresh)
                }
            } finally {
                _loading.value = false
            }
        }
    }

    fun loadMore() {
        if (_loading.value == true || _loadingMore.value == true) return
        viewModelScope.launch {
            try {
                _loadingMore.value = true
                Model.postsRepository.loadMorePosts()
            } catch (e: Exception) {
                _error.value = e.toUserFriendlyMessageRes(R.string.error_posts_refresh)
            } finally {
                _loadingMore.value = false
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
        
        viewModelScope.launch {
            try {
                Model.postsRepository.toggleLike(postId)
            } catch (e: Exception) {
                _error.value = e.toUserFriendlyMessageRes(R.string.error_like_update)
            } finally {
                _isLikeProcessing.value = false
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
                _error.value = e.toUserFriendlyMessageRes(R.string.error_post_delete)
            }
        }
    }

    fun resetDeleteSuccess() {
        _deleteSuccess.value = false
    }
}
