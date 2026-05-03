package com.booknook.app.base

import android.app.Application
import com.booknook.app.data.local.AppDatabase
import com.booknook.app.data.local.LocalCacheDataSource
import com.booknook.app.data.repository.AuthRepository
import com.booknook.app.data.repository.BooksRepository
import com.booknook.app.data.repository.ListsRepository
import com.booknook.app.data.repository.PostsRepository
import com.booknook.app.data.repository.ProfileRepository
import com.booknook.app.model.StorageModel
import com.booknook.app.model.api.ApiModel
import com.booknook.app.model.firebase.FirebaseModel

class MyApplication : Application() {

    private val database by lazy(LazyThreadSafetyMode.NONE) {
        AppDatabase.getInstance(applicationContext)
    }

    private val firebaseModel by lazy(LazyThreadSafetyMode.NONE) { FirebaseModel() }
    private val apiModel by lazy(LazyThreadSafetyMode.NONE) { ApiModel() }
    private val storageModel by lazy(LazyThreadSafetyMode.NONE) { StorageModel(applicationContext) }

    private val localCacheDataSource by lazy(LazyThreadSafetyMode.NONE) {
        LocalCacheDataSource(
            postDao = database.postDao(),
            wishlistDao = database.wishlistDao(),
            readlistDao = database.readlistDao(),
            userDao = database.userDao(),
            cachedBookDao = database.cachedBookDao(),
            likeDao = database.likeDao(),
            commentDao = database.commentDao()
        )
    }

    val profileRepository by lazy(LazyThreadSafetyMode.NONE) {
        ProfileRepository(
            local = localCacheDataSource,
            firebase = firebaseModel,
            storageModel = storageModel
        )
    }

    val listsRepository by lazy(LazyThreadSafetyMode.NONE) {
        ListsRepository(
            local = localCacheDataSource,
            firebase = firebaseModel
        )
    }

    val authRepository by lazy(LazyThreadSafetyMode.NONE) {
        AuthRepository(
            firebase = firebaseModel,
            profileRepository = profileRepository,
            listsRepository = listsRepository,
            storageModel = storageModel,
            local = localCacheDataSource
        )
    }

    val booksRepository by lazy(LazyThreadSafetyMode.NONE) {
        BooksRepository(
            local = localCacheDataSource,
            api = apiModel
        )
    }

    val postsRepository by lazy(LazyThreadSafetyMode.NONE) {
        PostsRepository(
            local = localCacheDataSource,
            firebase = firebaseModel,
            storageModel = storageModel
        )
    }

    override fun onCreate() {
        super.onCreate()
    }
}
