package com.colman.booknook.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.colman.booknook.data.local.dao.*
import com.colman.booknook.data.local.entities.*

@Database(
    entities = [PostEntity::class, WishlistEntity::class, UserEntity::class, CachedBookEntity::class],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun wishlistDao(): WishlistDao
    abstract fun userDao(): UserDao
    abstract fun cachedBookDao(): CachedBookDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "booknook.db")
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
