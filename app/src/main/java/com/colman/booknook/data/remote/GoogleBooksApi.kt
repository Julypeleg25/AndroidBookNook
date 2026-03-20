package com.colman.booknook.data.remote

import com.colman.booknook.data.remote.model.BookSearchResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface GoogleBooksApi {
    @GET("volumes")
    suspend fun searchBooks(
        @Query("q") query: String,
        @Query("maxResults") maxResults: Int = 20
    ): Response<BookSearchResponse>
}
