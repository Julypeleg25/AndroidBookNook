package com.booknook.app.model.firebase

import com.booknook.app.data.local.entities.PostEntity
import com.booknook.app.data.local.entities.ReadlistEntity
import com.booknook.app.data.local.entities.SavedBookListItem
import com.booknook.app.data.local.entities.WishlistEntity
import com.google.firebase.firestore.DocumentSnapshot

internal fun PostEntity.toFirestorePostMap(): Map<String, Any?> = mapOf(
    FirebaseFields.ID to id,
    FirebaseFields.USER_ID to userId,
    FirebaseFields.USERNAME to username,
    FirebaseFields.BOOK_ID to bookId,
    FirebaseFields.BOOK_TITLE to bookTitle,
    FirebaseFields.BOOK_AUTHOR to bookAuthor,
    FirebaseFields.BOOK_THUMBNAIL to bookThumbnail,
    FirebaseFields.RATING to rating,
    FirebaseFields.REVIEW to review,
    FirebaseFields.IMAGE_URL to imageUrl,
    FirebaseFields.CREATED_AT to createdAt,
    FirebaseFields.LIKES_COUNT to likesCount,
    FirebaseFields.COMMENTS_COUNT to commentsCount,
    FirebaseFields.BOOK_PUBLISHED_DATE to bookPublishedDate,
    FirebaseFields.BOOK_GENRE to bookGenre,
    FirebaseFields.BOOK_PAGE_COUNT to bookPageCount,
    FirebaseFields.BOOK_DESCRIPTION to bookDescription
)

internal fun DocumentSnapshot.toPostEntity(isLiked: Boolean): PostEntity? {
    val id = getString(FirebaseFields.ID) ?: return null
    return PostEntity(
        id = id,
        userId = getString(FirebaseFields.USER_ID).orEmpty(),
        username = getString(FirebaseFields.USERNAME).orEmpty(),
        bookId = getString(FirebaseFields.BOOK_ID).orEmpty(),
        bookTitle = getString(FirebaseFields.BOOK_TITLE).orEmpty(),
        bookAuthor = getString(FirebaseFields.BOOK_AUTHOR).orEmpty(),
        bookThumbnail = getString(FirebaseFields.BOOK_THUMBNAIL),
        rating = (getLong(FirebaseFields.RATING) ?: 0L).toInt(),
        review = getString(FirebaseFields.REVIEW).orEmpty(),
        imageUrl = getString(FirebaseFields.IMAGE_URL),
        createdAt = getLong(FirebaseFields.CREATED_AT) ?: 0L,
        likesCount = (getLong(FirebaseFields.LIKES_COUNT) ?: 0L).toInt(),
        commentsCount = (getLong(FirebaseFields.COMMENTS_COUNT) ?: 0L).toInt(),
        bookPublishedDate = getString(FirebaseFields.BOOK_PUBLISHED_DATE),
        bookGenre = getString(FirebaseFields.BOOK_GENRE),
        bookPageCount = getLong(FirebaseFields.BOOK_PAGE_COUNT)?.toInt(),
        bookDescription = getString(FirebaseFields.BOOK_DESCRIPTION),
        isLikedByUser = isLiked
    )
}

internal fun SavedBookListItem.toFirestoreSavedBookMap(): Map<String, Any?> = mapOf(
    FirebaseFields.KEY to key,
    FirebaseFields.USER_ID to userId,
    FirebaseFields.BOOK_ID to bookId,
    FirebaseFields.TITLE to title,
    FirebaseFields.AUTHOR to author,
    FirebaseFields.THUMBNAIL to thumbnail,
    FirebaseFields.ADDED_AT to addedAt,
    FirebaseFields.GENRE to genre,
    FirebaseFields.PUBLISHED_DATE to publishedDate,
    FirebaseFields.PAGE_COUNT to pageCount,
    FirebaseFields.DESCRIPTION to description
)

internal fun DocumentSnapshot.toWishlistEntity(userId: String): WishlistEntity? {
    val bookId = getString(FirebaseFields.BOOK_ID) ?: id.takeIf { it.isNotBlank() } ?: return null
    return WishlistEntity(
        key = getString(FirebaseFields.KEY) ?: savedBookKey(userId, bookId),
        userId = getString(FirebaseFields.USER_ID) ?: userId,
        bookId = bookId,
        title = getString(FirebaseFields.TITLE).orEmpty(),
        author = getString(FirebaseFields.AUTHOR).orEmpty(),
        thumbnail = getString(FirebaseFields.THUMBNAIL),
        addedAt = getLong(FirebaseFields.ADDED_AT) ?: 0L,
        genre = getString(FirebaseFields.GENRE),
        publishedDate = getString(FirebaseFields.PUBLISHED_DATE),
        pageCount = getLong(FirebaseFields.PAGE_COUNT)?.toInt(),
        description = getString(FirebaseFields.DESCRIPTION)
    )
}

internal fun DocumentSnapshot.toReadlistEntity(userId: String): ReadlistEntity? {
    val bookId = getString(FirebaseFields.BOOK_ID) ?: id.takeIf { it.isNotBlank() } ?: return null
    return ReadlistEntity(
        key = getString(FirebaseFields.KEY) ?: savedBookKey(userId, bookId),
        userId = getString(FirebaseFields.USER_ID) ?: userId,
        bookId = bookId,
        title = getString(FirebaseFields.TITLE).orEmpty(),
        author = getString(FirebaseFields.AUTHOR).orEmpty(),
        thumbnail = getString(FirebaseFields.THUMBNAIL),
        addedAt = getLong(FirebaseFields.ADDED_AT) ?: 0L,
        genre = getString(FirebaseFields.GENRE),
        publishedDate = getString(FirebaseFields.PUBLISHED_DATE),
        pageCount = getLong(FirebaseFields.PAGE_COUNT)?.toInt(),
        description = getString(FirebaseFields.DESCRIPTION)
    )
}

internal fun savedBookKey(userId: String, bookId: String): String = "$userId|$bookId"
