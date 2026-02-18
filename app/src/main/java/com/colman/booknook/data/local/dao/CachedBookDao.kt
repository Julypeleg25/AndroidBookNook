package com.booknook.app.data.local.dao

import androidx.room.*
import com.booknook.app.data.local.entities.CachedBookEntity

@Dao
interface CachedBookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertAll(items: List<CachedBookEntity>)

    @Query("DELETE FROM cached_books WHERE lastFetchedAt < :minTime")
    fun deleteOlderThan(minTime: Long)
}
