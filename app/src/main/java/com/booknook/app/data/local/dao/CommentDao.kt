package com.booknook.app.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.booknook.app.data.local.entities.CommentEntity

@Dao
interface CommentDao {
    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY createdAt ASC")
    fun observeByPost(postId: String): LiveData<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(comment: CommentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(comments: List<CommentEntity>)

    @Query("DELETE FROM comments WHERE postId = :postId")
    suspend fun deleteByPost(postId: String)
}
