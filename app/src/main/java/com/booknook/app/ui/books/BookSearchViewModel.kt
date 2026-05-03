package com.booknook.app.ui.books

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.booknook.app.R
import com.booknook.app.data.repository.BooksRepository
import com.booknook.app.model.Book
import com.booknook.app.util.Event
import com.booknook.app.util.toUserFriendlyMessageRes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class BookSearchViewModel(
    private val booksRepository: BooksRepository
) : ViewModel() {

    private val _uiState = MutableLiveData(BookSearchUiState())
    val uiState: LiveData<BookSearchUiState> = _uiState

    private val _event = MutableLiveData<Event<BookSearchEvent>>()
    val event: LiveData<Event<BookSearchEvent>> = _event

    private var currentQuery: String = ""
    private var startIndex = 0
    private var canLoadMore = true
    private var searchJob: Job? = null

    fun onQueryChanged(query: String) {
        if (_uiState.value?.query == query) return
        _uiState.value = _uiState.value?.copy(query = query)
        if (query.isBlank() && _uiState.value?.hasSearched == true) {
            clearSearch()
        }
    }

    fun onSearchRequested() {
        val trimmedQuery = _uiState.value?.query?.trim().orEmpty()
        if (trimmedQuery.isEmpty()) {
            _event.value = Event(BookSearchEvent.ShowMessage(R.string.search_query_required))
            return
        }

        searchJob?.cancel()
        currentQuery = trimmedQuery
        startIndex = 0
        canLoadMore = true
        _uiState.value = _uiState.value?.copy(
            query = trimmedQuery,
            results = emptyList(),
            isInitialLoading = true,
            isLoadingMore = false,
            hasSearched = true,
            isEmpty = false,
            isEndReached = false
        )

        searchJob = viewModelScope.launch {
            try {
                val results = booksRepository.searchBooks(currentQuery, startIndex)
                startIndex = results.size
                canLoadMore = results.isNotEmpty()
                _uiState.value = _uiState.value?.copy(
                    results = results,
                    isInitialLoading = false,
                    isEmpty = results.isEmpty(),
                    isEndReached = results.isEmpty()
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value?.copy(isInitialLoading = false)
                _event.value = Event(BookSearchEvent.ShowMessage(e.toUserFriendlyMessageRes(R.string.search_failed)))
            }
        }
    }

    fun loadMore() {
        val state = _uiState.value ?: return
        if (state.isInitialLoading || state.isLoadingMore || !canLoadMore || currentQuery.isEmpty() || state.isEndReached) {
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value?.copy(isLoadingMore = true)
                val results = booksRepository.searchBooks(currentQuery, startIndex)
                val currentList = _uiState.value?.results.orEmpty()
                val newList = (currentList + results).distinctBy { it.id }
                startIndex += results.size
                canLoadMore = results.isNotEmpty()
                _uiState.value = _uiState.value?.copy(
                    results = newList,
                    isLoadingMore = false,
                    isEmpty = newList.isEmpty(),
                    isEndReached = results.isEmpty()
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                com.booknook.app.util.Logger.e("BookSearch", "Failed to load more books", e)
                _uiState.value = _uiState.value?.copy(isLoadingMore = false)
                _event.value = Event(BookSearchEvent.ShowMessage(e.toUserFriendlyMessageRes(R.string.search_failed)))
            }
        }
    }

    fun onBookSelected(book: Book) {
        _event.value = Event(BookSearchEvent.NavigateToCreatePost(book))
    }

    fun clearSearch() {
        searchJob?.cancel()
        currentQuery = ""
        startIndex = 0
        canLoadMore = true
        _uiState.value = BookSearchUiState()
    }

    companion object {
        fun factory(booksRepository: BooksRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(BookSearchViewModel::class.java)) {
                        return BookSearchViewModel(booksRepository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}

data class BookSearchUiState(
    val query: String = "",
    val results: List<Book> = emptyList(),
    val isInitialLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasSearched: Boolean = false,
    val isEmpty: Boolean = false,
    val isEndReached: Boolean = false
)

sealed interface BookSearchEvent {
    data class NavigateToCreatePost(val book: Book) : BookSearchEvent
    data class ShowMessage(@StringRes val messageRes: Int) : BookSearchEvent
}
