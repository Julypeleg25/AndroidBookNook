package com.booknook.app.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.booknook.app.data.local.entities.PostEntity

@Dao
interface PostDao {
    @Query("""SELECT *, 
        (SELECT EXISTS(SELECT 1 FROM likes WHERE userId = :currUid AND postId = posts.id)) as isLikedByUser 
        FROM posts ORDER BY createdAt DESC""")
    fun getAll(currUid: String): LiveData<List<PostEntity>>

    @Query("""SELECT *, 
        (SELECT EXISTS(SELECT 1 FROM likes WHERE userId = :currUid AND postId = posts.id)) as isLikedByUser 
        FROM posts WHERE userId = :userId ORDER BY createdAt DESC""")
    fun getByUser(userId: String, currUid: String): LiveData<List<PostEntity>>

    @Query("""SELECT *, 
        (SELECT EXISTS(SELECT 1 FROM likes WHERE userId = :currUid AND postId = posts.id)) as isLikedByUser 
        FROM posts WHERE id = :postId""")
    fun getById(postId: String, currUid: String): LiveData<PostEntity?>

    @Query("""SELECT *, 
        (SELECT EXISTS(SELECT 1 FROM likes WHERE userId = :currUid AND postId = posts.id)) as isLikedByUser 
        FROM posts WHERE id = :postId LIMIT 1""")
    suspend fun getByIdSync(postId: String, currUid: String): PostEntity?

    @Query("""SELECT *, 
        (SELECT EXISTS(SELECT 1 FROM likes WHERE userId = :currUid AND postId = posts.id)) as isLikedByUser 
        FROM posts
        WHERE (:title IS NULL OR bookTitle LIKE '%' || :title || '%')
          AND (:author IS NULL OR bookAuthor LIKE '%' || :author || '%')
          AND (:minRating IS NULL OR rating >= :minRating)
          AND (:minComments IS NULL OR commentsCount >= :minComments)
        ORDER BY createdAt DESC""")
    fun search(title: String?, author: String?, minRating: Int?, minComments: Int?, currUid: String): LiveData<List<PostEntity>>

    @Query("""SELECT *, 
        (SELECT EXISTS(SELECT 1 FROM likes WHERE userId = :currUid AND postId = posts.id)) as isLikedByUser 
        FROM posts
        WHERE bookTitle LIKE '%' || :query || '%'
           OR bookAuthor LIKE '%' || :query || '%'
           OR review LIKE '%' || :query || '%'
        ORDER BY createdAt DESC""")
    fun searchByQuery(query: String, currUid: String): LiveData<List<PostEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(post: PostEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(posts: List<PostEntity>): List<Long>

    @Update
    suspend fun update(post: PostEntity)

    @Update
    suspend fun updateAll(posts: List<PostEntity>)

    @Transaction
    suspend fun upsert(post: PostEntity) {
        if (insert(post) == -1L) {
            update(post)
        }
    }

    @Transaction
    suspend fun upsertAll(posts: List<PostEntity>) {
        if (posts.isEmpty()) return

        val insertResults = insertAll(posts)
        val updates = posts.filterIndexed { index, _ -> insertResults[index] == -1L }
        if (updates.isNotEmpty()) {
            updateAll(updates)
        }
    }

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deleteById(postId: String)
}
