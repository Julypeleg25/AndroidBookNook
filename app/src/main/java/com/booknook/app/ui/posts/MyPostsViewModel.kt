package com.booknook.app.ui.posts

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.booknook.app.data.repository.AuthRepository
import com.booknook.app.data.repository.PostsRepository
import com.booknook.app.data.local.entities.PostEntity
import com.booknook.app.R
import com.booknook.app.util.Event
import com.booknook.app.util.toUserFriendlyMessageRes
import kotlinx.coroutines.launch

class MyPostsViewModel(
    private val postsRepository: PostsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val currentUserId: String? = authRepository.currentUserId()

    private val _uiState = MediatorLiveData(MyPostsUiState(currentUserId = currentUserId))
    val uiState: LiveData<MyPostsUiState> = _uiState

    private val _event = MutableLiveData<Event<MyPostsEvent>>()
    val event: LiveData<Event<MyPostsEvent>> = _event

    private var isLikeProcessing = false

    init {
        val userId = currentUserId
        if (userId != null) {
            _uiState.addSource(postsRepository.observeMyPosts(userId, userId)) { posts ->
                val currentState = _uiState.value ?: return@addSource
                _uiState.value = currentState.copy(
                    posts = posts,
                    isEmpty = posts.isEmpty() && !currentState.isInitialLoading && !currentState.isRefreshing
                )
            }
            refreshPosts(isUserInitiated = false)
        } else {
            _uiState.value = MyPostsUiState()
        }
    }

    fun refreshPosts(isUserInitiated: Boolean = true) {
        val userId = currentUserId ?: return
        val state = _uiState.value ?: return
        if (state.isInitialLoading || state.isRefreshing) return

        val showFullscreenLoader = state.posts.isEmpty()
        _uiState.value = _uiState.value?.copy(
            isInitialLoading = showFullscreenLoader,
            isRefreshing = !showFullscreenLoader
        )
        viewModelScope.launch {
            try {
                postsRepository.refreshMyPosts(userId)
            } catch (e: Exception) {
                if (isUserInitiated || showFullscreenLoader) {
                    _event.value = Event(MyPostsEvent.ShowMessage(e.toUserFriendlyMessageRes(R.string.error_posts_refresh)))
                }
            } finally {
                val currentState = _uiState.value ?: return@launch
                _uiState.value = currentState.copy(
                    isInitialLoading = false,
                    isRefreshing = false,
                    isEmpty = currentState.posts.isEmpty()
                )
            }
        }
    }

    fun onPostSelected(postId: String) {
        _event.value = Event(MyPostsEvent.NavigateToDetails(postId))
    }

    fun onEditRequested(postId: String) {
        _event.value = Event(MyPostsEvent.NavigateToEdit(postId))
    }

    fun onDeleteConfirmed(postId: String) {
        viewModelScope.launch {
            try {
                postsRepository.deletePost(postId)
                _event.value = Event(MyPostsEvent.ShowMessage(R.string.post_deleted))
            } catch (e: Exception) {
                _event.value = Event(MyPostsEvent.ShowMessage(e.toUserFriendlyMessageRes(R.string.error_post_delete)))
            }
        }
    }

    fun onLikeClicked(postId: String) {
        if (isLikeProcessing) return
        isLikeProcessing = true
        
        viewModelScope.launch {
            try {
                postsRepository.toggleLike(postId)
            } catch (e: Exception) {
                _event.value = Event(MyPostsEvent.ShowMessage(e.toUserFriendlyMessageRes(R.string.error_like_update)))
            } finally {
                isLikeProcessing = false
            }
        }
    }

    companion object {
        fun factory(
            postsRepository: PostsRepository,
            authRepository: AuthRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(MyPostsViewModel::class.java)) {
                        return MyPostsViewModel(postsRepository, authRepository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}

data class MyPostsUiState(
    val currentUserId: String? = null,
    val posts: List<PostEntity> = emptyList(),
    val isInitialLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isEmpty: Boolean = false
)

sealed interface MyPostsEvent {
    data class NavigateToDetails(val postId: String) : MyPostsEvent
    data class NavigateToEdit(val postId: String) : MyPostsEvent
    data class ShowMessage(@StringRes val messageRes: Int) : MyPostsEvent
}
