package com.colman.booknook.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.colman.booknook.data.local.entities.UserEntity

@Dao
interface UserDao {
    @Query("SELECT * FROM user LIMIT 1")
    fun get(): LiveData<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(user: UserEntity)

    @Query("DELETE FROM user")
    fun clear()
}
