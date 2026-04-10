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

    private val _removeFeedback = MutableLiveData<Int?>()
    val removeFeedback: LiveData<Int?> = _removeFeedback

    fun removeFromWishlist(bookId: String) {
        viewModelScope.launch {
            try {
                val uid = Model.authRepository.currentUserId() ?: return@launch
                Model.listsRepository.removeFromWishlist(uid, bookId)
                _removeFeedback.value = R.string.wishlist_removed
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
                _removeFeedback.value = R.string.readlist_removed
            } catch (e: Exception) {
                _error.value = e.toUserFriendlyMessageRes(R.string.error_readlist_remove)
            }
        }
    }

    fun resetRemoveFeedback() {
        _removeFeedback.value = null
    }
}
