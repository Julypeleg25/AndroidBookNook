package com.booknook.app.model.api

import com.booknook.app.domain.Book
import com.booknook.app.model.api.dto.SearchResponseDto
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

private interface GoogleBooksApi {
    @GET("volumes")
    suspend fun search(
        @Query("q") q: String,
        @Query("maxResults") maxResults: Int = 20,
        @Query("key") apiKey: String? = null
    ): SearchResponseDto
}

class ApiModel {

    private val GOOGLE_BOOKS_API_KEY = "AIzaSyANFioq_qMn6WNL-CyJshKHboYRhi1CRCA"

    private val cache = mutableMapOf<String, List<Book>>()

    private val api: GoogleBooksApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/books/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GoogleBooksApi::class.java)
    }

    suspend fun searchBooks(query: String): List<Book> {
        val q = query.lowercase().trim()
        cache[q]?.let { return it }

        val keyParam = if (GOOGLE_BOOKS_API_KEY.isNotEmpty()) GOOGLE_BOOKS_API_KEY else null
        val res = api.search(q, apiKey = keyParam)
        val items = res.items ?: emptyList()
        val books = items.map {
            Book(
                id = it.id,
                title = it.volumeInfo.title ?: "",
                author = it.volumeInfo.authors?.joinToString(", ") ?: "",
                thumbnail = it.volumeInfo.imageLinks?.thumbnail?.replace("http://", "https://")
            )
        }
        cache[q] = books
        return books
    }
}
