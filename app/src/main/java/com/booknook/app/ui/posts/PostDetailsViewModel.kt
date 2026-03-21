package com.booknook.app.ui.posts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.booknook.app.R
import com.booknook.app.data.local.entities.CommentEntity
import com.booknook.app.data.local.entities.PostEntity
import com.booknook.app.domain.Book
import com.booknook.app.model.Model
import com.booknook.app.util.toUserFriendlyMessageRes
import kotlinx.coroutines.launch

class PostDetailsViewModel : ViewModel() {

    private val _isActionProcessing = MutableLiveData<Boolean>(false)
    val isActionProcessing: LiveData<Boolean> = _isActionProcessing

    private val _isCommentProcessing = MutableLiveData<Boolean>(false)
    val isCommentProcessing: LiveData<Boolean> = _isCommentProcessing

    private val _error = MutableLiveData<Int?>()
    val error: LiveData<Int?> = _error

    private val _actionFeedback = MutableLiveData<Int?>()
    val actionFeedback: LiveData<Int?> = _actionFeedback

    private val _bookInfo = MutableLiveData<BookInfoCardModel>()
    val bookInfo: LiveData<BookInfoCardModel> = _bookInfo

    private var resolvedBook: Book? = null
    private var resolvedBookId: String? = null

    fun observePost(postId: String): LiveData<PostEntity?> {
        return Model.postsRepository.observePost(postId, Model.authRepository.currentUserId().orEmpty())
    }

    fun observeComments(postId: String): LiveData<List<CommentEntity>> =
        Model.postsRepository.observeComments(postId)

    fun refreshComments(postId: String) {
        viewModelScope.launch {
            try {
                Model.postsRepository.refreshComments(postId)
            } catch (e: Exception) {
                com.booknook.app.util.Logger.e("PostDetailsVM", "Failed to refresh comments for $postId", e)
                _error.value = e.toUserFriendlyMessageRes(R.string.error_comment_refresh)
            }
        }
    }

    fun resolveBookInfo(post: PostEntity) {
        if (resolvedBookId == post.bookId && _bookInfo.value != null) return

        val fallbackBook = post.toBook()
        _bookInfo.value = fallbackBook.toCardModel()

        viewModelScope.launch {
            val freshBook = try {
                Model.booksRepository.getBook(post.bookId)
            } catch (e: Exception) {
                com.booknook.app.util.Logger.e("PostDetailsVM", "Failed to resolve book info for ${post.bookId}", e)
                _error.value = e.toUserFriendlyMessageRes(R.string.error_book_info_load)
                null
            } ?: fallbackBook

            resolvedBook = freshBook
            resolvedBookId = post.bookId
            _bookInfo.value = freshBook.toCardModel()
        }
    }

    fun observeWishlist(bookId: String): LiveData<Boolean> {
        val uid = Model.authRepository.currentUserId() ?: return MutableLiveData(false)
        return Model.listsRepository.observeWishlistExists(uid, bookId)
    }

    fun observeReadlist(bookId: String): LiveData<Boolean> {
        val uid = Model.authRepository.currentUserId() ?: return MutableLiveData(false)
        return Model.listsRepository.observeReadlistExists(uid, bookId)
    }

    fun toggleLike(postId: String) {
        if (_isActionProcessing.value == true) return
        _isActionProcessing.value = true
        
        viewModelScope.launch {
            try {
                Model.postsRepository.toggleLike(postId)
            } catch (e: Exception) {
                _error.value = e.toUserFriendlyMessageRes(R.string.error_like_update)
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
                val post = Model.postsRepository.getPost(postId, Model.authRepository.currentUserId().orEmpty()) ?: return@launch
                val book = resolvedBook?.takeIf { it.id == post.bookId } ?: post.toBook()
                val userId = Model.authRepository.currentUserId() ?: return@launch
                val added = Model.listsRepository.toggleWishlist(userId, book)
                _actionFeedback.value = if (added) R.string.wishlist_added else R.string.wishlist_removed
            } catch (e: Exception) {
                _error.value = e.toUserFriendlyMessageRes(R.string.error_post_action)
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
                val post = Model.postsRepository.getPost(postId, Model.authRepository.currentUserId().orEmpty()) ?: return@launch
                val book = resolvedBook?.takeIf { it.id == post.bookId } ?: post.toBook()
                val userId = Model.authRepository.currentUserId() ?: return@launch
                val added = Model.listsRepository.toggleReadlist(userId, book)
                _actionFeedback.value = if (added) R.string.readlist_added else R.string.readlist_removed
            } catch (e: Exception) {
                _error.value = e.toUserFriendlyMessageRes(R.string.error_post_action)
            } finally {
                _isActionProcessing.value = false
            }
        }
    }

    fun addComment(postId: String, text: String) {
        if (text.isBlank()) {
            _error.value = R.string.error_comment_required
            return
        }
        _isCommentProcessing.value = true
        viewModelScope.launch {
            try {
                Model.postsRepository.addComment(postId, text)
                _actionFeedback.value = R.string.comment_added
            } catch (e: Exception) {
                _error.value = e.toUserFriendlyMessageRes(R.string.error_comment_add)
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
