package com.booknook.app.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.booknook.app.data.local.entities.WishlistEntity

@Dao
interface WishlistDao {
    @Query("SELECT * FROM wishlist WHERE userId = :userId ORDER BY addedAt DESC")
    fun getByUser(userId: String): LiveData<List<WishlistEntity>>

    @Query("SELECT * FROM wishlist WHERE userId = :userId ORDER BY addedAt DESC")
    suspend fun getByUserSync(userId: String): List<WishlistEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: WishlistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<WishlistEntity>)

    @Query("SELECT COUNT(*) > 0 FROM wishlist WHERE key = :key")
    fun observeExistsByKey(key: String): LiveData<Boolean>

    @Query("SELECT COUNT(*) > 0 FROM wishlist WHERE key = :key")
    suspend fun existsByKey(key: String): Boolean

    @Query("SELECT * FROM wishlist WHERE key = :key LIMIT 1")
    suspend fun getByKey(key: String): WishlistEntity?

    @Query("DELETE FROM wishlist WHERE key = :key")
    suspend fun deleteByKey(key: String)

    @Query("DELETE FROM wishlist WHERE userId = :userId")
    suspend fun deleteByUser(userId: String)

    @Transaction
    suspend fun replaceForUser(userId: String, items: List<WishlistEntity>) {
        deleteByUser(userId)
        if (items.isNotEmpty()) {
            upsertAll(items)
        }
    }

    @Query("DELETE FROM wishlist")
    suspend fun deleteAll()
}
