package com.booknook.app.ui.wishlist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.booknook.app.data.local.entities.ReadlistEntity
import com.booknook.app.data.local.entities.WishlistEntity
import com.booknook.app.data.repository.AuthRepository
import com.booknook.app.data.repository.ListsRepository
import kotlinx.coroutines.launch

class WishlistViewModel(
    private val listsRepository: ListsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun observeWishlist(): LiveData<List<WishlistEntity>> {
        val userId = authRepository.currentUserId() ?: return MutableLiveData(emptyList())
        return listsRepository.observeWishlist(userId)
    }

    fun observeReadlist(): LiveData<List<ReadlistEntity>> {
        val userId = authRepository.currentUserId() ?: return MutableLiveData(emptyList())
        return listsRepository.observeReadlist(userId)
    }

    fun removeFromWishlist(bookId: String) {
        viewModelScope.launch {
            try {
                val userId = authRepository.currentUserId() ?: return@launch
                listsRepository.removeFromWishlist(userId, bookId)
            } catch (e: Exception) {
                _error.value = "Failed to remove from wishlist"
            }
        }
    }

    fun removeFromReadlist(bookId: String) {
        viewModelScope.launch {
            try {
                val userId = authRepository.currentUserId() ?: return@launch
                listsRepository.removeFromReadlist(userId, bookId)
            } catch (e: Exception) {
                _error.value = "Failed to remove from readlist"
            }
        }
    }

    companion object {
        fun factory(
            listsRepository: ListsRepository,
            authRepository: AuthRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(WishlistViewModel::class.java)) {
                        return WishlistViewModel(listsRepository, authRepository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}
