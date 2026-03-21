package com.booknook.app.model.api.dto

data class VolumeInfoDto(
    val title: String?,
    val authors: List<String>?,
    val imageLinks: ImageLinksDto?,
    val publishedDate: String?,
    val categories: List<String>?,
    val pageCount: Int?,
    val description: String?
)

data class ImageLinksDto(
    val thumbnail: String?
)
