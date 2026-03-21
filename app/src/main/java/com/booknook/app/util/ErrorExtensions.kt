package com.booknook.app.util

import com.booknook.app.R
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import java.io.IOException
import java.net.UnknownHostException

fun Throwable?.toUserFriendlyMessageRes(defaultResId: Int = R.string.error_generic): Int {
    return when (this) {
        is UnknownHostException, is FirebaseNetworkException, is IOException ->
            R.string.error_offline

        is FirebaseAuthInvalidUserException ->
            R.string.error_auth_user_not_found

        is FirebaseAuthInvalidCredentialsException ->
            R.string.error_auth_wrong_password

        is FirebaseAuthUserCollisionException ->
            R.string.error_auth_email_exists

        else -> this?.message?.toUserFriendlyMessageRes(defaultResId)
            ?: R.string.error_generic_retry
    }
}

fun String?.toUserFriendlyMessageRes(defaultResId: Int = R.string.error_generic): Int {
    if (this == null) return defaultResId

    val lower = this.lowercase()
    return when {
        lower.contains("network") || lower.contains("timeout") || lower.contains("connection") ->
            R.string.error_connection

        lower.contains("password") && (lower.contains("wrong") || lower.contains("invalid")) ->
            R.string.error_auth_wrong_password

        lower.contains("invalid_login_credentials") ->
            R.string.error_auth_invalid_credentials

        lower.contains("user not found") || lower.contains("no user") ->
            R.string.error_auth_user_not_found

        lower.contains("already in use") || lower.contains("collision") ->
            R.string.error_auth_email_exists

        lower.contains("badly formatted") ->
            R.string.error_auth_invalid_email

        lower.contains("at least 6 characters") ->
            R.string.error_auth_password_short

        lower.contains("empty") || lower.contains("required") ->
            R.string.error_fill_all_details

        lower.contains("permission") || lower.contains("denied") ->
            R.string.error_permission

        lower.contains("api has not been used") || lower.contains("firestore api") ->
            R.string.error_firestore_api

        lower.contains("rating") && lower.contains("required") ->
            R.string.error_rating_required

        lower.contains("photo") && lower.contains("required") ->
            R.string.error_photo_required

        lower.contains("comment") && lower.contains("empty") ->
            R.string.error_comment_required

        else -> defaultResId
    }
}
