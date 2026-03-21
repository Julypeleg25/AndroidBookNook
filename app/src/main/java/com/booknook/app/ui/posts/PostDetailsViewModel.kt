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

    private val _isProcessing = MutableLiveData<Boolean>(false)
    val isProcessing: LiveData<Boolean> = _isProcessing

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _actionFeedback = MutableLiveData<String?>()
    val actionFeedback: LiveData<String?> = _actionFeedback

    fun observePost(postId: String): LiveData<PostEntity?> = Model.observePost(postId)

    fun observeWishlist(bookId: String): LiveData<Boolean> {
        val uid = Model.currentUserId() ?: return MutableLiveData(false)
        return Model.observeWishlistExists(uid, bookId)
    }

    fun observeReadlist(bookId: String): LiveData<Boolean> {
        val uid = Model.currentUserId() ?: return MutableLiveData(false)
        return Model.observeReadlistExists(uid, bookId)
    }

    fun toggleLike(postId: String) {
        if (_isProcessing.value == true) return
        _isProcessing.value = true
        
        viewModelScope.launch {
            try {
                val post = Model.getPost(postId)
                if (post != null && post.userId == Model.currentUserId()) {
                    com.booknook.app.util.Logger.d("PostDetails", "Self-like blocked for $postId")
                    _actionFeedback.value = "You can't like your own post"
                    return@launch
                }
                
                com.booknook.app.util.Logger.d("PostDetails", "Toggling like for $postId")
                Model.toggleLike(postId)
            } catch (e: Exception) {
                com.booknook.app.util.Logger.e("PostDetails", "Like failed", e)
                _error.value = e.message ?: "Like failed"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun addComment(postId: String, text: String) {
        if (text.isBlank() || _isProcessing.value == true) return
        _isProcessing.value = true
        
        viewModelScope.launch {
            try {
                com.booknook.app.util.Logger.d("PostDetails", "Adding comment to $postId")
                Model.addComment(postId, text)
                _actionFeedback.value = "Comment added"
            } catch (e: Exception) {
                com.booknook.app.util.Logger.e("PostDetails", "Comment failed", e)
                _error.value = e.message ?: "Comment failed"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun toggleWishlist(postId: String) {
        if (_isProcessing.value == true) return
        _isProcessing.value = true
        
        viewModelScope.launch {
            try {
                val post = Model.getPost(postId) ?: return@launch
                val book = Book(post.bookId, post.bookTitle, post.bookAuthor, post.bookThumbnail)
                val userId = Model.currentUserId() ?: return@launch
                
                com.booknook.app.util.Logger.d("PostDetails", "Toggling wishlist for book ${book.title}")
                val added = Model.toggleWishlist(userId, book)
                _actionFeedback.value = if (added) "Added to wishlist" else "Removed from wishlist"
            } catch (e: Exception) {
                com.booknook.app.util.Logger.e("PostDetails", "Wishlist toggle failed", e)
                _error.value = e.message ?: "Action failed"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun toggleReadlist(postId: String) {
        if (_isProcessing.value == true) return
        _isProcessing.value = true
        
        viewModelScope.launch {
            try {
                val post = Model.getPost(postId) ?: return@launch
                val book = Book(post.bookId, post.bookTitle, post.bookAuthor, post.bookThumbnail)
                val userId = Model.currentUserId() ?: return@launch
                
                com.booknook.app.util.Logger.d("PostDetails", "Toggling readlist for book ${book.title}")
                val added = Model.toggleReadlist(userId, book)
                _actionFeedback.value = if (added) "Added to readlist" else "Removed from readlist"
            } catch (e: Exception) {
                com.booknook.app.util.Logger.e("PostDetails", "Readlist toggle failed", e)
                _error.value = e.message ?: "Action failed"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun resetFeedback() {
        _actionFeedback.value = null
    }
}
