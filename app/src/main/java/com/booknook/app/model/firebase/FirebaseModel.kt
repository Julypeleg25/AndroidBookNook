package com.booknook.app.model.firebase

import com.booknook.app.data.local.entities.CommentEntity
import com.booknook.app.data.local.entities.PostEntity
import com.booknook.app.data.local.entities.ReadlistEntity
import com.booknook.app.data.local.entities.SavedBookListItem
import com.booknook.app.data.local.entities.UserEntity
import com.booknook.app.data.local.entities.WishlistEntity
import com.booknook.app.util.Logger
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseModel {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val authUserId = MutableStateFlow(auth.currentUser?.uid)

    init {
        auth.addAuthStateListener { firebaseAuth ->
            authUserId.value = firebaseAuth.currentUser?.uid
        }
    }

    fun currentUserId(): String? = auth.currentUser?.uid
    fun requireUserId(): String = currentUserId() ?: throw IllegalStateException("Not logged in")
    fun observeAuthUserId(): StateFlow<String?> = authUserId

    fun logout() { auth.signOut() }

    suspend fun register(email: String, password: String, username: String): String {
        val res = auth.createUserWithEmailAndPassword(email, password).await()
        val user = res.user ?: throw IllegalStateException("Registration failed: No User")
        val uid = user.uid
        val profileUpdates = userProfileChangeRequest {
            displayName = username
        }
        user.updateProfile(profileUpdates).await()
        return uid
    }

    suspend fun createUserProfile(uid: String, username: String, email: String, avatarUrl: String?) {
        withTimeout(15000) {
            withContext(Dispatchers.IO) {
                db.collection("users").document(uid).set(mapOf(
                    "username" to username,
                    "email" to email,
                    "avatarUrl" to avatarUrl
                )).await()
            }
        }
    }

    suspend fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    suspend fun fetchProfile(): UserEntity? {
        val user = auth.currentUser ?: return null
        val uid = user.uid
        val doc = db.collection("users").document(uid).get().await()
        return UserEntity(
            id = uid,
            username = doc.getString("username")?.takeIf { it.isNotBlank() }
                ?: user.displayName
                ?: "User",
            email = doc.getString("email")?.takeIf { it.isNotBlank() }
                ?: user.email
                .orEmpty(),
            avatarUrl = doc.getString("avatarUrl")
        )
    }

    suspend fun updateProfile(username: String, email: String, avatarUrl: String?): UserEntity {
        val uid = requireUserId()
        val user = auth.currentUser ?: throw IllegalStateException("No auth user")
        if (user.displayName != username) {
            val profileUpdates = userProfileChangeRequest { displayName = username }
            user.updateProfile(profileUpdates).await()
        }
        val resolvedEmail = user.email ?: email
        val data = mapOf(
            "username" to username,
            "email" to resolvedEmail,
            "avatarUrl" to avatarUrl
        )
        db.collection("users").document(uid).set(data, SetOptions.merge()).await()

        runCatching {
            syncUserContentProfile(uid, username, avatarUrl)
        }.onFailure { error ->
            Logger.e(
                "Profile",
                "Profile saved, but syncing historical posts/comments failed. Keeping the profile update.",
                error
            )
        }

        return UserEntity(id = uid, username = username, email = resolvedEmail, avatarUrl = avatarUrl)
    }

    suspend fun fetchWishlist(userId: String): List<WishlistEntity> {
        return fetchSavedBooks(userId, WISHLIST_COLLECTION) { doc ->
            doc.toWishlistEntity(userId)
        }
    }

    suspend fun upsertWishlist(item: WishlistEntity) {
        upsertSavedBook(item.userId, WISHLIST_COLLECTION, item.bookId, item)
    }

    suspend fun deleteWishlist(userId: String, bookId: String) {
        deleteSavedBook(userId, WISHLIST_COLLECTION, bookId)
    }

    suspend fun fetchReadlist(userId: String): List<ReadlistEntity> {
        return fetchSavedBooks(userId, READLIST_COLLECTION) { doc ->
            doc.toReadlistEntity(userId)
        }
    }

    suspend fun upsertReadlist(item: ReadlistEntity) {
        upsertSavedBook(item.userId, READLIST_COLLECTION, item.bookId, item)
    }

    suspend fun deleteReadlist(userId: String, bookId: String) {
        deleteSavedBook(userId, READLIST_COLLECTION, bookId)
    }

    suspend fun updatePost(post: PostEntity) {
        val uid = requireUserId()
        if (post.userId != uid) throw IllegalStateException("Unauthorized")
        db.collection("posts").document(post.id).set(postToMap(post)).await()
    }

    suspend fun createPost(post: PostEntity) {
        db.collection("posts").document(post.id).set(postToMap(post)).await()
    }

    suspend fun deletePost(postId: String) {
        val post = getPost(postId) ?: return
        val uid = requireUserId()
        if (post.userId != uid) throw Exception("Unauthorized")
        db.collection("posts").document(postId).delete().await()
    }

    suspend fun getPost(postId: String): PostEntity? {
        val doc = db.collection("posts").document(postId).get().await()
        if (!doc.exists()) return null
        val uid = currentUserId()
        val isLiked = if (uid != null) {
            db.collection("posts").document(postId).collection("likes").document(uid).get().await().exists()
        } else false
        return documentToPostEntity(doc, isLiked)
    }

    suspend fun fetchPostsByUser(userId: String): List<PostEntity> {
        val currentUserId = currentUserId()
        val documents = fetchPostsByUserDocuments(userId)

        return documents.mapNotNull { doc ->
            val post = documentToPostEntity(doc, false) ?: return@mapNotNull null
            if (currentUserId != null && currentUserId != userId) {
                val isLiked = db.collection("posts").document(post.id)
                    .collection("likes").document(currentUserId)
                    .get()
                    .await()
                    .exists()
                post.copy(isLikedByUser = isLiked)
            } else {
                post
            }
        }.sortedByDescending { it.createdAt }
    }

    suspend fun fetchPostsPage(
        limit: Long,
        lastDocument: DocumentSnapshot?
    ): Pair<List<PostEntity>, DocumentSnapshot?> {
        var query = db.collection("posts")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit)
        if (lastDocument != null) {
            query = query.startAfter(lastDocument)
        }
        val snap = query.get().await()
        val newLastDoc = snap.documents.lastOrNull()
        val uid = currentUserId()
        val posts = snap.documents.mapNotNull { doc ->
            val post = documentToPostEntity(doc, false) ?: return@mapNotNull null
            if (uid != null) {
                val isLiked = db.collection("posts").document(post.id)
                    .collection("likes").document(uid).get().await().exists()
                post.copy(isLikedByUser = isLiked)
            } else {
                post
            }
        }
        return Pair(posts, newLastDoc)
    }

    suspend fun toggleLike(postId: String) {
        val uid = requireUserId()
        val likeRef = db.collection("posts").document(postId).collection("likes").document(uid)
        val likeDoc = likeRef.get().await()
        val postRef = db.collection("posts").document(postId)
        if (likeDoc.exists()) {
            db.runBatch { batch ->
                batch.delete(likeRef)
                batch.update(postRef, "likesCount", FieldValue.increment(-1))
            }.await()
        } else {
            db.runBatch { batch ->
                batch.set(likeRef, mapOf("createdAt" to System.currentTimeMillis()))
                batch.update(postRef, "likesCount", FieldValue.increment(1))
            }.await()
        }
    }

    suspend fun addComment(postId: String, text: String) {
        val uid = requireUserId()
        val profile = fetchProfile()
        val commentId = UUID.randomUUID().toString()
        val commentData = mapOf(
            "id" to commentId,
            "userId" to uid,
            "username" to (profile?.username ?: "User"),
            "userAvatarUrl" to profile?.avatarUrl,
            "text" to text,
            "createdAt" to System.currentTimeMillis()
        )
        db.collection("posts").document(postId)
            .collection("comments").document(commentId)
            .set(commentData).await()
        db.collection("posts").document(postId)
            .update("commentsCount", FieldValue.increment(1)).await()
    }

    suspend fun fetchComments(postId: String): List<CommentEntity> {
        val snap = db.collection("posts").document(postId)
            .collection("comments").orderBy("createdAt").get().await()
        val authorProfiles = fetchCommentAuthorProfiles(
            snap.documents.mapNotNull { document ->
                document.getString("userId")?.takeIf { it.isNotBlank() }
            }.distinct()
        )

        return snap.documents.mapNotNull { doc ->
            val commentUserId = doc.getString("userId") ?: ""
            val authorProfile = authorProfiles[commentUserId]
            CommentEntity(
                id = doc.getString("id") ?: "",
                postId = postId,
                userId = commentUserId,
                username = authorProfile?.username ?: doc.getString("username") ?: "User",
                text = doc.getString("text") ?: "",
                userAvatarUrl = authorProfile?.avatarUrl ?: doc.getString("userAvatarUrl"),
                createdAt = doc.getLong("createdAt") ?: 0L
            )
        }
    }

    private suspend fun fetchCommentAuthorProfiles(userIds: List<String>): Map<String, CommentAuthorProfile> {
        if (userIds.isEmpty()) {
            return emptyMap()
        }

        val authUser = auth.currentUser
        val profiles = mutableMapOf<String, CommentAuthorProfile>()

        userIds.chunked(USER_QUERY_CHUNK_SIZE).forEach { chunk ->
            val documents = db.collection("users")
                .whereIn(FieldPath.documentId(), chunk)
                .get()
                .await()
                .documents

            documents.forEach { document ->
                val userId = document.id
                profiles[userId] = CommentAuthorProfile(
                    username = document.getString("username")?.takeIf { it.isNotBlank() },
                    avatarUrl = document.getString("avatarUrl")
                )
            }
        }

        val currentUserId = authUser?.uid
        if (currentUserId != null && userIds.contains(currentUserId)) {
            val currentProfile = profiles[currentUserId]
            profiles[currentUserId] = CommentAuthorProfile(
                username = currentProfile?.username ?: authUser.displayName,
                avatarUrl = currentProfile?.avatarUrl
            )
        }

        return profiles
    }

    private fun postToMap(post: PostEntity): Map<String, Any?> = mapOf(
        "id" to post.id,
        "userId" to post.userId,
        "username" to post.username,
        "bookId" to post.bookId,
        "bookTitle" to post.bookTitle,
        "bookAuthor" to post.bookAuthor,
        "bookThumbnail" to post.bookThumbnail,
        "rating" to post.rating,
        "review" to post.review,
        "imageUrl" to post.imageUrl,
        "createdAt" to post.createdAt,
        "likesCount" to post.likesCount,
        "commentsCount" to post.commentsCount,
        "bookPublishedDate" to post.bookPublishedDate,
        "bookGenre" to post.bookGenre,
        "bookPageCount" to post.bookPageCount,
        "bookDescription" to post.bookDescription
    )

    private suspend fun <T> fetchSavedBooks(
        userId: String,
        collectionName: String,
        mapper: (DocumentSnapshot) -> T?
    ): List<T> {
        requireAuthorizedUser(userId)
        val snap = db.collection("users")
            .document(userId)
            .collection(collectionName)
            .orderBy("addedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .await()
        return snap.documents.mapNotNull(mapper)
    }

    private suspend fun fetchPostsByUserDocuments(userId: String): List<DocumentSnapshot> {
        return try {
            db.collection("posts")
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
                .documents
        } catch (e: FirebaseFirestoreException) {
            val requiresIndex = e.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION &&
                e.message?.contains("requires an index", ignoreCase = true) == true
            if (!requiresIndex) {
                throw e
            }

            Logger.e(
                "Firestore",
                "Missing composite index for posts-by-user query. Falling back to client-side sorting.",
                e
            )

            db.collection("posts")
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .documents
        }
    }

    private suspend fun syncUserContentProfile(userId: String, username: String, avatarUrl: String?) {
        val postDocuments = db.collection("posts")
            .whereEqualTo("userId", userId)
            .get()
            .await()
            .documents
        commitBatchedUpdates(postDocuments) { batch, document ->
            batch.update(document.reference, "username", username)
        }

        val commentDocuments = db.collectionGroup("comments")
            .whereEqualTo("userId", userId)
            .get()
            .await()
            .documents
        commitBatchedUpdates(commentDocuments) { batch, document ->
            batch.update(document.reference, "username", username)
            if (avatarUrl.isNullOrBlank()) {
                batch.update(document.reference, "userAvatarUrl", FieldValue.delete())
            } else {
                batch.update(document.reference, "userAvatarUrl", avatarUrl)
            }
        }
    }

    private suspend fun commitBatchedUpdates(
        documents: List<DocumentSnapshot>,
        applyUpdate: (WriteBatch, DocumentSnapshot) -> Unit
    ) {
        documents.chunked(MAX_BATCH_WRITE_SIZE).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { document ->
                applyUpdate(batch, document)
            }
            batch.commit().await()
        }
    }

    private suspend fun upsertSavedBook(
        userId: String,
        collectionName: String,
        documentId: String,
        item: SavedBookListItem
    ) {
        requireAuthorizedUser(userId)
        db.collection("users")
            .document(userId)
            .collection(collectionName)
            .document(documentId)
            .set(savedBookToMap(item))
            .await()
    }

    private suspend fun deleteSavedBook(
        userId: String,
        collectionName: String,
        bookId: String
    ) {
        requireAuthorizedUser(userId)
        db.collection("users")
            .document(userId)
            .collection(collectionName)
            .document(bookId)
            .delete()
            .await()
    }

    private fun savedBookToMap(item: SavedBookListItem): Map<String, Any?> = mapOf(
        "key" to item.key,
        "userId" to item.userId,
        "bookId" to item.bookId,
        "title" to item.title,
        "author" to item.author,
        "thumbnail" to item.thumbnail,
        "addedAt" to item.addedAt,
        "genre" to item.genre,
        "publishedDate" to item.publishedDate,
        "pageCount" to item.pageCount,
        "description" to item.description
    )

    private fun DocumentSnapshot.toWishlistEntity(userId: String): WishlistEntity? {
        val bookId = getString("bookId") ?: id.takeIf { it.isNotBlank() } ?: return null
        return WishlistEntity(
            key = getString("key") ?: "$userId|$bookId",
            userId = getString("userId") ?: userId,
            bookId = bookId,
            title = getString("title") ?: "",
            author = getString("author") ?: "",
            thumbnail = getString("thumbnail"),
            addedAt = getLong("addedAt") ?: 0L,
            genre = getString("genre"),
            publishedDate = getString("publishedDate"),
            pageCount = getLong("pageCount")?.toInt(),
            description = getString("description")
        )
    }

    private fun DocumentSnapshot.toReadlistEntity(userId: String): ReadlistEntity? {
        val bookId = getString("bookId") ?: id.takeIf { it.isNotBlank() } ?: return null
        return ReadlistEntity(
            key = getString("key") ?: "$userId|$bookId",
            userId = getString("userId") ?: userId,
            bookId = bookId,
            title = getString("title") ?: "",
            author = getString("author") ?: "",
            thumbnail = getString("thumbnail"),
            addedAt = getLong("addedAt") ?: 0L,
            genre = getString("genre"),
            publishedDate = getString("publishedDate"),
            pageCount = getLong("pageCount")?.toInt(),
            description = getString("description")
        )
    }

    private fun requireAuthorizedUser(userId: String) {
        if (requireUserId() != userId) {
            throw IllegalStateException("Unauthorized")
        }
    }

    private fun documentToPostEntity(doc: DocumentSnapshot, isLiked: Boolean): PostEntity? {
        val id = doc.getString("id") ?: return null
        return PostEntity(
            id = id,
            userId = doc.getString("userId") ?: "",
            username = doc.getString("username") ?: "",
            bookId = doc.getString("bookId") ?: "",
            bookTitle = doc.getString("bookTitle") ?: "",
            bookAuthor = doc.getString("bookAuthor") ?: "",
            bookThumbnail = doc.getString("bookThumbnail"),
            rating = (doc.getLong("rating") ?: 0).toInt(),
            review = doc.getString("review") ?: "",
            imageUrl = doc.getString("imageUrl"),
            createdAt = doc.getLong("createdAt") ?: 0L,
            likesCount = (doc.getLong("likesCount") ?: 0).toInt(),
            commentsCount = (doc.getLong("commentsCount") ?: 0).toInt(),
            bookPublishedDate = doc.getString("bookPublishedDate"),
            bookGenre = doc.getString("bookGenre"),
            bookPageCount = doc.getLong("bookPageCount")?.toInt(),
            bookDescription = doc.getString("bookDescription"),
            isLikedByUser = isLiked
        )
    }

    companion object {
        private const val MAX_BATCH_WRITE_SIZE = 400
        private const val USER_QUERY_CHUNK_SIZE = 10
        private const val WISHLIST_COLLECTION = "wishlist"
        private const val READLIST_COLLECTION = "readlist"
    }
}

private data class CommentAuthorProfile(
    val username: String?,
    val avatarUrl: String?
)
