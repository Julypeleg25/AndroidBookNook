package com.booknook.app.data.local

import android.content.Context
import androidx.room.migration.Migration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.booknook.app.data.local.dao.*
import com.booknook.app.data.local.entities.*

@Database(
    entities = [PostEntity::class, CommentEntity::class, WishlistEntity::class, ReadlistEntity::class, UserEntity::class, CachedBookEntity::class, LikeEntity::class],
    version = 12,
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
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE cached_books ADD COLUMN publishedDate TEXT")
                database.execSQL("ALTER TABLE cached_books ADD COLUMN genre TEXT")
                database.execSQL("ALTER TABLE cached_books ADD COLUMN pageCount INTEGER")
                database.execSQL("ALTER TABLE cached_books ADD COLUMN description TEXT")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `feed_posts` (
                        `postId` TEXT NOT NULL,
                        PRIMARY KEY(`postId`)
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS `feed_posts`")
            }
        }

        @Volatile private var INSTANCE: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "booknook.db")
                    .addMigrations(MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
