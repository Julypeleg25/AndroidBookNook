package com.booknook.app.data.repository

import com.booknook.app.data.local.entities.CachedBookEntity
import com.booknook.app.domain.Book
import com.booknook.app.model.api.ApiModel
import com.booknook.app.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BooksRepository(
    private val local: AppLocalRepository,
    private val api: ApiModel
) {
    suspend fun searchBooks(query: String, startIndex: Int = 0): List<Book> {
        return try {
            val books = api.searchBooks(query, startIndex)
            withContext(Dispatchers.IO) {
                local.cacheBooks(books.map { it.toCachedEntity() })
            }
            books
        } catch (e: Exception) {
            Logger.e("Books", "Remote search failed. Falling back to cache.", e)
            withContext(Dispatchers.IO) {
                local.searchCachedBooks(
                    query = query.trim(),
                    limit = ApiModel.MAX_RESULTS_PER_PAGE,
                    offset = startIndex
                ).map { it.toDomainBook() }
            }
        }
    }

    suspend fun getBook(bookId: String): Book? {
        return try {
            val book = api.getBook(bookId)
            if (book != null) {
                withContext(Dispatchers.IO) {
                    local.cacheBooks(listOf(book.toCachedEntity()))
                }
            }
            book
        } catch (e: Exception) {
            Logger.e("Books", "Remote book fetch failed for $bookId. Falling back to cache.", e)
            withContext(Dispatchers.IO) {
                local.getCachedBook(bookId)?.toDomainBook()
            }
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
