package com.example.runningapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class BaseApplication : Application() {

    //setting up timber logging library to avoid using default log command and writing tags all the time
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}