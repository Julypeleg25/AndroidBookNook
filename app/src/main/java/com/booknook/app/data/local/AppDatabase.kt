package com.booknook.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.booknook.app.data.local.dao.*
import com.booknook.app.data.local.entities.*

@Database(
    entities = [PostEntity::class, WishlistEntity::class, ReadlistEntity::class, UserEntity::class, CachedBookEntity::class, LikeEntity::class, CommentEntity::class],
    version = 4
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun wishlistDao(): WishlistDao
    abstract fun readlistDao(): ReadlistDao
    abstract fun userDao(): UserDao
    abstract fun cachedBookDao(): CachedBookDao
    abstract fun likeDao(): LikeDao
    abstract fun commentDao(): CommentDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "booknook.db")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
