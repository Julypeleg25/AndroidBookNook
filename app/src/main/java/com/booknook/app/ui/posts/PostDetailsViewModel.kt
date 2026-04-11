package com.booknook.app.ui.posts

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.booknook.app.R
import com.booknook.app.data.local.entities.CommentEntity
import com.booknook.app.data.local.entities.PostEntity
import com.booknook.app.data.repository.AuthRepository
import com.booknook.app.data.repository.BooksRepository
import com.booknook.app.data.repository.ListsRepository
import com.booknook.app.data.repository.PostsRepository
import com.booknook.app.model.Book
import com.booknook.app.util.Event
import com.booknook.app.util.toUserFriendlyMessageRes
import kotlinx.coroutines.launch

class PostDetailsViewModel(
    private val postsRepository: PostsRepository,
    private val booksRepository: BooksRepository,
    private val listsRepository: ListsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val currentUserId: String? = authRepository.currentUserId()

    private val _uiState = MediatorLiveData(PostDetailsUiState(currentUserId = currentUserId))
    val uiState: LiveData<PostDetailsUiState> = _uiState

    private val _event = MutableLiveData<Event<PostDetailsEvent>>()
    val event: LiveData<Event<PostDetailsEvent>> = _event

    private var resolvedBook: Book? = null
    private var resolvedBookId: String? = null
    private var currentPostId: String? = null
    private var postSource: LiveData<PostEntity?>? = null
    private var commentsSource: LiveData<List<CommentEntity>>? = null
    private var wishlistSource: LiveData<Boolean>? = null
    private var readlistSource: LiveData<Boolean>? = null

    init {
        _uiState.value = PostDetailsUiState(currentUserId = currentUserId)
    }

    fun loadPost(postId: String) {
        if (currentPostId == postId) return

        currentPostId = postId
        resolvedBook = null
        resolvedBookId = null
        _uiState.value = _uiState.value?.copy(isContentLoading = true)

        postSource?.let { _uiState.removeSource(it) }
        commentsSource?.let { _uiState.removeSource(it) }

        postSource = postsRepository.observePost(postId, currentUserId.orEmpty()).also { source ->
            _uiState.addSource(source) { post ->
                val currentState = _uiState.value ?: PostDetailsUiState(currentUserId = currentUserId)
                _uiState.value = currentState.copy(
                    post = post,
                    canEdit = post?.userId == currentUserId,
                    isContentLoading = false
                )
                if (post != null) {
                    resolveBookInfo(post)
                    observeBookMembership(post.bookId)
                }
            }
        }

        commentsSource = postsRepository.observeComments(postId).also { source ->
            _uiState.addSource(source) { comments ->
                _uiState.value = _uiState.value?.copy(comments = comments)
            }
        }

        refreshComments()
    }

    fun onLikeClicked() {
        val postId = currentPostId ?: return
        if (_uiState.value?.isActionProcessing == true) return

        _uiState.value = _uiState.value?.copy(isActionProcessing = true)
        viewModelScope.launch {
            try {
                postsRepository.toggleLike(postId)
            } catch (e: Exception) {
                _event.value = Event(PostDetailsEvent.ShowMessage(e.toUserFriendlyMessageRes(R.string.error_like_update)))
            } finally {
                _uiState.value = _uiState.value?.copy(isActionProcessing = false)
            }
        }
    }

    fun onWishlistClicked() {
        toggleListMembership(
            toggle = { userId, book -> listsRepository.toggleWishlist(userId, book) },
            addedMessageRes = R.string.wishlist_added,
            removedMessageRes = R.string.wishlist_removed
        )
    }

    fun onReadlistClicked() {
        toggleListMembership(
            toggle = { userId, book -> listsRepository.toggleReadlist(userId, book) },
            addedMessageRes = R.string.readlist_added,
            removedMessageRes = R.string.readlist_removed
        )
    }

    private fun toggleListMembership(
        toggle: suspend (String, Book) -> Boolean,
        @StringRes addedMessageRes: Int,
        @StringRes removedMessageRes: Int
    ) {
        val post = _uiState.value?.post ?: return
        if (_uiState.value?.isActionProcessing == true) return

        _uiState.value = _uiState.value?.copy(isActionProcessing = true)
        viewModelScope.launch {
            try {
                val userId = currentUserId ?: return@launch
                val book = resolvedBook?.takeIf { it.id == post.bookId } ?: post.toBook()
                val added = toggle(userId, book)
                val message = if (added) addedMessageRes else removedMessageRes
                _event.value = Event(PostDetailsEvent.ShowMessage(message))
            } catch (e: Exception) {
                _event.value = Event(PostDetailsEvent.ShowMessage(e.toUserFriendlyMessageRes(R.string.error_post_action)))
            } finally {
                _uiState.value = _uiState.value?.copy(isActionProcessing = false)
            }
        }
    }

    fun onAddCommentSubmitted(text: String) {
        val postId = currentPostId ?: return
        if (text.isBlank()) {
            _event.value = Event(PostDetailsEvent.ShowMessage(R.string.error_comment_required))
            return
        }

        _uiState.value = _uiState.value?.copy(isCommentProcessing = true)
        viewModelScope.launch {
            try {
                postsRepository.addComment(postId, text)
                _event.value = Event(PostDetailsEvent.CommentAdded(R.string.comment_added))
            } catch (e: Exception) {
                _event.value = Event(PostDetailsEvent.ShowMessage(e.toUserFriendlyMessageRes(R.string.error_comment_add)))
            } finally {
                _uiState.value = _uiState.value?.copy(isCommentProcessing = false)
            }
        }
    }

    fun onEditRequested() {
        currentPostId?.let { postId ->
            _event.value = Event(PostDetailsEvent.NavigateToEdit(postId))
        }
    }

    fun resolveBookInfo(post: PostEntity) {
        if (resolvedBookId == post.bookId && _uiState.value?.bookInfo != null) return

        val fallbackBook = post.toBook()
        _uiState.value = _uiState.value?.copy(bookInfo = fallbackBook.toCardModel())

        viewModelScope.launch {
            val freshBook = try {
                booksRepository.getBook(post.bookId)
            } catch (e: Exception) {
                _event.value = Event(PostDetailsEvent.ShowMessage(e.toUserFriendlyMessageRes(R.string.error_book_info_load)))
                null
            } ?: fallbackBook

            resolvedBook = freshBook
            resolvedBookId = post.bookId
            _uiState.value = _uiState.value?.copy(bookInfo = freshBook.toCardModel())
        }
    }

    private fun refreshComments() {
        val postId = currentPostId ?: return
        viewModelScope.launch {
            try {
                postsRepository.refreshComments(postId)
            } catch (e: Exception) {
                _event.value = Event(PostDetailsEvent.ShowMessage(e.toUserFriendlyMessageRes(R.string.error_comment_refresh)))
            }
        }
    }

    private fun observeBookMembership(bookId: String) {
        wishlistSource?.let { _uiState.removeSource(it) }
        readlistSource?.let { _uiState.removeSource(it) }

        val userId = currentUserId
        if (userId == null) {
            _uiState.value = _uiState.value?.copy(isWishlisted = false, isReadlisted = false)
            return
        }

        wishlistSource = listsRepository.observeWishlistExists(userId, bookId).also { source ->
            _uiState.addSource(source) { isWishlisted ->
                _uiState.value = _uiState.value?.copy(isWishlisted = isWishlisted)
            }
        }

        readlistSource = listsRepository.observeReadlistExists(userId, bookId).also { source ->
            _uiState.addSource(source) { isReadlisted ->
                _uiState.value = _uiState.value?.copy(isReadlisted = isReadlisted)
            }
        }
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

    companion object {
        fun factory(
            postsRepository: PostsRepository,
            booksRepository: BooksRepository,
            listsRepository: ListsRepository,
            authRepository: AuthRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(PostDetailsViewModel::class.java)) {
                        return PostDetailsViewModel(
                            postsRepository = postsRepository,
                            booksRepository = booksRepository,
                            listsRepository = listsRepository,
                            authRepository = authRepository
                        ) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}

data class PostDetailsUiState(
    val currentUserId: String? = null,
    val post: PostEntity? = null,
    val comments: List<CommentEntity> = emptyList(),
    val bookInfo: BookInfoCardModel? = null,
    val isContentLoading: Boolean = false,
    val isActionProcessing: Boolean = false,
    val isCommentProcessing: Boolean = false,
    val isWishlisted: Boolean = false,
    val isReadlisted: Boolean = false,
    val canEdit: Boolean = false
)

sealed interface PostDetailsEvent {
    data class NavigateToEdit(val postId: String) : PostDetailsEvent
    data class CommentAdded(@StringRes val messageRes: Int) : PostDetailsEvent
    data class ShowMessage(@StringRes val messageRes: Int) : PostDetailsEvent
}
