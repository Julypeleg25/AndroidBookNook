package com.booknook.app.model.firebase

internal object FirebaseCollections {
    const val USERS = "users"
    const val POSTS = "posts"
    const val LIKES = "likes"
    const val COMMENTS = "comments"
    const val WISHLIST = "wishlist"
    const val READLIST = "readlist"
}

internal object FirebaseFields {
    const val ID = "id"
    const val USER_ID = "userId"
    const val USERNAME = "username"
    const val EMAIL = "email"
    const val AVATAR_URL = "avatarUrl"
    const val USER_AVATAR_URL = "userAvatarUrl"
    const val TEXT = "text"
    const val CREATED_AT = "createdAt"
    const val ADDED_AT = "addedAt"
    const val KEY = "key"
    const val BOOK_ID = "bookId"
    const val TITLE = "title"
    const val AUTHOR = "author"
    const val THUMBNAIL = "thumbnail"
    const val BOOK_TITLE = "bookTitle"
    const val BOOK_AUTHOR = "bookAuthor"
    const val BOOK_THUMBNAIL = "bookThumbnail"
    const val RATING = "rating"
    const val REVIEW = "review"
    const val IMAGE_URL = "imageUrl"
    const val LIKES_COUNT = "likesCount"
    const val COMMENTS_COUNT = "commentsCount"
    const val BOOK_PUBLISHED_DATE = "bookPublishedDate"
    const val BOOK_GENRE = "bookGenre"
    const val BOOK_PAGE_COUNT = "bookPageCount"
    const val BOOK_DESCRIPTION = "bookDescription"
    const val GENRE = "genre"
    const val PUBLISHED_DATE = "publishedDate"
    const val PAGE_COUNT = "pageCount"
    const val DESCRIPTION = "description"
}

internal object FirebaseDefaults {
    const val USERNAME = "User"
}

internal object FirebaseLimits {
    const val MAX_BATCH_WRITE_SIZE = 400
    const val USER_QUERY_CHUNK_SIZE = 10
    const val PROFILE_CREATE_TIMEOUT_MS = 15_000L
}
