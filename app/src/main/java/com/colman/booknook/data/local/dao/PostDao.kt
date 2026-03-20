package com.booknook.app.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.booknook.app.data.local.entities.PostEntity

@Dao
interface PostDao {
    @Query("SELECT * FROM posts ORDER BY createdAt DESC")
    fun getAll(): LiveData<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE userId = :userId ORDER BY createdAt DESC")
    fun getByUser(userId: String): LiveData<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE id = :postId LIMIT 1")
    fun getById(postId: String): LiveData<PostEntity?>

    @Query("""SELECT * FROM posts
        WHERE (:title IS NULL OR bookTitle LIKE '%' || :title || '%')
          AND (:author IS NULL OR bookAuthor LIKE '%' || :author || '%')
          AND (:minRating IS NULL OR rating >= :minRating)
          AND (:minComments IS NULL OR commentsCount >= :minComments)
        ORDER BY createdAt DESC""")
    fun search(title: String?, author: String?, minRating: Int?, minComments: Int?): LiveData<List<PostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(post: PostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertAll(posts: List<PostEntity>)

    @Query("DELETE FROM posts WHERE id = :postId")
    fun deleteById(postId: String)
}
