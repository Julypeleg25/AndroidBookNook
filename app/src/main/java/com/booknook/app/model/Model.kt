package com.booknook.app.model

import android.content.Context
import com.booknook.app.data.local.AppDatabase
import com.booknook.app.data.repository.AppLocalRepository
import com.booknook.app.data.repository.AuthRepository
import com.booknook.app.data.repository.BooksRepository
import com.booknook.app.data.repository.ListsRepository
import com.booknook.app.data.repository.PostsRepository
import com.booknook.app.data.repository.ProfileRepository
import com.booknook.app.model.api.ApiModel
import com.booknook.app.model.firebase.FirebaseModel

object Model {

    lateinit var localRepository: AppLocalRepository
        private set

    lateinit var authRepository: AuthRepository
        private set

    lateinit var profileRepository: ProfileRepository
        private set

    lateinit var booksRepository: BooksRepository
        private set

    lateinit var postsRepository: PostsRepository
        private set

    lateinit var listsRepository: ListsRepository
        private set

    lateinit var storageModel: StorageModel
        private set

    private val firebase = FirebaseModel()
    private val api = ApiModel()

    fun init(context: Context) {
        storageModel = StorageModel()

        val db = AppDatabase.getInstance(context)
        localRepository = AppLocalRepository(
            db.postDao(),
            db.wishlistDao(),
            db.readlistDao(),
            db.userDao(),
            db.cachedBookDao(),
            db.likeDao(),
            db.commentDao()
        )
        profileRepository = ProfileRepository(localRepository, firebase, storageModel)
        authRepository = AuthRepository(firebase, profileRepository, storageModel)
        booksRepository = BooksRepository(localRepository, api)
        postsRepository = PostsRepository(localRepository, firebase, storageModel)
        listsRepository = ListsRepository(localRepository)
    }
}
