package com.colman.booknook.data.repository

import androidx.lifecycle.LiveData
import com.colman.booknook.data.local.dao.*
import com.colman.booknook.data.local.entities.*

class AppLocalRepository(
    private val postDao: PostDao,
    private val wishlistDao: WishlistDao,
    private val userDao: UserDao,
    private val cachedBookDao: CachedBookDao
) {
    fun observePosts(): LiveData<List<PostEntity>> = postDao.getAll()
    fun observeMyPosts(userId: String): LiveData<List<PostEntity>> = postDao.getByUser(userId)
    fun observePost(postId: String): LiveData<PostEntity?> = postDao.getById(postId)
    fun searchPosts(title: String?, author: String?, minRating: Int?, minComments: Int?) =
        postDao.search(title, author, minRating, minComments)

    suspend fun upsertPosts(items: List<PostEntity>) = postDao.upsertAll(items)
    suspend fun upsertPost(item: PostEntity) = postDao.upsert(item)
    suspend fun deletePost(postId: String) = postDao.deleteById(postId)

    fun observeWishlist(userId: String) = wishlistDao.getByUser(userId)
    suspend fun upsertWishlist(item: WishlistEntity) = wishlistDao.upsert(item)
    suspend fun deleteWishlist(key: String) = wishlistDao.deleteByKey(key)

    fun observeUser() = userDao.get()
    suspend fun upsertUser(user: UserEntity) = userDao.upsert(user)
    suspend fun clearUser() = userDao.clear()

    suspend fun cacheBooks(items: List<CachedBookEntity>) = cachedBookDao.upsertAll(items)
}
