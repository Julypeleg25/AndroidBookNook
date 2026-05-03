package com.booknook.app.data.local

import androidx.lifecycle.LiveData
import com.booknook.app.data.local.dao.CachedBookDao
import com.booknook.app.data.local.dao.CommentDao
import com.booknook.app.data.local.dao.LikeDao
import com.booknook.app.data.local.dao.PostDao
import com.booknook.app.data.local.dao.ReadlistDao
import com.booknook.app.data.local.dao.UserDao
import com.booknook.app.data.local.dao.WishlistDao
import com.booknook.app.data.local.entities.CachedBookEntity
import com.booknook.app.data.local.entities.CommentEntity
import com.booknook.app.data.local.entities.LikeEntity
import com.booknook.app.data.local.entities.PostEntity
import com.booknook.app.data.local.entities.ReadlistEntity
import com.booknook.app.data.local.entities.UserEntity
import com.booknook.app.data.local.entities.WishlistEntity

class LocalCacheDataSource(
    private val postDao: PostDao,
    private val wishlistDao: WishlistDao,
    private val readlistDao: ReadlistDao,
    private val userDao: UserDao,
    private val cachedBookDao: CachedBookDao,
    private val likeDao: LikeDao,
    private val commentDao: CommentDao
) {
    fun observePosts(currentUserId: String): LiveData<List<PostEntity>> = postDao.getAll(currentUserId)

    fun observeMyPosts(userId: String, currentUserId: String): LiveData<List<PostEntity>> {
        return postDao.getByUser(userId, currentUserId)
    }

    fun observePost(postId: String, currentUserId: String): LiveData<PostEntity?> {
        return postDao.getById(postId, currentUserId)
    }

    suspend fun getPost(postId: String, currentUserId: String): PostEntity? {
        return postDao.getByIdSync(postId, currentUserId)
    }

    fun searchPostsByQuery(query: String, currentUserId: String): LiveData<List<PostEntity>> {
        return postDao.searchByQuery(query, currentUserId)
    }

    suspend fun upsertPosts(items: List<PostEntity>) = postDao.upsertAll(items)

    suspend fun upsertPost(item: PostEntity) = postDao.upsert(item)

    suspend fun deletePost(postId: String) = postDao.deleteById(postId)

    suspend fun deleteUserPostsNotIn(userId: String, ids: List<String>) {
        if (ids.isEmpty()) {
            postDao.deleteByUser(userId)
        } else {
            postDao.deleteByUserAndIdNotIn(userId, ids)
        }
    }

    fun observeWishlist(userId: String): LiveData<List<WishlistEntity>> = wishlistDao.getByUser(userId)

    suspend fun getWishlistByUser(userId: String) = wishlistDao.getByUserSync(userId)

    fun observeWishlistExists(key: String): LiveData<Boolean> = wishlistDao.observeExistsByKey(key)

    suspend fun upsertWishlist(item: WishlistEntity) = wishlistDao.upsert(item)

    suspend fun replaceWishlist(userId: String, items: List<WishlistEntity>) = wishlistDao.replaceForUser(userId, items)

    suspend fun getWishlist(key: String) = wishlistDao.getByKey(key)

    suspend fun deleteWishlist(key: String) = wishlistDao.deleteByKey(key)

    fun observeReadlist(userId: String): LiveData<List<ReadlistEntity>> = readlistDao.getByUser(userId)

    suspend fun getReadlistByUser(userId: String) = readlistDao.getByUserSync(userId)

    fun observeReadlistExists(key: String): LiveData<Boolean> = readlistDao.observeExistsByKey(key)

    suspend fun upsertReadlist(item: ReadlistEntity) = readlistDao.upsert(item)

    suspend fun replaceReadlist(userId: String, items: List<ReadlistEntity>) = readlistDao.replaceForUser(userId, items)

    suspend fun getReadlist(key: String) = readlistDao.getByKey(key)

    suspend fun deleteReadlist(key: String) = readlistDao.deleteByKey(key)

    fun observeUser() = userDao.get()

    suspend fun upsertUser(user: UserEntity) = userDao.upsert(user)

    suspend fun clearUser() = userDao.clear()

    suspend fun cacheBooks(items: List<CachedBookEntity>) = cachedBookDao.upsertAll(items)

    suspend fun searchCachedBooks(query: String, limit: Int, offset: Int): List<CachedBookEntity> {
        return cachedBookDao.search(query, limit, offset)
    }

    suspend fun getCachedBook(bookId: String) = cachedBookDao.getById(bookId)

    suspend fun isLikedSync(userId: String, postId: String) = likeDao.exists(userId, postId)

    suspend fun upsertLike(item: LikeEntity) = likeDao.insert(item)

    suspend fun upsertLikes(items: List<LikeEntity>) = likeDao.insertAll(items)

    suspend fun replaceUserLikes(userId: String, items: List<LikeEntity>) {
        likeDao.replaceUserLikes(userId, items)
    }

    suspend fun deleteLike(userId: String, postId: String) = likeDao.delete(userId, postId)

    suspend fun deleteLikesByPost(postId: String) = likeDao.deleteByPost(postId)

    fun observeComments(postId: String) = commentDao.observeByPost(postId)

    suspend fun replaceComments(postId: String, comments: List<CommentEntity>) {
        commentDao.replaceByPost(postId, comments)
    }

    suspend fun updateUserContentProfile(userId: String, username: String, avatarUrl: String?) {
        postDao.updateUsernameForUser(userId, username)
        commentDao.updateAuthorProfile(userId, username, avatarUrl)
    }

    suspend fun clearAllData() {
        postDao.deleteAll()
        wishlistDao.deleteAll()
        readlistDao.deleteAll()
        userDao.clear()
        cachedBookDao.deleteAll()
        likeDao.deleteAll()
        commentDao.deleteAll()
    }
}
