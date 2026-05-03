package com.booknook.app.ui.books

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.booknook.app.data.repository.BooksRepository
import com.booknook.app.model.Book
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class BookSearchViewModel(
    private val booksRepository: BooksRepository
) : ViewModel() {

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _loadingMore = MutableLiveData(false)

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _searchResults = MutableLiveData<List<Book>>(emptyList())
    val searchResults: LiveData<List<Book>> = _searchResults

    private val _isEmpty = MutableLiveData(false)
    val isEmpty: LiveData<Boolean> = _isEmpty

    private var currentQuery: String = ""
    private var startIndex = 0
    private var canLoadMore = true
    private var searchJob: Job? = null

    fun searchBooks(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            clearSearch()
            return
        }

        if (trimmed == currentQuery) return

        prepareNewSearch(trimmed)

        searchJob = viewModelScope.launch {
            try {
                setInitialLoading()
                val books = booksRepository.searchBooks(currentQuery, startIndex)
                applyInitialResults(books)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = mapErrorMessage(e)
            } finally {
                _loading.value = false
            }
        }
    }

    fun loadMore() {
        if (_loading.value == true || _loadingMore.value == true || !canLoadMore || currentQuery.isEmpty()) return

        viewModelScope.launch {
            try {
                _loadingMore.value = true
                val books = booksRepository.searchBooks(currentQuery, startIndex)
                applyMoreResults(books)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = mapErrorMessage(e)
            } finally {
                _loadingMore.value = false
            }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        currentQuery = ""
        startIndex = 0
        canLoadMore = true
        _loading.value = false
        _loadingMore.value = false
        _error.value = null
        _searchResults.value = emptyList()
        _isEmpty.value = false
    }

    private fun prepareNewSearch(query: String) {
        searchJob?.cancel()
        currentQuery = query
        startIndex = 0
        canLoadMore = true
        _searchResults.value = emptyList()
    }

    private fun setInitialLoading() {
        _loading.value = true
        _error.value = null
        _isEmpty.value = false
    }

    private fun applyInitialResults(books: List<Book>) {
        _searchResults.value = books
        _isEmpty.value = books.isEmpty()
        startIndex = books.size
        canLoadMore = books.isNotEmpty()
    }

    private fun applyMoreResults(books: List<Book>) {
        if (books.isNotEmpty()) {
            val currentBooks = _searchResults.value.orEmpty()
            _searchResults.value = (currentBooks + books).distinctBy { it.id }
            startIndex += books.size
        }
        canLoadMore = books.isNotEmpty()
    }

    private fun mapErrorMessage(e: Exception): String {
        val msg = e.message ?: return "Search failed"
        return when {
            msg.contains("network", ignoreCase = true) || msg.contains("offline", ignoreCase = true) ->
                "Device is offline. Please check your connection."
            msg.contains("Timed out", ignoreCase = true) -> "Connection timed out."
            else -> msg
        }
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
