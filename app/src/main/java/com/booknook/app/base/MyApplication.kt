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
import com.booknook.app.model.firebase.FirebaseAuthModel
import com.booknook.app.model.firebase.FirebaseCommentsModel
import com.booknook.app.model.firebase.FirebaseListsModel
import com.booknook.app.model.firebase.FirebasePostsModel
import com.booknook.app.model.firebase.FirebaseProfileModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MyApplication : Application() {

    private val database by lazy(LazyThreadSafetyMode.NONE) {
        AppDatabase.getInstance(applicationContext)
    }

    private val firebaseAuth by lazy(LazyThreadSafetyMode.NONE) { FirebaseAuth.getInstance() }
    private val firebaseFirestore by lazy(LazyThreadSafetyMode.NONE) { FirebaseFirestore.getInstance() }
    private val firebaseAuthModel by lazy(LazyThreadSafetyMode.NONE) { FirebaseAuthModel(firebaseAuth) }
    private val firebaseProfileModel by lazy(LazyThreadSafetyMode.NONE) {
        FirebaseProfileModel(
            auth = firebaseAuth,
            db = firebaseFirestore,
            authModel = firebaseAuthModel
        )
    }
    private val firebasePostsModel by lazy(LazyThreadSafetyMode.NONE) {
        FirebasePostsModel(
            db = firebaseFirestore,
            authModel = firebaseAuthModel
        )
    }
    private val firebaseCommentsModel by lazy(LazyThreadSafetyMode.NONE) {
        FirebaseCommentsModel(
            auth = firebaseAuth,
            db = firebaseFirestore,
            authModel = firebaseAuthModel
        )
    }
    private val firebaseListsModel by lazy(LazyThreadSafetyMode.NONE) {
        FirebaseListsModel(
            db = firebaseFirestore,
            authModel = firebaseAuthModel
        )
    }
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
            auth = firebaseAuthModel,
            profileModel = firebaseProfileModel,
            storageModel = storageModel
        )
    }

    val listsRepository by lazy(LazyThreadSafetyMode.NONE) {
        ListsRepository(
            local = localCacheDataSource,
            firebase = firebaseListsModel
        )
    }

    val authRepository by lazy(LazyThreadSafetyMode.NONE) {
        AuthRepository(
            auth = firebaseAuthModel,
            profileModel = firebaseProfileModel,
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
            auth = firebaseAuthModel,
            profileModel = firebaseProfileModel,
            postsModel = firebasePostsModel,
            commentsModel = firebaseCommentsModel,
            storageModel = storageModel
        )
    }

    override fun onCreate() {
        super.onCreate()
    }
}
