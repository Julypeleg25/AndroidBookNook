package com.colman.booknook.base
import com.google.firebase.FirebaseApp
import android.app.Application
import com.colman.booknook.model.Model

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        Model.init(this)
    }
}
