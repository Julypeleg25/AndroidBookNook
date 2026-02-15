package com.colman.booknook.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.colman.booknook.data.local.entities.WishlistEntity

@Dao
interface WishlistDao {
    @Query("SELECT * FROM wishlist WHERE userId = :userId ORDER BY addedAt DESC")
    fun getWishlist(userId: String): LiveData<List<WishlistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: WishlistEntity)

    @Query("DELETE FROM wishlist WHERE bookId = :bookId AND userId = :userId")
    suspend fun delete(bookId: String, userId: String)

    @Query("SELECT EXISTS(SELECT * FROM wishlist WHERE bookId = :bookId AND userId = :userId)")
    fun isInWishlist(bookId: String, userId: String): LiveData<Boolean>
}
