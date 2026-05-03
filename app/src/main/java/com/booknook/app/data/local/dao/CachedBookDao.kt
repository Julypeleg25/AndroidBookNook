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

    @Query(
        """
        SELECT * FROM cached_books
        WHERE title LIKE '%' || :query || '%'
           OR author LIKE '%' || :query || '%'
        ORDER BY lastFetchedAt DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun search(query: String, limit: Int, offset: Int): List<CachedBookEntity>

    @Query("SELECT * FROM cached_books WHERE id = :bookId LIMIT 1")
    suspend fun getById(bookId: String): CachedBookEntity?

    @Query("DELETE FROM cached_books WHERE lastFetchedAt < :minTime")
    suspend fun deleteOlderThan(minTime: Long)

    @Query("DELETE FROM cached_books")
    suspend fun deleteAll()
}
