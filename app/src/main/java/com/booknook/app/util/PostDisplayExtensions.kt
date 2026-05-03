package com.booknook.app.util

import com.booknook.app.data.local.entities.PostEntity

private const val MAX_GENRES_TO_SHOW = 2

val PostEntity.displayPostImageUrl: String?
    get() = imageUrl.nullIfBlank()

fun String?.toShortGenreList(maxGenres: Int = MAX_GENRES_TO_SHOW): String? {
    val genres = this
        ?.split(",", "/", "|")
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        .orEmpty()

    if (genres.isEmpty()) return null

    val visibleGenres = genres.take(maxGenres).joinToString(", ")
    return if (genres.size > maxGenres) "$visibleGenres..." else visibleGenres
}
