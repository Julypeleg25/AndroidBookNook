package com.booknook.app.model.api

import com.booknook.app.domain.Book
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
        @Query("maxResults") maxResults: Int = 40
    ): SearchResponseDto

    @GET("volumes/{volumeId}")
    suspend fun getVolume(
        @Path("volumeId") volumeId: String
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
        val q = query.lowercase().trim()
        if (q.isBlank()) return emptyList()

        val refinedQuery = "intitle:\"$q\" OR inauthor:\"$q\""
        com.booknook.app.util.Logger.d("GoogleBooks", "Search query: $refinedQuery (startIndex: $startIndex)")
        val res = api.search(refinedQuery, startIndex = startIndex, maxResults = MAX_RESULTS_PER_PAGE)
        val items = res.items ?: return emptyList()
        return items.map { dto -> dto.toDomainBook() }
    }

    suspend fun getBook(volumeId: String): Book? {
        if (volumeId.isBlank()) return null
        return api.getVolume(volumeId).toDomainBook()
    }
}

private fun BookDto.toDomainBook(): Book {
    return Book(
        id = id,
        title = volumeInfo.title ?: "",
        author = volumeInfo.authors?.joinToString(", ") ?: "",
        thumbnail = volumeInfo.imageLinks?.thumbnail?.replace("http://", "https://"),
        publishedDate = volumeInfo.publishedDate,
        genre = volumeInfo.categories?.joinToString(", "),
        pageCount = volumeInfo.pageCount,
        description = volumeInfo.description
    )
}
