package com.colman.booknook.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.colman.booknook.data.local.dao.*
import com.colman.booknook.data.local.entities.*

@Database(entities = [UserEntity::class, PostEntity::class, BookEntity::class, WishlistEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun postDao(): PostDao
    abstract fun bookDao(): BookDao
    abstract fun wishlistDao(): WishlistDao
}
