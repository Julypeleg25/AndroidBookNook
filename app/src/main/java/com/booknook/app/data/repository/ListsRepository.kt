package com.booknook.app.data.repository

import androidx.lifecycle.LiveData
import com.booknook.app.data.local.LocalCacheDataSource
import com.booknook.app.data.local.entities.ReadlistEntity
import com.booknook.app.data.local.entities.WishlistEntity
import com.booknook.app.model.Book
import com.booknook.app.model.firebase.FirebaseModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class ListsRepository(
    private val local: LocalCacheDataSource,
    private val firebase: FirebaseModel
) {
    fun observeWishlist(userId: String): LiveData<List<WishlistEntity>> = local.observeWishlist(userId)

    fun observeWishlistExists(userId: String, bookId: String): LiveData<Boolean> {
        return local.observeWishlistExists(userId, bookId)
    }

    suspend fun refreshLists(userId: String) = coroutineScope {
        val localWishlistDeferred = async(Dispatchers.IO) { local.getWishlistByUser(userId) }
        val localReadlistDeferred = async(Dispatchers.IO) { local.getReadlistByUser(userId) }
        val wishlistDeferred = async { firebase.fetchWishlist(userId) }
        val readlistDeferred = async { firebase.fetchReadlist(userId) }

        val localWishlist = localWishlistDeferred.await()
        val localReadlist = localReadlistDeferred.await()
        val remoteWishlist = wishlistDeferred.await()
        val remoteReadlist = readlistDeferred.await()

        val resolvedWishlist = resolveRemoteSeed<WishlistEntity>(
            remoteItems = remoteWishlist,
            localItems = localWishlist,
            upsertRemote = { firebase.upsertWishlist(it) }
        )
        val resolvedReadlist = resolveRemoteSeed<ReadlistEntity>(
            remoteItems = remoteReadlist,
            localItems = localReadlist,
            upsertRemote = { firebase.upsertReadlist(it) }
        )

        withContext(Dispatchers.IO) {
            local.replaceWishlist(userId, resolvedWishlist)
            local.replaceReadlist(userId, resolvedReadlist)
        }
    }

    suspend fun toggleWishlist(userId: String, book: Book): Boolean {
        val key = buildKey(userId, book.id)
        val newItem = book.toWishlistEntity(userId, key)
        val existingItem = withContext(Dispatchers.IO) { local.getWishlist(key) }
        val wasSaved = existingItem != null

        withContext(Dispatchers.IO) {
            if (wasSaved) {
                local.deleteWishlist(key)
            } else {
                local.upsertWishlist(newItem)
            }
        }

        try {
            if (wasSaved) {
                firebase.deleteWishlist(userId, book.id)
            } else {
                firebase.upsertWishlist(newItem)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.IO) {
                if (wasSaved) {
                    local.upsertWishlist(existingItem!!)
                } else if (!wasSaved) {
                    local.deleteWishlist(key)
                }
            }
            throw e
        }

        return !wasSaved
    }

    suspend fun removeFromWishlist(userId: String, bookId: String) {
        val key = buildKey(userId, bookId)
        val existingItem = withContext(Dispatchers.IO) { local.getWishlist(key) }

        withContext(Dispatchers.IO) {
            local.deleteWishlist(key)
        }

        try {
            firebase.deleteWishlist(userId, bookId)
        } catch (e: Exception) {
            if (existingItem != null) {
                withContext(Dispatchers.IO) {
                    local.upsertWishlist(existingItem)
                }
            }
            throw e
        }
    }

    fun observeReadlist(userId: String): LiveData<List<ReadlistEntity>> = local.observeReadlist(userId)

    fun observeReadlistExists(userId: String, bookId: String): LiveData<Boolean> {
        return local.observeReadlistExists(userId, bookId)
    }

    suspend fun toggleReadlist(userId: String, book: Book): Boolean {
        val key = buildKey(userId, book.id)
        val newItem = book.toReadlistEntity(userId, key)
        val existingItem = withContext(Dispatchers.IO) { local.getReadlist(key) }
        val wasSaved = existingItem != null

        withContext(Dispatchers.IO) {
            if (wasSaved) {
                local.deleteReadlist(key)
            } else {
                local.upsertReadlist(newItem)
            }
        }

        try {
            if (wasSaved) {
                firebase.deleteReadlist(userId, book.id)
            } else {
                firebase.upsertReadlist(newItem)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.IO) {
                if (wasSaved) {
                    local.upsertReadlist(existingItem!!)
                } else if (!wasSaved) {
                    local.deleteReadlist(key)
                }
            }
            throw e
        }

        return !wasSaved
    }

    suspend fun removeFromReadlist(userId: String, bookId: String) {
        val key = buildKey(userId, bookId)
        val existingItem = withContext(Dispatchers.IO) { local.getReadlist(key) }

        withContext(Dispatchers.IO) {
            local.deleteReadlist(key)
        }

        try {
            firebase.deleteReadlist(userId, bookId)
        } catch (e: Exception) {
            if (existingItem != null) {
                withContext(Dispatchers.IO) {
                    local.upsertReadlist(existingItem)
                }
            }
            throw e
        }
    }

    private fun buildKey(userId: String, bookId: String): String = "$userId|$bookId"

    private suspend fun <T> resolveRemoteSeed(
        remoteItems: List<T>,
        localItems: List<T>,
        upsertRemote: suspend (T) -> Unit
    ): List<T> {
        if (remoteItems.isNotEmpty() || localItems.isEmpty()) {
            return remoteItems
        }

        localItems.forEach { item ->
            upsertRemote(item)
        }
        return localItems
    }

    private fun Book.toWishlistEntity(userId: String, key: String): WishlistEntity {
        return WishlistEntity(
            key = key,
            userId = userId,
            bookId = id,
            title = title,
            author = author,
            thumbnail = thumbnail,
            addedAt = System.currentTimeMillis(),
            genre = genre,
            publishedDate = publishedDate,
            pageCount = pageCount,
            description = description
        )
    }

    private fun Book.toReadlistEntity(userId: String, key: String): ReadlistEntity {
        return ReadlistEntity(
            key = key,
            userId = userId,
            bookId = id,
            title = title,
            author = author,
            thumbnail = thumbnail,
            addedAt = System.currentTimeMillis(),
            genre = genre,
            publishedDate = publishedDate,
            pageCount = pageCount,
            description = description
        )
    }
}
