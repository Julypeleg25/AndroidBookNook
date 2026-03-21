package com.booknook.app.data.repository

import androidx.lifecycle.LiveData
import com.booknook.app.data.local.dao.*
import com.booknook.app.data.local.entities.*

class AppLocalRepository(
    private val postDao: PostDao,
    private val wishlistDao: WishlistDao,
    private val readlistDao: ReadlistDao,
    private val userDao: UserDao,
    private val cachedBookDao: CachedBookDao,
    private val likeDao: LikeDao,
    private val commentDao: CommentDao
) {
    fun observePosts(currUid: String): LiveData<List<PostEntity>> = postDao.getAll(currUid)
    fun observeMyPosts(userId: String, currUid: String): LiveData<List<PostEntity>> = postDao.getByUser(userId, currUid)
    fun observePost(postId: String, currUid: String): LiveData<PostEntity?> = postDao.getById(postId, currUid)
    suspend fun getPost(postId: String, currUid: String): PostEntity? = postDao.getByIdSync(postId, currUid)
    fun searchPosts(title: String?, author: String?, minRating: Int?, minComments: Int?, currUid: String) =
        postDao.search(title, author, minRating, minComments, currUid)
    fun searchPostsByQuery(query: String, currUid: String) = postDao.searchByQuery(query, currUid)

    suspend fun upsertPosts(items: List<PostEntity>) = postDao.upsertAll(items)
    suspend fun upsertPost(item: PostEntity) = postDao.upsert(item)
    suspend fun deletePost(postId: String) = postDao.deleteById(postId)

    fun observeWishlist(userId: String) = wishlistDao.getByUser(userId)
    fun observeWishlistExists(key: String) = wishlistDao.observeExistsByKey(key)
    suspend fun upsertWishlist(item: WishlistEntity) = wishlistDao.upsert(item)
    suspend fun existsInWishlist(key: String) = wishlistDao.existsByKey(key)
    suspend fun deleteWishlist(key: String) = wishlistDao.deleteByKey(key)

    fun observeReadlist(userId: String) = readlistDao.getByUser(userId)
    fun observeReadlistExists(key: String) = readlistDao.observeExistsByKey(key)
    suspend fun upsertReadlist(item: ReadlistEntity) = readlistDao.upsert(item)
    suspend fun existsInReadlist(key: String) = readlistDao.existsByKey(key)
    suspend fun deleteReadlist(key: String) = readlistDao.deleteByKey(key)

    fun observeUser() = userDao.get()
    suspend fun upsertUser(user: UserEntity) = userDao.upsert(user)
    suspend fun clearUser() = userDao.clear()

    suspend fun cacheBooks(items: List<CachedBookEntity>) = cachedBookDao.upsertAll(items)

    suspend fun isLikedSync(userId: String, postId: String) = likeDao.exists(userId, postId)
    suspend fun upsertLike(item: LikeEntity) = likeDao.insert(item)
    suspend fun deleteLike(userId: String, postId: String) = likeDao.delete(userId, postId)
    suspend fun clearUserLikes(userId: String) = likeDao.clearUserLikes(userId)
    suspend fun upsertLikes(items: List<LikeEntity>) = likeDao.insertAll(items)

    fun observeComments(postId: String) = commentDao.observeByPost(postId)
    suspend fun upsertComment(item: CommentEntity) = commentDao.insert(item)
    suspend fun upsertComments(items: List<CommentEntity>) = commentDao.insertAll(items)
    suspend fun deleteCommentsByPost(postId: String) = commentDao.deleteByPost(postId)
}
