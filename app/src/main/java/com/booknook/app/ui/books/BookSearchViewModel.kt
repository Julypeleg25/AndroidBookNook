package com.booknook.app.ui.books

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.booknook.app.domain.Book
import com.booknook.app.model.Model
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BookSearchViewModel : ViewModel() {

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _searchResults = MutableLiveData<List<Book>>()
    val searchResults: LiveData<List<Book>> = _searchResults

    private var searchJob: Job? = null

    fun searchBooks(query: String) {
        if (query.isEmpty()) {
            _searchResults.value = emptyList()
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            try {
                delay(800)
                _loading.value = true
                _error.value = null
                val results = Model.searchBooks(query)
                _searchResults.value = results
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = "Search failed: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
}
