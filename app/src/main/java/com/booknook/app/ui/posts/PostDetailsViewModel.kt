package com.booknook.app.ui.posts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.booknook.app.data.local.entities.PostEntity
import com.booknook.app.domain.Book
import com.booknook.app.model.Model
import kotlinx.coroutines.launch

class PostDetailsViewModel : ViewModel() {

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _actionFeedback = MutableLiveData<String?>()
    val actionFeedback: LiveData<String?> = _actionFeedback

    fun observePost(postId: String): LiveData<PostEntity?> = Model.observePost(postId)

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            try {
                Model.toggleLike(postId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Like failed"
            }
        }
    }

    fun addComment(postId: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            try {
                Model.addComment(postId, text)
                _actionFeedback.value = "Comment added"
            } catch (e: Exception) {
                _error.value = e.message ?: "Comment failed"
            }
        }
    }

    fun addToWishlist(postId: String) {
        viewModelScope.launch {
            try {
                val userId = Model.currentUserId() ?: return@launch
                val post = Model.getPost(postId) ?: return@launch
                Model.addToWishlist(userId, Book(post.bookId, post.bookTitle, post.bookAuthor, post.bookThumbnail))
                _actionFeedback.value = "Added to wishlist"
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to add to wishlist"
            }
        }
    }

    fun addToReadlist(postId: String) {
        viewModelScope.launch {
            try {
                val userId = Model.currentUserId() ?: return@launch
                val post = Model.getPost(postId) ?: return@launch
                Model.addToReadlist(userId, Book(post.bookId, post.bookTitle, post.bookAuthor, post.bookThumbnail))
                _actionFeedback.value = "Added to readlist"
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to add to readlist"
            }
        }
    }

    fun resetFeedback() {
        _actionFeedback.value = null
    }
}
