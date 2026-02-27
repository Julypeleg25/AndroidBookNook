package com.colman.booknook.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.colman.booknook.data.local.entities.CachedBookEntity

@Dao
interface BookDao {
    @Query("SELECT * FROM cached_books WHERE id = :id")
    suspend fun getBook(id: String): CachedBookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(book: CachedBookEntity)
}
