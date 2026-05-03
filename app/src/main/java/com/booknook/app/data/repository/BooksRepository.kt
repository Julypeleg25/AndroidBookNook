package com.booknook.app.data.repository

import com.booknook.app.data.local.LocalCacheDataSource
import com.booknook.app.data.local.entities.CachedBookEntity
import com.booknook.app.model.Book
import com.booknook.app.model.api.ApiModel
import com.booknook.app.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BooksRepository(
    private val local: LocalCacheDataSource,
    private val api: ApiModel
) {
    suspend fun searchBooks(query: String, startIndex: Int = 0): List<Book> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) return emptyList()

        return try {
            val remoteBooks = api.searchBooks(normalizedQuery, startIndex)
            if (remoteBooks.isNotEmpty()) {
                cacheBooks(remoteBooks)
                remoteBooks
            } else {
                searchCachedBooks(normalizedQuery, startIndex)
            }
        } catch (e: Exception) {
            Logger.e("Books", "Remote search failed for \"$normalizedQuery\". Falling back to cache.", e)
            searchCachedBooks(normalizedQuery, startIndex)
        }
    }

    suspend fun getBook(bookId: String): Book? {
        return try {
            val book = api.getBook(bookId)
            if (book != null) {
                cacheBooks(listOf(book))
            }
            book
        } catch (e: Exception) {
            Logger.e("Books", "Remote book fetch failed for $bookId. Falling back to cache.", e)
            withContext(Dispatchers.IO) {
                local.getCachedBook(bookId)?.toDomainBook()
            }
        }
    }

    private suspend fun cacheBooks(books: List<Book>) {
        withContext(Dispatchers.IO) {
            local.cacheBooks(books.map { it.toCachedEntity() })
        }
    }

    private suspend fun searchCachedBooks(query: String, startIndex: Int): List<Book> {
        return withContext(Dispatchers.IO) {
            local.searchCachedBooks(
                query = query,
                limit = ApiModel.MAX_RESULTS_PER_PAGE,
                offset = startIndex
            ).map { it.toDomainBook() }
        }
    }
}

private fun Book.toCachedEntity(): CachedBookEntity {
    return CachedBookEntity(
        id = id,
        title = title,
        author = author,
        thumbnail = thumbnail,
        publishedDate = publishedDate,
        genre = genre,
        pageCount = pageCount,
        description = description,
        lastFetchedAt = System.currentTimeMillis()
    )
}

private fun CachedBookEntity.toDomainBook(): Book {
    return Book(
        id = id,
        title = title,
        author = author,
        thumbnail = thumbnail,
        publishedDate = publishedDate,
        genre = genre,
        pageCount = pageCount,
        description = description
    )
}
