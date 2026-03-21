package com.booknook.app.util

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import java.io.IOException
import java.net.UnknownHostException

fun Throwable?.toUserFriendlyMessage(): String {
    return when (this) {
        is UnknownHostException, is FirebaseNetworkException, is IOException -> 
            "It looks like you're offline. Please check your internet connection."
        
        is FirebaseAuthInvalidUserException -> 
            "We couldn't find an account with this email."
            
        is FirebaseAuthInvalidCredentialsException -> 
            "The password you entered is incorrect. Please try again."
            
        is FirebaseAuthUserCollisionException -> 
            "An account already exists with this email address."
            
        else -> this?.message?.toUserFriendlyMessage() ?: "Something went wrong on our end. Please try again in a moment."
    }
}

fun String?.toUserFriendlyMessage(): String {
    if (this == null) return "Something went wrong. Please try again."
    
    val lower = this.lowercase()
    return when {
        lower.contains("network") || lower.contains("timeout") || lower.contains("connection") ->
            "Having trouble connecting. Please check your internet and try again."
            
        lower.contains("password") && (lower.contains("wrong") || lower.contains("invalid")) ->
            "Oops! That password doesn't look right."
            
        lower.contains("user not found") || lower.contains("no user") ->
            "We couldn't find that account. Want to sign up instead?"
            
        lower.contains("already in use") || lower.contains("collision") ->
            "This email is already part of the BookNook family! Try logging in."
            
        lower.contains("empty") || lower.contains("required") ->
            "Please fill in all the details so we can continue."
            
        lower.contains("permission") || lower.contains("denied") ->
            "You don't have permission to do that."
            
        lower.contains("rating") && lower.contains("required") ->
            "Please give the book a rating and a quick review!"
            
        lower.contains("photo") && lower.contains("required") ->
            "A picture is worth a thousand words! Please add a photo."

        else -> "Something went wrong. We're looking into it!"
    }
}
