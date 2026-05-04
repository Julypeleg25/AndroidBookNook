package com.booknook.app.model.firebase

import com.booknook.app.data.local.entities.ReadlistEntity
import com.booknook.app.data.local.entities.SavedBookListItem
import com.booknook.app.data.local.entities.WishlistEntity
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class FirebaseListsModel(
    private val db: FirebaseFirestore,
    private val authModel: FirebaseAuthModel
) {
    suspend fun fetchWishlist(userId: String): List<WishlistEntity> {
        return fetchSavedBooks(userId, FirebaseCollections.WISHLIST) { doc ->
            doc.toWishlistEntity(userId)
        }
    }

    suspend fun upsertWishlist(item: WishlistEntity) {
        upsertSavedBook(item.userId, FirebaseCollections.WISHLIST, item.bookId, item)
    }

    suspend fun deleteWishlist(userId: String, bookId: String) {
        deleteSavedBook(userId, FirebaseCollections.WISHLIST, bookId)
    }

    suspend fun fetchReadlist(userId: String): List<ReadlistEntity> {
        return fetchSavedBooks(userId, FirebaseCollections.READLIST) { doc ->
            doc.toReadlistEntity(userId)
        }
    }

    suspend fun upsertReadlist(item: ReadlistEntity) {
        upsertSavedBook(item.userId, FirebaseCollections.READLIST, item.bookId, item)
    }

    suspend fun deleteReadlist(userId: String, bookId: String) {
        deleteSavedBook(userId, FirebaseCollections.READLIST, bookId)
    }

    private suspend fun <T> fetchSavedBooks(
        userId: String,
        collectionName: String,
        mapper: (DocumentSnapshot) -> T?
    ): List<T> {
        requireAuthorizedUser(userId)
        val snap = usersCollection()
            .document(userId)
            .collection(collectionName)
            .orderBy(FirebaseFields.ADDED_AT, Query.Direction.DESCENDING)
            .get()
            .await()
        return snap.documents.mapNotNull(mapper)
    }

    private suspend fun upsertSavedBook(
        userId: String,
        collectionName: String,
        documentId: String,
        item: SavedBookListItem
    ) {
        requireAuthorizedUser(userId)
        usersCollection()
            .document(userId)
            .collection(collectionName)
            .document(documentId)
            .set(item.toFirestoreSavedBookMap())
            .await()
    }

    private suspend fun deleteSavedBook(userId: String, collectionName: String, bookId: String) {
        requireAuthorizedUser(userId)
        usersCollection()
            .document(userId)
            .collection(collectionName)
            .document(bookId)
            .delete()
            .await()
    }

    private fun requireAuthorizedUser(userId: String) {
        if (authModel.requireUserId() != userId) {
            throw IllegalStateException("Unauthorized")
        }
    }

    private fun usersCollection() = db.collection(FirebaseCollections.USERS)
}
