package com.booknook.app.ui.posts

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.booknook.app.R
import com.booknook.app.data.local.entities.PostEntity
import com.booknook.app.data.repository.AuthRepository
import com.booknook.app.data.repository.PostsRepository
import com.booknook.app.model.Book
import com.booknook.app.util.Event
import com.booknook.app.util.toUserFriendlyMessageRes
import kotlinx.coroutines.launch

class PostEditorViewModel(
    private val postsRepository: PostsRepository,
    authRepository: AuthRepository
) : ViewModel() {

    private val currentUserId = authRepository.currentUserId().orEmpty()

    private val _uiState = MediatorLiveData(PostEditorUiState())
    val uiState: LiveData<PostEditorUiState> = _uiState

    private val _event = MutableLiveData<Event<PostEditorEvent>>()
    val event: LiveData<Event<PostEditorEvent>> = _event

    private var editingPostId: String? = null
    private var postSource: LiveData<PostEntity?>? = null

    init {
        _uiState.value = PostEditorUiState()
    }

    fun loadPost(postId: String) {
        if (editingPostId == postId) return
        editingPostId = postId
        _uiState.value = _uiState.value?.copy(isLoadingPost = true)

        postSource?.let { _uiState.removeSource(it) }
        postSource = postsRepository.observePost(postId, currentUserId).also { source ->
            _uiState.addSource(source) { post ->
                _uiState.value = _uiState.value?.copy(
                    post = post,
                    isLoadingPost = false
                )
            }
        }
    }

    fun createPost(book: Book, rating: Int, review: String, imageUri: Uri?) {
        if (_uiState.value?.isSaving == true) return

        _uiState.value = _uiState.value?.copy(isSaving = true)
        viewModelScope.launch {
            try {
                postsRepository.createPost(book, rating, review, imageUri)
                _event.value = Event(PostEditorEvent.Finish(R.string.create_post_success))
            } catch (e: Exception) {
                _event.value = Event(PostEditorEvent.ShowMessage(e.toUserFriendlyMessageRes(R.string.create_post_save_failed)))
            } finally {
                _uiState.value = _uiState.value?.copy(isSaving = false)
            }
        }
    }

    fun updatePost(postId: String, rating: Int, review: String, imageUri: Uri?) {
        if (_uiState.value?.isSaving == true) return

        _uiState.value = _uiState.value?.copy(isSaving = true)
        viewModelScope.launch {
            try {
                postsRepository.updatePost(postId, rating, review, imageUri)
                _event.value = Event(PostEditorEvent.Finish(R.string.create_post_update_success))
            } catch (e: Exception) {
                _event.value = Event(PostEditorEvent.ShowMessage(e.toUserFriendlyMessageRes(R.string.create_post_update_failed)))
            } finally {
                _uiState.value = _uiState.value?.copy(isSaving = false)
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
                    if (modelClass.isAssignableFrom(PostEditorViewModel::class.java)) {
                        return PostEditorViewModel(postsRepository, authRepository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}

data class PostEditorUiState(
    val post: PostEntity? = null,
    val isLoadingPost: Boolean = false,
    val isSaving: Boolean = false
)

sealed interface PostEditorEvent {
    data class Finish(@StringRes val messageRes: Int) : PostEditorEvent
    data class ShowMessage(@StringRes val messageRes: Int) : PostEditorEvent
}
