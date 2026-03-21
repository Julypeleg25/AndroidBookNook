package com.booknook.app.ui.books

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.booknook.app.domain.Book
import com.booknook.app.model.Model
import com.booknook.app.model.api.ApiModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class BookSearchViewModel : ViewModel() {

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _loadingMore = MutableLiveData(false)
    val loadingMore: LiveData<Boolean> = _loadingMore

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
            _searchResults.value = emptyList()
            _isEmpty.value = false
            return
        }

        if (trimmed == currentQuery) return

        searchJob?.cancel()
        currentQuery = trimmed
        startIndex = 0
        canLoadMore = true
        _searchResults.value = emptyList()

        searchJob = viewModelScope.launch {
            try {
                _loading.value = true
                _error.value = null
                _isEmpty.value = false
                val results = Model.searchBooks(currentQuery, startIndex)
                _searchResults.value = results
                _isEmpty.value = results.isEmpty()
                startIndex = results.size
                canLoadMore = results.isNotEmpty()
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
                val results = Model.searchBooks(currentQuery, startIndex)
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
                // Silently fail or log for "load more"
            } finally {
                _loadingMore.value = false
            }
        }
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
}
