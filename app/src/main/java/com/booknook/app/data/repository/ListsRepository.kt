package com.booknook.app.data.repository

import androidx.lifecycle.LiveData
import com.booknook.app.data.local.entities.ReadlistEntity
import com.booknook.app.data.local.entities.WishlistEntity
import com.booknook.app.domain.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ListsRepository(
    private val local: AppLocalRepository
) {
    fun observeWishlist(userId: String): LiveData<List<WishlistEntity>> = local.observeWishlist(userId)

    fun observeWishlistExists(userId: String, bookId: String): LiveData<Boolean> {
        return local.observeWishlistExists("$userId|$bookId")
    }

    suspend fun toggleWishlist(userId: String, book: Book): Boolean {
        val key = "$userId|${book.id}"
        return withContext(Dispatchers.IO) {
            if (local.existsInWishlist(key)) {
                local.deleteWishlist(key)
                false
            } else {
                local.upsertWishlist(
                    WishlistEntity(
                        key = key,
                        userId = userId,
                        bookId = book.id,
                        title = book.title,
                        author = book.author,
                        thumbnail = book.thumbnail,
                        addedAt = System.currentTimeMillis(),
                        genre = book.genre,
                        publishedDate = book.publishedDate,
                        pageCount = book.pageCount,
                        description = book.description
                    )
                )
                true
            }
        }
    }

    suspend fun removeFromWishlist(userId: String, bookId: String) {
        val key = "$userId|$bookId"
        withContext(Dispatchers.IO) {
            local.deleteWishlist(key)
        }
    }

    fun observeReadlist(userId: String): LiveData<List<ReadlistEntity>> = local.observeReadlist(userId)

    fun observeReadlistExists(userId: String, bookId: String): LiveData<Boolean> {
        return local.observeReadlistExists("$userId|$bookId")
    }

    suspend fun toggleReadlist(userId: String, book: Book): Boolean {
        val key = "$userId|${book.id}"
        return withContext(Dispatchers.IO) {
            if (local.existsInReadlist(key)) {
                local.deleteReadlist(key)
                false
            } else {
                local.upsertReadlist(
                    ReadlistEntity(
                        key = key,
                        userId = userId,
                        bookId = book.id,
                        title = book.title,
                        author = book.author,
                        thumbnail = book.thumbnail,
                        addedAt = System.currentTimeMillis(),
                        genre = book.genre,
                        publishedDate = book.publishedDate,
                        pageCount = book.pageCount,
                        description = book.description
                    )
                )
                true
            }
        }
    }

    suspend fun removeFromReadlist(userId: String, bookId: String) {
        val key = "$userId|$bookId"
        withContext(Dispatchers.IO) {
            local.deleteReadlist(key)
        }
    }
}
