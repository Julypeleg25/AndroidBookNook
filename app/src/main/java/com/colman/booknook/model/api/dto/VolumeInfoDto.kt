package com.colman.booknook.model.api.dto

data class VolumeInfoDto(
    val title: String?,
    val authors: List<String>?,
    val imageLinks: ImageLinksDto?
)

data class ImageLinksDto(
    val thumbnail: String?
)
