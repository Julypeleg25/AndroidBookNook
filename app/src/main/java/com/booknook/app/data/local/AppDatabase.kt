package com.booknook.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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

@Database(
    entities = [
        PostEntity::class,
        CommentEntity::class,
        WishlistEntity::class,
        ReadlistEntity::class,
        UserEntity::class,
        CachedBookEntity::class,
        LikeEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun commentDao(): CommentDao
    abstract fun wishlistDao(): WishlistDao
    abstract fun readlistDao(): ReadlistDao
    abstract fun userDao(): UserDao
    abstract fun cachedBookDao(): CachedBookDao
    abstract fun likeDao(): LikeDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "booknook.db")
                    .fallbackToDestructiveMigration()
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
