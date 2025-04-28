package com.example.runningapp.other

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.runningapp.services.polyLine
import java.sql.Time
import java.util.concurrent.TimeUnit

object TrackingUtility {

    fun hasForegroundLocationPermission(context : Context) : Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasBackgroundLocationPermission(context: Context) : Boolean {
        return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        } else return true
    }

    fun getFormattedStopWatchTime(ms : Long, includeMillis : Boolean = false) : String{
        var milliSeconds =  ms
        val hours = TimeUnit.MILLISECONDS.toHours(milliSeconds)
        milliSeconds -= TimeUnit.HOURS.toMillis(hours) //we get the remaining millis by subtracting the hoursMIllis
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliSeconds)
        milliSeconds -= TimeUnit.MINUTES.toMillis(minutes)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliSeconds)
        if(!includeMillis){
            return "${if(hours < 10) "0" else ""}$hours:" +
                    "${if (minutes < 10) "0" else ""}$minutes:" +
                    "${if(seconds < 10) "0" else ""}$seconds"

        }
        milliSeconds -= TimeUnit.SECONDS.toMillis(seconds)
        milliSeconds /= 10
        return "${if(hours < 10) "0" else ""}$hours:" +
                "${if (minutes < 10) "0" else ""}$minutes:" +
                "${if(seconds < 10) "0" else ""}$seconds:" +
                "${if (milliSeconds < 10) "0" else ""}$milliSeconds"
    }

    fun calculatePolylineLength(polyLine: polyLine) : Float{
        var distance = 0f
        for (i in 0..polyLine.size - 2){
            val pos1 = polyLine[i]
            val pos2 = polyLine[i + 1]

            val result = FloatArray(1)

            Location.distanceBetween(
                pos1.latitude,
                pos1.longitude,
                pos2.latitude,
                pos2.longitude,
                result
            )
            distance += result[0]
        }
        return distance
    }
}