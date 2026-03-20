package com.colman.booknook.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.colman.booknook.data.local.entities.WishlistEntity

@Dao
interface WishlistDao {
    @Query("SELECT * FROM wishlist WHERE userId = :userId ORDER BY addedAt DESC")
    fun getByUser(userId: String): LiveData<List<WishlistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(item: WishlistEntity)

    @Query("DELETE FROM wishlist WHERE key = :key")
    fun deleteByKey(key: String)
}
