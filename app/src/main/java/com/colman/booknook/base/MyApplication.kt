package com.colman.booknook.app.base

import android.app.Application
import com.colman.booknook.app.model.Model

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Model.init(this)
    }
}
