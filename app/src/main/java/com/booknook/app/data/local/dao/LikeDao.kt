package com.booknook.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LikeDao {
    @Query("SELECT EXISTS(SELECT 1 FROM likes WHERE userId = :userId AND postId = :postId)")
    suspend fun exists(userId: String, postId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(like: com.booknook.app.data.local.entities.LikeEntity)

    @Query("DELETE FROM likes WHERE userId = :userId AND postId = :postId")
    suspend fun delete(userId: String, postId: String)

    @Query("DELETE FROM likes WHERE userId = :userId")
    suspend fun clearUserLikes(userId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(likes: List<com.booknook.app.data.local.entities.LikeEntity>)
}
