package com.booknook.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.booknook.app.data.local.entities.LikeEntity

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

    @Query("DELETE FROM likes WHERE postId = :postId")
    suspend fun deleteByPost(postId: String)

    @Query("DELETE FROM likes")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(likes: List<LikeEntity>)

    @Transaction
    suspend fun replaceUserLikes(userId: String, likes: List<LikeEntity>) {
        clearUserLikes(userId)
        if (likes.isNotEmpty()) {
            insertAll(likes)
        }
    }
}
