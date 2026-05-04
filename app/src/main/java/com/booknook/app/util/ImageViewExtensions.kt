package com.booknook.app.util

import android.net.Uri
import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.squareup.picasso.Picasso

fun String?.nullIfBlank(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

fun ImageView.loadRemoteImage(
    url: String?,
    @DrawableRes placeholderResId: Int,
    fit: Boolean = true,
    centerCrop: Boolean = true
) {
    val normalizedUrl = url.nullIfBlank()
    if (normalizedUrl == null) {
        Picasso.get().cancelRequest(this)
        setImageResource(placeholderResId)
        return
    }

    val request = Picasso.get()
        .load(normalizedUrl)
        .placeholder(placeholderResId)
        .error(placeholderResId)

    if (fit) {
        request.fit()
    }
    if (centerCrop) {
        request.centerCrop()
    }

    request.into(this)
}

fun ImageView.loadLocalImage(
    uri: Uri?,
    @DrawableRes placeholderResId: Int,
    fit: Boolean = true,
    centerCrop: Boolean = true
) {
    if (uri == null) {
        Picasso.get().cancelRequest(this)
        setImageResource(placeholderResId)
        return
    }

    val request = Picasso.get()
        .load(uri)
        .placeholder(placeholderResId)
        .error(placeholderResId)

    if (fit) {
        request.fit()
    }
    if (centerCrop) {
        request.centerCrop()
    }

    request.into(this)
}
