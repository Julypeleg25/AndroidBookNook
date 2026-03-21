package com.booknook.app.ui.posts

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.booknook.app.data.local.entities.PostEntity
import com.booknook.app.domain.Book
import com.booknook.app.model.Model
import com.booknook.app.ui.auth.Event
import kotlinx.coroutines.launch

class CreatePostViewModel : ViewModel() {

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _saveSuccess = MutableLiveData<Event<Boolean>>()
    val saveSuccess: LiveData<Event<Boolean>> = _saveSuccess

    fun observePost(postId: String): LiveData<PostEntity?> = Model.observePost(postId)

    fun createPost(book: Book, rating: Int, review: String, imageUri: Uri?) {
        if (_loading.value == true) return // Prevent double tap
        _loading.value = true
        com.booknook.app.util.Logger.d("CreatePost", "Starting post creation for: ${book.title}")
        
        viewModelScope.launch {
            try {
                Model.createPost(book, rating, review, imageUri)
                com.booknook.app.util.Logger.d("CreatePost", "Post saved successfully")
                _saveSuccess.value = Event(true)
            } catch (e: Exception) {
                com.booknook.app.util.Logger.e("CreatePost", "Failed to save post", e)
                _error.value = mapErrorMessage(e, "Failed to save post")
            } finally {
                _loading.value = false
            }
        }
    }

    fun updatePost(postId: String, rating: Int, review: String, imageUri: Uri?) {
        if (_loading.value == true) return
        _loading.value = true
        com.booknook.app.util.Logger.d("CreatePost", "Updating post: $postId")

        viewModelScope.launch {
            try {
                Model.updatePost(postId, rating, review, imageUri)
                com.booknook.app.util.Logger.d("CreatePost", "Post $postId updated successfully")
                _saveSuccess.value = Event(true)
            } catch (e: Exception) {
                com.booknook.app.util.Logger.e("CreatePost", "Failed to update post", e)
                _error.value = mapErrorMessage(e, "Failed to update post")
            } finally {
                _loading.value = false
            }
        }
    }

    private fun mapErrorMessage(e: Exception, default: String): String {
        val msg = e.message ?: return default
        return when {
            msg.contains("network", ignoreCase = true) || msg.contains("offline", ignoreCase = true) -> 
                "Device is offline. Action will sync when online."
            msg.contains("PERMISSION_DENIED", ignoreCase = true) || msg.contains("API has not been used", ignoreCase = true) -> 
                "Project error: Firestore API is disabled in Firebase Console."
            msg.contains("timeout", ignoreCase = true) || msg.contains("Timed out", ignoreCase = true) -> 
                "Connection timed out. If your internet is fine, check if Firestore API is enabled in Firebase Console."
            else -> msg
        }
    }
}
