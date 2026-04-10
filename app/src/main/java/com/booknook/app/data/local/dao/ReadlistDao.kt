package com.booknook.app.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.booknook.app.data.local.entities.ReadlistEntity

@Dao
interface ReadlistDao {
    @Query("SELECT * FROM readlist WHERE userId = :userId ORDER BY addedAt DESC")
    fun getByUser(userId: String): LiveData<List<ReadlistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ReadlistEntity)

    @Query("SELECT COUNT(*) > 0 FROM readlist WHERE key = :key")
    fun observeExistsByKey(key: String): LiveData<Boolean>

    @Query("SELECT COUNT(*) > 0 FROM readlist WHERE key = :key")
    suspend fun existsByKey(key: String): Boolean

    @Query("DELETE FROM readlist WHERE key = :key")
    suspend fun deleteByKey(key: String)

    @Query("DELETE FROM readlist")
    suspend fun deleteAll()
}
