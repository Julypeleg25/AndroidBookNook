package com.colman.booknook.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.colman.booknook.data.local.entities.PostEntity

@Dao
interface PostDao {
    @Query("SELECT * FROM posts ORDER BY createdAt DESC")
    fun getAll(): LiveData<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE userId = :userId ORDER BY createdAt DESC")
    fun getPostsByUserId(userId: String): LiveData<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE id = :id")
    suspend fun getPostById(id: String): PostEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(post: PostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(posts: List<PostEntity>)

    @Query("DELETE FROM posts WHERE id = :id")
    suspend fun delete(id: String)
}
