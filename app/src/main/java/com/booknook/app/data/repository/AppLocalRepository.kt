package com.booknook.app.data.repository

import androidx.lifecycle.LiveData
import com.booknook.app.data.local.dao.*
import com.booknook.app.data.local.entities.*

class AppLocalRepository(
    private val postDao: PostDao,
    private val wishlistDao: WishlistDao,
    private val readlistDao: ReadlistDao,
    private val userDao: UserDao,
    private val cachedBookDao: CachedBookDao
) {
    fun observePosts(): LiveData<List<PostEntity>> = postDao.getAll()
    fun observeMyPosts(userId: String): LiveData<List<PostEntity>> = postDao.getByUser(userId)
    fun observePost(postId: String): LiveData<PostEntity?> = postDao.getById(postId)
    suspend fun getPost(postId: String): PostEntity? = postDao.getByIdSync(postId)
    fun searchPosts(title: String?, author: String?, minRating: Int?, minComments: Int?) =
        postDao.search(title, author, minRating, minComments)
    fun searchPostsByQuery(query: String) = postDao.searchByQuery(query)

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
}
