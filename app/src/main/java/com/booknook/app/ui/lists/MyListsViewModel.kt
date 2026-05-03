package com.booknook.app.ui.lists

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.booknook.app.R
import com.booknook.app.data.local.entities.SavedBookListItem
import com.booknook.app.data.repository.AuthRepository
import com.booknook.app.data.repository.ListsRepository
import com.booknook.app.util.Event
import com.booknook.app.util.toUserFriendlyMessageRes
import kotlinx.coroutines.launch

class MyListsViewModel(
    private val listsRepository: ListsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private var currentUserId = authRepository.currentUserId()

    private val _uiState = MediatorLiveData(MyListsUiState())
    val uiState: LiveData<MyListsUiState> = _uiState

    private val _event = MutableLiveData<Event<MyListsEvent>>()
    val event: LiveData<Event<MyListsEvent>> = _event
    private var wishlistSource: LiveData<out List<SavedBookListItem>>? = null
    private var readlistSource: LiveData<out List<SavedBookListItem>>? = null

    init {
        bindCurrentUser(currentUserId)

        viewModelScope.launch {
            authRepository.authState
                .collect { userId ->
                    if (userId == currentUserId) return@collect
                    currentUserId = userId
                    bindCurrentUser(userId)
                }
        }
    }

    fun onRemoveFromWishlist(bookId: String) {
        removeBook(
            bookId = bookId,
            remove = { userId, targetBookId -> listsRepository.removeFromWishlist(userId, targetBookId) },
            successMessageRes = R.string.wishlist_removed,
            errorMessageRes = R.string.error_wishlist_remove
        )
    }

    fun onRemoveFromReadlist(bookId: String) {
        removeBook(
            bookId = bookId,
            remove = { userId, targetBookId -> listsRepository.removeFromReadlist(userId, targetBookId) },
            successMessageRes = R.string.readlist_removed,
            errorMessageRes = R.string.error_readlist_remove
        )
    }

    private fun removeBook(
        bookId: String,
        remove: suspend (String, String) -> Unit,
        @StringRes successMessageRes: Int,
        @StringRes errorMessageRes: Int
    ) {
        viewModelScope.launch {
            try {
                val userId = currentUserId ?: return@launch
                remove(userId, bookId)
                _event.value = Event(MyListsEvent.ShowMessage(successMessageRes))
            } catch (e: Exception) {
                _event.value = Event(MyListsEvent.ShowMessage(e.toUserFriendlyMessageRes(errorMessageRes)))
            }
        }
    }

    private fun bindCurrentUser(userId: String?) {
        wishlistSource?.let { _uiState.removeSource(it) }
        readlistSource?.let { _uiState.removeSource(it) }
        _uiState.value = MyListsUiState()

        if (userId == null) {
            return
        }

        wishlistSource = listsRepository.observeWishlist(userId).also { source ->
            _uiState.addSource(source) { wishlist ->
                _uiState.value = _uiState.value?.copy(wishlist = wishlist)
            }
        }
        readlistSource = listsRepository.observeReadlist(userId).also { source ->
            _uiState.addSource(source) { readlist ->
                _uiState.value = _uiState.value?.copy(readlist = readlist)
            }
        }

        viewModelScope.launch {
            try {
                listsRepository.refreshLists(userId)
            } catch (e: Exception) {
                _event.value = Event(MyListsEvent.ShowMessage(e.toUserFriendlyMessageRes(R.string.error_post_action)))
            }
        }
    }

    companion object {
        fun factory(
            listsRepository: ListsRepository,
            authRepository: AuthRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(MyListsViewModel::class.java)) {
                        return MyListsViewModel(listsRepository, authRepository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}

data class MyListsUiState(
    val wishlist: List<SavedBookListItem> = emptyList(),
    val readlist: List<SavedBookListItem> = emptyList()
)

sealed interface MyListsEvent {
    data class ShowMessage(@StringRes val messageRes: Int) : MyListsEvent
}
