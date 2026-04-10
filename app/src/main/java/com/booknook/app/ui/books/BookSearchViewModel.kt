package com.booknook.app.ui.books

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.booknook.app.R
import com.booknook.app.domain.Book
import com.booknook.app.model.Model
import com.booknook.app.util.toUserFriendlyMessageRes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class BookSearchViewModel : ViewModel() {

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _loadingMore = MutableLiveData(false)
    val loadingMore: LiveData<Boolean> = _loadingMore

    private val _error = MutableLiveData<Int?>()
    val error: LiveData<Int?> = _error

    private val _searchResults = MutableLiveData<List<Book>>(emptyList())
    val searchResults: LiveData<List<Book>> = _searchResults

    private val _isEmpty = MutableLiveData(false)
    val isEmpty: LiveData<Boolean> = _isEmpty

    private val _hasSearched = MutableLiveData(false)
    val hasSearched: LiveData<Boolean> = _hasSearched

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

        searchJob?.cancel()
        currentQuery = trimmed
        startIndex = 0
        canLoadMore = true
        _searchResults.value = emptyList()
        _hasSearched.value = true

        searchJob = viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = null
                _isEmpty.value = false
                val results = Model.booksRepository.searchBooks(currentQuery, startIndex)
                _searchResults.value = results
                _isEmpty.value = results.isEmpty()
                startIndex = results.size
                canLoadMore = results.isNotEmpty()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = e.toUserFriendlyMessageRes(R.string.search_failed)
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
                val results = Model.booksRepository.searchBooks(currentQuery, startIndex)
                if (results.isNotEmpty()) {
                    val currentList = _searchResults.value ?: emptyList()
                    val newList = currentList + results
                    _searchResults.value = newList.distinctBy { it.id }
                    startIndex += results.size
                }
                canLoadMore = results.isNotEmpty()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                com.booknook.app.util.Logger.e("BookSearch", "Failed to load more books", e)
                _error.value = e.toUserFriendlyMessageRes(R.string.search_failed)
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
        _hasSearched.value = false
    }
}
