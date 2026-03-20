package com.colman.booknook.model

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.Room
import com.colman.booknook.data.local.AppDatabase
import com.colman.booknook.data.local.entities.PostEntity
import com.colman.booknook.data.local.entities.UserEntity
import com.colman.booknook.data.remote.RetrofitClient
import com.colman.booknook.data.remote.model.BookSearchResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class Model private constructor() {

    private lateinit var database: AppDatabase

    companion object {
        val instance: Model = Model()
        
        fun init(context: Context) {
            instance.database = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "booknook_db"
            ).build()
        }
    }

    fun getAllPosts(): LiveData<List<PostEntity>> {
        return database.postDao().getAll()
    }

    suspend fun searchBooks(query: String): BookSearchResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val response = RetrofitClient.googleBooksApi.searchBooks(query)
                if (response.isSuccessful) {
                    response.body()
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    // Authentication
    fun login(email: String, pass: String, callback: (Boolean) -> Unit) {
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            callback(task.isSuccessful)
        }
    }

    fun register(email: String, pass: String, username: String, callback: (Boolean) -> Unit) {
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Ideally create UserEntity in DB or Firebase Firestore here
                callback(true)
            } else {
                callback(false)
            }
        }
    }

    fun logout() {
        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
    }

    fun isLoggedIn(): Boolean {
        return com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null
    }
}
