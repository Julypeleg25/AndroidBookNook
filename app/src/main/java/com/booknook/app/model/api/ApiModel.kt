package com.booknook.app.model.api

import com.booknook.app.model.Book
import com.booknook.app.model.api.dto.BookDto
import com.booknook.app.model.api.dto.SearchResponseDto
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

private interface GoogleBooksApi {
    @GET("volumes")
    suspend fun search(
        @Query("q") q: String,
        @Query("startIndex") startIndex: Int = 0,
        @Query("maxResults") maxResults: Int = ApiModel.MAX_RESULTS_PER_PAGE,
        @Query("key") apiKey: String? = null
    ): SearchResponseDto

    @GET("volumes/{volumeId}")
    suspend fun getVolume(
        @Path("volumeId") volumeId: String,
        @Query("key") apiKey: String? = null
    ): BookDto
}

class ApiModel {

    companion object {
        const val MAX_RESULTS_PER_PAGE = 40
    }

    private val api: GoogleBooksApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/books/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GoogleBooksApi::class.java)
    }

    suspend fun searchBooks(query: String, startIndex: Int = 0): List<Book> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) return emptyList()

        com.booknook.app.util.Logger.d("GoogleBooks", "Search query: $normalizedQuery (startIndex: $startIndex)")
        val res = api.search(
            q = normalizedQuery,
            startIndex = startIndex,
            maxResults = MAX_RESULTS_PER_PAGE,
            apiKey = GoogleBooksConfig.apiKeyOrNull()
        )
        val items = res.items ?: return emptyList()
        return items.mapNotNull { dto -> dto.toDomainBookOrNull() }
    }

    suspend fun getBook(volumeId: String): Book? {
        if (volumeId.isBlank()) return null
        return api.getVolume(
            volumeId = volumeId,
            apiKey = GoogleBooksConfig.apiKeyOrNull()
        ).toDomainBookOrNull()
    }
}

private fun BookDto.toDomainBookOrNull(): Book? {
    val normalizedId = id.trim()
    val info = volumeInfo ?: return null
    if (normalizedId.isEmpty()) return null

    return Book(
        id = normalizedId,
        title = info.title ?: "",
        author = info.authors?.joinToString(", ") ?: "",
        thumbnail = info.imageLinks?.thumbnail?.replace("http://", "https://"),
        publishedDate = info.publishedDate,
        genre = info.categories?.joinToString(", "),
        pageCount = info.pageCount,
        description = info.description
    )
}
