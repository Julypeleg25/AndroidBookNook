package com.booknook.app.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.booknook.app.data.local.entities.CommentEntity

@Dao
interface CommentDao {
    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY createdAt DESC")
    fun observeByPost(postId: String): LiveData<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(comment: CommentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(comments: List<CommentEntity>)

    @Query("DELETE FROM comments WHERE postId = :postId")
    suspend fun deleteByPost(postId: String)

    @Query("UPDATE comments SET username = :username, userAvatarUrl = :avatarUrl WHERE userId = :userId")
    suspend fun updateAuthorProfile(userId: String, username: String, avatarUrl: String?)

    @Query("DELETE FROM comments")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceByPost(postId: String, comments: List<CommentEntity>) {
        deleteByPost(postId)
        if (comments.isNotEmpty()) {
            insertAll(comments)
        }
    }
}
