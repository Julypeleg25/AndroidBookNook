package com.booknook.app.ui.posts

import android.net.Uri
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

class CreatePostViewModel : ViewModel() {

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _saveSuccess = MutableLiveData<Boolean>()
    val saveSuccess: LiveData<Boolean> = _saveSuccess

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

    fun createPost(book: Book, rating: Int, review: String, imageUri: Uri?) {
        _loading.value = true
        viewModelScope.launch {
            try {
                Model.createPost(book, rating, review, imageUri)
                _saveSuccess.value = true
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to save post"
            } finally {
                _loading.value = false
            }
        }
    }

    fun updatePost(postId: String, rating: Int, review: String, imageUri: Uri?) {
        _loading.value = true
        viewModelScope.launch {
            try {
                Model.updatePost(postId, rating, review, imageUri)
                _saveSuccess.value = true
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to update post"
            } finally {
                _loading.value = false
            }
        }
    }

    fun resetSaveSuccess() {
        _saveSuccess.value = false
    }
}
