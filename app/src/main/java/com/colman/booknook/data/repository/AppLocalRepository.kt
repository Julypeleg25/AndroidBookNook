package com.booknook.app.data.repository

import androidx.lifecycle.LiveData
import com.booknook.app.data.local.dao.*
import com.booknook.app.data.local.entities.*

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

    fun upsertPosts(items: List<PostEntity>) = postDao.upsertAll(items)
    fun upsertPost(item: PostEntity) = postDao.upsert(item)
    fun deletePost(postId: String) = postDao.deleteById(postId)

    fun observeWishlist(userId: String) = wishlistDao.getByUser(userId)
    fun upsertWishlist(item: WishlistEntity) = wishlistDao.upsert(item)
    fun deleteWishlist(key: String) = wishlistDao.deleteByKey(key)

    fun observeUser() = userDao.get()
    fun upsertUser(user: UserEntity) = userDao.upsert(user)
    fun clearUser() = userDao.clear()

    fun cacheBooks(items: List<CachedBookEntity>) = cachedBookDao.upsertAll(items)
}
