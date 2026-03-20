package com.colman.booknook.model.api

import com.colman.booknook.domain.Book
import com.colman.booknook.model.api.dto.SearchResponseDto
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
            Book(
                id = it.id,
                title = it.volumeInfo.title ?: "",
                author = it.volumeInfo.authors?.joinToString(", ") ?: "",
                thumbnail = it.volumeInfo.imageLinks?.thumbnail
            )
        }
    }
}
