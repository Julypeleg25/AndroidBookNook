package com.booknook.app.data.local.entities

interface SavedBookListItem {
    val key: String
    val userId: String
    val bookId: String
    val title: String
    val author: String
    val thumbnail: String?
    val addedAt: Long
    val genre: String?
    val publishedDate: String?
    val pageCount: Int?
    val description: String?
}
