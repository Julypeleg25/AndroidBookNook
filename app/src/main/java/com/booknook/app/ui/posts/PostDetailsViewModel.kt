package com.booknook.app.ui.posts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.booknook.app.data.local.entities.CommentEntity
import com.booknook.app.data.local.entities.PostEntity
import com.booknook.app.domain.Book
import com.booknook.app.model.Model
import kotlinx.coroutines.launch

class PostDetailsViewModel : ViewModel() {

    private val _isActionProcessing = MutableLiveData<Boolean>(false)
    val isActionProcessing: LiveData<Boolean> = _isActionProcessing

    private val _isCommentProcessing = MutableLiveData<Boolean>(false)
    val isCommentProcessing: LiveData<Boolean> = _isCommentProcessing

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _actionFeedback = MutableLiveData<String?>()
    val actionFeedback: LiveData<String?> = _actionFeedback

    private val _bookInfo = MutableLiveData<BookInfoCardModel>()
    val bookInfo: LiveData<BookInfoCardModel> = _bookInfo

    private var resolvedBook: Book? = null
    private var resolvedBookId: String? = null

    fun observePost(postId: String): LiveData<PostEntity?> = Model.observePost(postId)

    fun observeComments(postId: String): LiveData<List<CommentEntity>> =
        Model.observeComments(postId)

    fun refreshComments(postId: String) {
        viewModelScope.launch {
            try { Model.refreshComments(postId) } catch (_: Exception) {}
        }
    }

    fun resolveBookInfo(post: PostEntity) {
        if (resolvedBookId == post.bookId && _bookInfo.value != null) return

        val fallbackBook = post.toBook()
        _bookInfo.value = fallbackBook.toCardModel()

        viewModelScope.launch {
            val freshBook = try {
                Model.getBook(post.bookId)
            } catch (_: Exception) {
                null
            } ?: fallbackBook

            resolvedBook = freshBook
            resolvedBookId = post.bookId
            _bookInfo.value = freshBook.toCardModel()
        }
    }

    fun observeWishlist(bookId: String): LiveData<Boolean> {
        val uid = Model.currentUserId() ?: return MutableLiveData(false)
        return Model.observeWishlistExists(uid, bookId)
    }

    fun observeReadlist(bookId: String): LiveData<Boolean> {
        val uid = Model.currentUserId() ?: return MutableLiveData(false)
        return Model.observeReadlistExists(uid, bookId)
    }

    fun toggleLike(postId: String) {
        if (_isActionProcessing.value == true) return
        _isActionProcessing.value = true
        
        viewModelScope.launch {
            try {
                Model.toggleLike(postId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Like failed"
            } finally {
                _isActionProcessing.value = false
            }
        }
    }

    fun toggleWishlist(postId: String) {
        if (_isActionProcessing.value == true) return
        _isActionProcessing.value = true
        
        viewModelScope.launch {
            try {
                val post = Model.getPost(postId) ?: return@launch
                val book = resolvedBook?.takeIf { it.id == post.bookId } ?: post.toBook()
                val userId = Model.currentUserId() ?: return@launch
                val added = Model.toggleWishlist(userId, book)
                _actionFeedback.value = if (added) "Added to wishlist" else "Removed from wishlist"
            } catch (e: Exception) {
                _error.value = e.message ?: "Action failed"
            } finally {
                _isActionProcessing.value = false
            }
        }
    }

    fun toggleReadlist(postId: String) {
        if (_isActionProcessing.value == true) return
        _isActionProcessing.value = true
        
        viewModelScope.launch {
            try {
                val post = Model.getPost(postId) ?: return@launch
                val book = resolvedBook?.takeIf { it.id == post.bookId } ?: post.toBook()
                val userId = Model.currentUserId() ?: return@launch
                val added = Model.toggleReadlist(userId, book)
                _actionFeedback.value = if (added) "Added to readlist" else "Removed from readlist"
            } catch (e: Exception) {
                _error.value = e.message ?: "Action failed"
            } finally {
                _isActionProcessing.value = false
            }
        }
    }

    fun addComment(postId: String, text: String) {
        if (text.isBlank()) {
            _error.value = "Comment cannot be empty"
            return
        }
        _isCommentProcessing.value = true
        viewModelScope.launch {
            try {
                Model.addComment(postId, text)
                _actionFeedback.value = "Comment added"
            } catch (e: Exception) {
                _error.value = e.message ?: "Comment failed"
            } finally {
                _isCommentProcessing.value = false
            }
        }
    }

    fun resetFeedback() {
        _actionFeedback.value = null
    }

    private fun PostEntity.toBook(): Book {
        return Book(
            id = bookId,
            title = bookTitle,
            author = bookAuthor,
            thumbnail = bookThumbnail,
            publishedDate = bookPublishedDate,
            genre = bookGenre,
            pageCount = bookPageCount,
            description = bookDescription
        )
    }

    private fun Book.toCardModel(): BookInfoCardModel {
        return BookInfoCardModel(
            title = title,
            author = author,
            thumbnail = thumbnail,
            genre = genre,
            publishedDate = publishedDate,
            pageCount = pageCount,
            description = description
        )
    }
}
