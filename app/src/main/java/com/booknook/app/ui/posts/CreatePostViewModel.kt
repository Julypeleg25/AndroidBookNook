package com.booknook.app.ui.posts

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.booknook.app.R
import com.booknook.app.data.local.entities.PostEntity
import com.booknook.app.domain.Book
import com.booknook.app.model.Model
import com.booknook.app.ui.auth.Event
import com.booknook.app.util.toUserFriendlyMessageRes
import kotlinx.coroutines.launch

class CreatePostViewModel : ViewModel() {

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<Int?>()
    val error: LiveData<Int?> = _error

    private val _saveSuccess = MutableLiveData<Event<Boolean>>()
    val saveSuccess: LiveData<Event<Boolean>> = _saveSuccess

    fun observePost(postId: String): LiveData<PostEntity?> {
        val currentUserId = Model.authRepository.currentUserId().orEmpty()
        return Model.postsRepository.observePost(postId, currentUserId)
    }

    fun createPost(book: Book, rating: Int, review: String, imageUri: Uri?) {
        if (_loading.value == true) return 
        _loading.value = true
        com.booknook.app.util.Logger.d("CreatePost", "Starting post creation for: ${book.title}")
        
        viewModelScope.launch {
            try {
                Model.postsRepository.createPost(book, rating, review, imageUri)
                com.booknook.app.util.Logger.d("CreatePost", "Post saved successfully")
                _saveSuccess.value = Event(true)
            } catch (e: Exception) {
                com.booknook.app.util.Logger.e("CreatePost", "Failed to save post", e)
                _error.value = e.toUserFriendlyMessageRes(R.string.create_post_save_failed)
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
                Model.postsRepository.updatePost(postId, rating, review, imageUri)
                com.booknook.app.util.Logger.d("CreatePost", "Post $postId updated successfully")
                _saveSuccess.value = Event(true)
            } catch (e: Exception) {
                com.booknook.app.util.Logger.e("CreatePost", "Failed to update post", e)
                _error.value = e.toUserFriendlyMessageRes(R.string.create_post_update_failed)
            } finally {
                _loading.value = false
            }
        }
    }
}
