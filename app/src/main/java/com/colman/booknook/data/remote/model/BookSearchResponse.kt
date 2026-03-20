package com.colman.booknook.data.remote.model

data class BookSearchResponse(
    val kind: String,
    val totalItems: Int,
    val items: List<BookItem>?
)

data class BookItem(
    val id: String,
    val volumeInfo: VolumeInfo
)

data class VolumeInfo(
    val title: String,
    val authors: List<String>?,
    val description: String?,
    val imageLinks: ImageLinks?,
    val pageCount: Int?,
    val averageRating: Float?
)

data class ImageLinks(
    val smallThumbnail: String?,
    val thumbnail: String?
)
