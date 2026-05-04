package com.booknook.app.data.repository

import androidx.lifecycle.LiveData
import com.booknook.app.data.local.LocalCacheDataSource
import com.booknook.app.data.local.entities.ReadlistEntity
import com.booknook.app.data.local.entities.SavedBookListItem
import com.booknook.app.data.local.entities.WishlistEntity
import com.booknook.app.model.Book
import com.booknook.app.model.firebase.FirebaseListsModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class ListsRepository(
    private val local: LocalCacheDataSource,
    private val firebase: FirebaseListsModel
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
        return toggleSavedBook(
            newItem = book.toWishlistEntity(userId, key),
            getLocal = { local.getWishlist(key) },
            upsertLocal = { local.upsertWishlist(it) },
            deleteLocal = { local.deleteWishlist(key) },
            upsertRemote = { firebase.upsertWishlist(it) },
            deleteRemote = { firebase.deleteWishlist(userId, book.id) }
        )
    }

    suspend fun removeFromWishlist(userId: String, bookId: String) {
        val key = buildKey(userId, bookId)
        removeSavedBook(
            getLocal = { local.getWishlist(key) },
            upsertLocal = { local.upsertWishlist(it) },
            deleteLocal = { local.deleteWishlist(key) },
            deleteRemote = { firebase.deleteWishlist(userId, bookId) }
        )
    }

    fun observeReadlist(userId: String): LiveData<List<ReadlistEntity>> = local.observeReadlist(userId)

    fun observeReadlistExists(userId: String, bookId: String): LiveData<Boolean> {
        return local.observeReadlistExists(userId, bookId)
    }

    suspend fun toggleReadlist(userId: String, book: Book): Boolean {
        val key = buildKey(userId, book.id)
        return toggleSavedBook(
            newItem = book.toReadlistEntity(userId, key),
            getLocal = { local.getReadlist(key) },
            upsertLocal = { local.upsertReadlist(it) },
            deleteLocal = { local.deleteReadlist(key) },
            upsertRemote = { firebase.upsertReadlist(it) },
            deleteRemote = { firebase.deleteReadlist(userId, book.id) }
        )
    }

    suspend fun removeFromReadlist(userId: String, bookId: String) {
        val key = buildKey(userId, bookId)
        removeSavedBook(
            getLocal = { local.getReadlist(key) },
            upsertLocal = { local.upsertReadlist(it) },
            deleteLocal = { local.deleteReadlist(key) },
            deleteRemote = { firebase.deleteReadlist(userId, bookId) }
        )
    }

    private fun buildKey(userId: String, bookId: String): String = "$userId|$bookId"

    private suspend fun <T : SavedBookListItem> toggleSavedBook(
        newItem: T,
        getLocal: suspend () -> T?,
        upsertLocal: suspend (T) -> Unit,
        deleteLocal: suspend () -> Unit,
        upsertRemote: suspend (T) -> Unit,
        deleteRemote: suspend () -> Unit
    ): Boolean {
        val existingItem = withContext(Dispatchers.IO) { getLocal() }
        val wasSaved = existingItem != null

        withContext(Dispatchers.IO) {
            if (wasSaved) {
                deleteLocal()
            } else {
                upsertLocal(newItem)
            }
        }

        try {
            if (wasSaved) {
                deleteRemote()
            } else {
                upsertRemote(newItem)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.IO) {
                if (existingItem != null) {
                    upsertLocal(existingItem)
                } else {
                    deleteLocal()
                }
            }
            throw e
        }

        return !wasSaved
    }

    private suspend fun <T : SavedBookListItem> removeSavedBook(
        getLocal: suspend () -> T?,
        upsertLocal: suspend (T) -> Unit,
        deleteLocal: suspend () -> Unit,
        deleteRemote: suspend () -> Unit
    ) {
        val existingItem = withContext(Dispatchers.IO) { getLocal() }

        withContext(Dispatchers.IO) {
            deleteLocal()
        }

        try {
            deleteRemote()
        } catch (e: Exception) {
            if (existingItem != null) {
                withContext(Dispatchers.IO) {
                    upsertLocal(existingItem)
                }
            }
            throw e
        }
    }

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
