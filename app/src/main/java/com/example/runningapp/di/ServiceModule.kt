package com.example.runningapp.di

import android.app.Notification
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.runningapp.R
import com.example.runningapp.other.Constants.ACTION_SHOW_TRACKING_FRAGMENT
import com.example.runningapp.other.Constants.NOTIFICATION_CHANNEL_ID
import com.example.runningapp.ui.theme.ui.MainActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped

@Module
@InstallIn(ServiceComponent::class) //it is used to declare how long will the dependencies inside the module will live
object ServiceModule { //this object here will hold all the dependencies that our tracking service, that will also be scoped to the lifetime of our tracking service,
                        //so the dependencies inside of this module will also live as long as our tracking service does, not as long as the whole application does
    @ServiceScoped //this annotation here declares how many instances the dependencies will live
    @Provides //writing the  provider functions
    fun provideFusedLocationProviderClient(
        @ApplicationContext app : Context
    ) = LocationServices.getFusedLocationProviderClient(app)

    @ServiceScoped
    @Provides
    fun provideMainActivityPendingIntent(
        @ApplicationContext app : Context
    ) : PendingIntent {
        val intent = Intent(app, MainActivity::class.java).also {
            // 1. Create the Intent to launch MainActivity
            it.action = ACTION_SHOW_TRACKING_FRAGMENT
            // **IMPORTANT:** Add FLAG_ACTIVITY_NEW_TASK because we are starting
            // the Activity from a Service context (via the notification)
            it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        // Define the flags based on the Android version (// 2. Determine the correct PendingIntent flags based on SDK version
        //        // (Handling FLAG_IMMUTABLE requirement for Android 12+)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // For Android 12 (S) and above, add FLAG_IMMUTABLE
            FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            // For older versions, FLAG_UPDATE_CURRENT is sufficient
            FLAG_UPDATE_CURRENT
        }

        return PendingIntent.getActivity(
            app,
            0, // Request code
            intent, // The intent to launch
            flags // Use the calculated flags
        )
    }

    @ServiceScoped
    @Provides
    fun provideBaseNotificationBuilder(
        @ApplicationContext app : Context,
        pendingIntent: PendingIntent
    ) : NotificationCompat.Builder{
        return NotificationCompat.Builder(app, NOTIFICATION_CHANNEL_ID)
        .setAutoCancel(false)
        .setOngoing(true)
        .setSmallIcon(R.drawable.ic_directions_run_black_24dp)
        .setContentTitle("Running App")
        .setContentText("00:00:00")
        .setContentIntent(pendingIntent)
        }
}