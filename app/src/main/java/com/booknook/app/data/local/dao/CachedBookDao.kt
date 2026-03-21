package com.booknook.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.booknook.app.data.local.entities.CachedBookEntity

@Dao
interface CachedBookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<CachedBookEntity>)

    @Query("DELETE FROM cached_books WHERE lastFetchedAt < :minTime")
    suspend fun deleteOlderThan(minTime: Long)
}
