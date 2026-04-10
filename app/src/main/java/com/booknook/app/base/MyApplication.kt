package com.booknook.app.base

import android.app.Application
import android.content.Context
import com.booknook.app.model.Model

class MyApplication : Application() {

    companion object {
        lateinit var appContext: Context
            private set
    }

    override fun onCreate() {
        super.onCreate()
        appContext = this
        Model.init(this)
    }
}
