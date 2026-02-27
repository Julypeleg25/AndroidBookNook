package com.colman.booknook.data.local.dao

import androidx.room.*
import com.colman.booknook.data.local.entities.CachedBookEntity

@Dao
interface CachedBookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<CachedBookEntity>)

    @Query("DELETE FROM cached_books WHERE lastFetchedAt < :minTime")
    suspend fun deleteOlderThan(minTime: Long)
}
