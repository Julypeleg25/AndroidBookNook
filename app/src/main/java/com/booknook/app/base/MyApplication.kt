package com.booknook.app.base

import android.app.Application
import com.booknook.app.model.Model

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Model.init(this)
    }
}
