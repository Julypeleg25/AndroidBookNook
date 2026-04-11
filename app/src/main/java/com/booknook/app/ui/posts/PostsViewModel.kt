package com.booknook.app.ui.posts

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.booknook.app.R
import com.booknook.app.data.repository.AuthRepository
import com.booknook.app.data.repository.PostsRepository
import com.booknook.app.data.local.entities.PostEntity
import com.booknook.app.util.Event
import com.booknook.app.util.toUserFriendlyMessageRes
import kotlinx.coroutines.launch

class PostsViewModel(
    private val postsRepository: PostsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val currentUserId: String? = authRepository.currentUserId()

    private val _uiState = MediatorLiveData(PostsUiState(currentUserId = currentUserId))
    val uiState: LiveData<PostsUiState> = _uiState

    private val _event = MutableLiveData<Event<PostsEvent>>()
    val event: LiveData<Event<PostsEvent>> = _event

    private val allPostsSource = postsRepository.observePosts(currentUserId.orEmpty())
    private var activePostsSource: LiveData<List<PostEntity>>? = null
    private var isLikeProcessing = false

    init {
        observePostsSource(allPostsSource)
        refreshPosts(isUserInitiated = false)
    }

    fun refreshPosts(isUserInitiated: Boolean = true) {
        val state = _uiState.value ?: return
        if (state.isInitialLoading || state.isRefreshing) return

        val showFullscreenLoader = state.posts.isEmpty()
        _uiState.value = _uiState.value?.copy(
            isInitialLoading = showFullscreenLoader,
            isRefreshing = !showFullscreenLoader
        )

        viewModelScope.launch {
            try {
                val result = postsRepository.refreshPosts(force = true)
                _uiState.value = _uiState.value?.copy(isEndReached = result.endReached)
            } catch (e: Exception) {
                if (isUserInitiated || showFullscreenLoader) {
                    _event.value = Event(PostsEvent.ShowMessage(e.toUserFriendlyMessageRes(R.string.error_posts_refresh)))
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

    fun onLoadMoreRequested() {
        val state = _uiState.value ?: return
        if (state.query.isNotBlank() || state.isInitialLoading || state.isRefreshing || state.isLoadingMore || state.isEndReached) {
            return
        }

        _uiState.value = _uiState.value?.copy(isLoadingMore = true)
        viewModelScope.launch {
            try {
                val result = postsRepository.loadMorePosts()
                _uiState.value = _uiState.value?.copy(isEndReached = result.endReached)
            } catch (e: Exception) {
                _event.value = Event(PostsEvent.ShowMessage(e.toUserFriendlyMessageRes(R.string.error_posts_refresh)))
            } finally {
                val currentState = _uiState.value ?: return@launch
                _uiState.value = currentState.copy(
                    isLoadingMore = false,
                    isEmpty = currentState.posts.isEmpty() && !currentState.isInitialLoading && !currentState.isRefreshing
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        val trimmedQuery = query.trim()
        if (_uiState.value?.query == trimmedQuery) return

        val currentState = _uiState.value ?: return
        _uiState.value = currentState.copy(
            query = trimmedQuery,
            isLoadingMore = false,
            isEndReached = if (trimmedQuery.isBlank()) currentState.isEndReached else true
        )

        if (trimmedQuery.isBlank()) {
            observePostsSource(allPostsSource)
        } else {
            observePostsSource(postsRepository.searchPostsByQuery(trimmedQuery, currentUserId.orEmpty()))
        }
    }

    fun onPostSelected(postId: String) {
        _event.value = Event(PostsEvent.NavigateToDetails(postId))
    }

    fun onEditRequested(postId: String) {
        _event.value = Event(PostsEvent.NavigateToEdit(postId))
    }

    fun onLikeClicked(postId: String) {
        if (isLikeProcessing) return
        isLikeProcessing = true
        
        viewModelScope.launch {
            try {
                postsRepository.toggleLike(postId)
            } catch (e: Exception) {
                _event.value = Event(PostsEvent.ShowMessage(e.toUserFriendlyMessageRes(R.string.error_like_update)))
            } finally {
                isLikeProcessing = false
            }
        }
    }

    fun onDeleteConfirmed(postId: String) {
        viewModelScope.launch {
            try {
                postsRepository.deletePost(postId)
                _event.value = Event(PostsEvent.ShowMessage(R.string.post_deleted))
            } catch (e: Exception) {
                _event.value = Event(PostsEvent.ShowMessage(e.toUserFriendlyMessageRes(R.string.error_post_delete)))
            }
        }
    }

    private fun observePostsSource(source: LiveData<List<PostEntity>>) {
        activePostsSource?.let { _uiState.removeSource(it) }
        activePostsSource = source
        _uiState.addSource(source) { posts ->
            val currentState = _uiState.value ?: return@addSource
            _uiState.value = currentState.copy(
                posts = posts,
                isEmpty = posts.isEmpty() && !currentState.isInitialLoading && !currentState.isRefreshing
            )
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
                    if (modelClass.isAssignableFrom(PostsViewModel::class.java)) {
                        return PostsViewModel(postsRepository, authRepository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}

data class PostsUiState(
    val currentUserId: String? = null,
    val posts: List<PostEntity> = emptyList(),
    val query: String = "",
    val isInitialLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isEmpty: Boolean = false,
    val isEndReached: Boolean = false
)

sealed interface PostsEvent {
    data class NavigateToDetails(val postId: String) : PostsEvent
    data class NavigateToEdit(val postId: String) : PostsEvent
    data class ShowMessage(@StringRes val messageRes: Int) : PostsEvent
}
