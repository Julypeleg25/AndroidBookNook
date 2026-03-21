package com.booknook.app.ui.wishlist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.booknook.app.R
import com.booknook.app.data.local.entities.ReadlistEntity
import com.booknook.app.data.local.entities.WishlistEntity
import com.booknook.app.model.Model
import com.booknook.app.util.toUserFriendlyMessageRes
import kotlinx.coroutines.launch

class WishlistViewModel : ViewModel() {

    private val _error = MutableLiveData<Int?>()
    val error: LiveData<Int?> = _error

    fun observeWishlist(): LiveData<List<WishlistEntity>> {
        val uid = Model.authRepository.currentUserId() ?: return MutableLiveData(emptyList())
        return Model.listsRepository.observeWishlist(uid)
    }

    fun observeReadlist(): LiveData<List<ReadlistEntity>> {
        val uid = Model.authRepository.currentUserId() ?: return MutableLiveData(emptyList())
        return Model.listsRepository.observeReadlist(uid)
    }

    fun removeFromWishlist(bookId: String) {
        viewModelScope.launch {
            try {
                val uid = Model.authRepository.currentUserId() ?: return@launch
                Model.listsRepository.removeFromWishlist(uid, bookId)
            } catch (e: Exception) {
                _error.value = e.toUserFriendlyMessageRes(R.string.error_wishlist_remove)
            }
        }
    }

    fun removeFromReadlist(bookId: String) {
        viewModelScope.launch {
            try {
                val uid = Model.authRepository.currentUserId() ?: return@launch
                Model.listsRepository.removeFromReadlist(uid, bookId)
            } catch (e: Exception) {
                _error.value = e.toUserFriendlyMessageRes(R.string.error_readlist_remove)
            }
        }
    }
}
