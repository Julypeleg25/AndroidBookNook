package com.booknook.app.ui.wishlist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.booknook.app.data.local.entities.ReadlistEntity
import com.booknook.app.data.local.entities.WishlistEntity
import com.booknook.app.model.Model
import kotlinx.coroutines.launch

class WishlistViewModel : ViewModel() {

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun observeWishlist(): LiveData<List<WishlistEntity>> {
        val uid = Model.currentUserId() ?: return MutableLiveData(emptyList())
        return Model.observeWishlist(uid)
    }

    fun observeReadlist(): LiveData<List<ReadlistEntity>> {
        val uid = Model.currentUserId() ?: return MutableLiveData(emptyList())
        return Model.observeReadlist(uid)
    }

    fun removeFromWishlist(bookId: String) {
        viewModelScope.launch {
            try {
                val uid = Model.currentUserId() ?: return@launch
                Model.removeFromWishlist(uid, bookId)
            } catch (e: Exception) {
                _error.value = "Failed to remove from wishlist"
            }
        }
    }

    fun removeFromReadlist(bookId: String) {
        viewModelScope.launch {
            try {
                val uid = Model.currentUserId() ?: return@launch
                Model.removeFromReadlist(uid, bookId)
            } catch (e: Exception) {
                _error.value = "Failed to remove from readlist"
            }
        }
    }
}
