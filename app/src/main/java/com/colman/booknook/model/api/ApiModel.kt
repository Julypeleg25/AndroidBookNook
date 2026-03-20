package com.booknook.app.model.api

import com.booknook.app.domain.Book
import com.booknook.app.model.api.dto.SearchResponseDto
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

private interface GoogleBooksApi {
    @GET("volumes")
    suspend fun search(@Query("q") q: String, @Query("maxResults") maxResults: Int = 20): SearchResponseDto
}

class ApiModel {

    private val api: GoogleBooksApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/books/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GoogleBooksApi::class.java)
    }

    suspend fun searchBooks(query: String): List<Book> {
        val res = api.search(query)
        val items = res.items ?: emptyList()
        return items.map {
            val title = it.volumeInfo.title ?: ""
            val author = it.volumeInfo.authors?.joinToString(", ") ?: ""
            Book(
                id = it.id,
                title = title,
                author = author,
                thumbnail = it.volumeInfo.imageLinks?.thumbnail
            )
        }
    }
}
