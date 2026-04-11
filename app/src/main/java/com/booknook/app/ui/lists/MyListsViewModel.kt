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

    private val currentUserId = authRepository.currentUserId()

    private val _uiState = MediatorLiveData(MyListsUiState())
    val uiState: LiveData<MyListsUiState> = _uiState

    private val _event = MutableLiveData<Event<MyListsEvent>>()
    val event: LiveData<Event<MyListsEvent>> = _event

    init {
        _uiState.value = MyListsUiState()
        val userId = currentUserId
        if (userId != null) {
            _uiState.addSource(listsRepository.observeWishlist(userId)) { wishlist ->
                _uiState.value = _uiState.value?.copy(wishlist = wishlist)
            }
            _uiState.addSource(listsRepository.observeReadlist(userId)) { readlist ->
                _uiState.value = _uiState.value?.copy(readlist = readlist)
            }

            viewModelScope.launch {
                try {
                    listsRepository.refreshLists(userId)
                } catch (e: Exception) {
                    _event.value = Event(MyListsEvent.ShowMessage(e.toUserFriendlyMessageRes(R.string.error_post_action)))
                }
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
