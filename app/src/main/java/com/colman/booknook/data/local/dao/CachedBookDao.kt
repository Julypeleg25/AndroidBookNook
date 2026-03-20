package com.booknook.app.data.local.dao

import androidx.room.*
import com.booknook.app.data.local.entities.CachedBookEntity

@Dao
interface CachedBookDao {
    @Upsert
    fun upsertAll(items: List<CachedBookEntity>)

    @Query("DELETE FROM cached_books WHERE lastFetchedAt < :minTime")
    fun deleteOlderThan(minTime: Long)
}
