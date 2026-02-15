package com.colman.booknook.base

import android.app.Application
import com.colman.booknook.model.Model

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Model.init(this)
    }
}
