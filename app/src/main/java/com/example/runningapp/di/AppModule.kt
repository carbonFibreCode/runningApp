package com.example.runningapp.di

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import com.example.runningapp.other.Constants.SHARED_PREFERENCE_NAME
import androidx.room.Room
import com.example.runningapp.db.RunningDatabase
import com.example.runningapp.other.Constants.KEY_NAME
import com.example.runningapp.other.Constants.KEY_WEIGHT
import com.example.runningapp.other.Constants.KEY_FIRST_TIME_TOGGLE
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import com.example.runningapp.other.Constants.RUNNING_DATABASE_NAME
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent :: class)
object AppModule {//it is used to declare how long will the dependencies inside the module will live

    @Singleton //this annotation here declares how many instances the dependencies will live
    @Provides
    fun provideRunningDatabase( @ApplicationContext app : Context) = Room.databaseBuilder(
        app,
        RunningDatabase::class.java,
        RUNNING_DATABASE_NAME

    ).build()

    @Singleton
    @Provides
    fun provideRunDao(db : RunningDatabase) = db.getRunDao()

    @Singleton
    @Provides
    fun provideSharedPreferences(@ApplicationContext app : Context) =
        app.getSharedPreferences(SHARED_PREFERENCE_NAME, MODE_PRIVATE)

    @Singleton
    @Provides
    fun provideWeight(sharedPref : SharedPreferences) = sharedPref.getFloat(KEY_WEIGHT, 80f)

    @Singleton
    @Provides
    fun provideName(sharedPref : SharedPreferences) = sharedPref.getString(KEY_NAME, "") ?: ""

    @Singleton
    @Provides
    fun provideFirstTimeToggle(sharedPref : SharedPreferences) = sharedPref.getBoolean(KEY_FIRST_TIME_TOGGLE, true)
}