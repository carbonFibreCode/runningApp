package com.example.runningapp.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.example.runningapp.R
import com.example.runningapp.other.Constants.ACTION_PAUSE_SERVICE
import com.example.runningapp.other.Constants.ACTION_SHOW_TRACKING_FRAGMENT
import com.example.runningapp.other.Constants.ACTION_START_OR_RESUME_SERVICE
import com.example.runningapp.other.Constants.ACTION_STOP_SERVICE
import com.example.runningapp.other.Constants.NOTIFICATION_CHANNEL_ID
import com.example.runningapp.other.Constants.NOTIFICATION_CHANNEL_NAME
import com.example.runningapp.other.Constants.NOTIFICATION_ID
import com.example.runningapp.other.Constants.LOCATION_UPDATE_INTERVAL
import com.example.runningapp.other.Constants.FASTEST_LOCATION_INTERVAL
import com.example.runningapp.other.Constants.TIMER_UPDATE_INTERVAL
import com.example.runningapp.other.TrackingUtility
import com.example.runningapp.ui.theme.ui.MainActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

typealias polyLine = MutableList<LatLng>
typealias polyLines = MutableList<polyLine>

@AndroidEntryPoint
class TrackingService : LifecycleService() {

    var isFirstRun = true
    var serviceKilled = false

    @Inject
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    @Inject
    lateinit var baseNotificationBuilder : NotificationCompat.Builder //it holds the configuration of every notification, as we update the notification by just creating a new notification with the same ID

    lateinit var curNotificationBuilder : NotificationCompat.Builder //it will have slightly different config, as we will update the text of notification for the time update and also the action buttons for pause, resume etc.

    private val timeRunInSeconds = MutableLiveData<Long>() //we will use this for updating the notification, it will not required to be observed in the companion object


    companion object{
        //we use companion object because we wanna observe on changes from outside of the trackingFragment on that liveData
        val timeRunInMillis = MutableLiveData<Long>() //usually if we have milliseconds we store it in Long dataType
        val isTracking = MutableLiveData<Boolean>()
        val pathPoints = MutableLiveData<polyLines>()
    }



    private fun postInitialValues(){ // for posting initial values ion the isTracking and PathPoints in companion object
        isTracking.postValue(false)
        pathPoints.postValue(mutableListOf())
        timeRunInSeconds.postValue(0L)
        timeRunInMillis.postValue(0L)
    }

    override fun onCreate() {
        super.onCreate()
        curNotificationBuilder = baseNotificationBuilder //initially we assign this way, as notification in same for both, later we will update the data(text) of curNotification
        postInitialValues()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        isTracking.observe(this, Observer{
            updateLocationTracking(it)
            updateNotificationTrackingState(it)
        })
    }

    private fun killService(){
        serviceKilled = true
        isFirstRun = true
        pauseService()
        postInitialValues() //post initial values so that we reset all the livedata values
        stopForeground(Service.STOP_FOREGROUND_REMOVE) //stop foreground so we remove the notification, and killed service
        stopSelf() //stop the whole service
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int { //this function gets called when we send the intent to this service
        intent?.let {
            when (it.action) {
                ACTION_START_OR_RESUME_SERVICE -> {
                    if(isFirstRun){
                        startForegroundService()
                        isFirstRun = false
                    } else{
                        Timber.d("resuming service")
                        startTimer()
                    }
                }

                ACTION_PAUSE_SERVICE -> pauseService()
                ACTION_STOP_SERVICE -> killService()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private var isTimerEnabled = false
    private var lapTime = 0L
    private var timeRun = 0L
    private var timeStarted = 0L
    private var lastSecondTimestamp = 0L

    private fun startTimer() {
        addEmptyPolyline() //add the empty polyline after starting the service and the we need to create the actual line of coordinates
        //so to add thoase coordinates we create dthe function addPathpoints
        isTracking.postValue(true)
        timeStarted = System.currentTimeMillis()
        isTimerEnabled = true
        //we will track the time in coroutiune, as it will be very bad performance wise to call the observers on every milliseconds updates
        //we will delay that coroutines for few Milliseconds that will not be noticeable normally but for computer
        CoroutineScope(Dispatchers.Main).launch {
            while(isTracking.value!!){
                lapTime = System.currentTimeMillis() - timeStarted //time difference between no wand the time started the lap
                timeRunInMillis.postValue(timeRun+lapTime) //update the livedata value
                if(timeRunInMillis.value!! >= lastSecondTimestamp + 1000L) { // the condn says that a new whole second has passed and we need to update our seconds livedata value
                    timeRunInSeconds.postValue(timeRunInSeconds.value!! + 1)
                    lastSecondTimestamp += 1000L
                } // delay the coroutines for 50ms
                delay(TIMER_UPDATE_INTERVAL)
            }
            //out of while loop and update our timerun
            timeRun += lapTime
        }
    }

    private fun pauseService(){
        isTracking.postValue(false)
        isTimerEnabled = false
    }

    private fun updateNotificationTrackingState(isTracking: Boolean){
        val notificationActionText = if(isTracking) "Pause" else "Resume"

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else{
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = if(isTracking) {
            val pauseIntent = Intent(this, TrackingService::class.java).apply {
                action = ACTION_PAUSE_SERVICE
            }
            PendingIntent.getService(this, 1, pauseIntent, pendingIntentFlags)
        } else {
            val resumeIntent = Intent(this, TrackingService::class.java).apply {
                action = ACTION_START_OR_RESUME_SERVICE
            }
            PendingIntent.getService(this, 2, resumeIntent, pendingIntentFlags)
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        //we want to add the actions to the notification, so inorder to do that we will have to remove the older actions to update the notification with the new action
        curNotificationBuilder.javaClass.getDeclaredField("mActions").apply {
            isAccessible = true
            set(curNotificationBuilder, ArrayList<NotificationCompat.Action>())
        } //this was the method to remove all the actions before we add the new action

        curNotificationBuilder = baseNotificationBuilder
            .addAction(R.drawable.ic_pause_black_24dp, notificationActionText, pendingIntent)
        notificationManager.notify(NOTIFICATION_ID, curNotificationBuilder.build())
    }

    val locationCallback = object : LocationCallback(){
        override fun onLocationResult(result : LocationResult) {
            super.onLocationResult(result)

            if(isTracking.value!!){
                result.locations.let {
                    locations ->
                    for (location in locations){
                        addPathPoint(location)
                        Timber.d("New Location : ${location.latitude}, ${location.longitude}")
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun updateLocationTracking(isTracking : Boolean) {
        if(isTracking){
            if(TrackingUtility.hasForegroundLocationPermission(this) && TrackingUtility.hasBackgroundLocationPermission(this)){
                val request = LocationRequest().apply { //request for locan updates
                    interval = LOCATION_UPDATE_INTERVAL
                    fastestInterval = FASTEST_LOCATION_INTERVAL
                    priority = PRIORITY_HIGH_ACCURACY
                }
                fusedLocationProviderClient.requestLocationUpdates(
                    request,
                    locationCallback,
                    Looper.getMainLooper()
                )
            }
        } else{
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        }
    }

    private fun addPathPoint(location: Location?){
        //we check if that location we got is not equal to null
        location?.let{
            val pos = LatLng(location.latitude, location.longitude) //location that we get from polyLine is not in coordinates so we convert it into that
            //we will add this pos to the last polyline of polyLinesList
            pathPoints.value?.apply {
                last().add(pos)
                pathPoints.postValue(this)
            } // TO ACTUALLY BE ABLE TO REQUEST FOR THE LOCATION UPDATES WE NEED to use fused provider client functions
        }
    }

    private fun addEmptyPolyline() // for adding a empty polyline after stopping and resuming the service
        = pathPoints.value?.apply {
            add(mutableListOf())
        pathPoints.postValue(this)
    } ?: pathPoints.postValue(mutableListOf(mutableListOf())) //incase the value in null in pathpoints.value


    //we are creating a foreground service, so we also need to create a notification channel as foreground service comes with the notification
    private fun startForegroundService(){
        startTimer()
        isTracking.postValue(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            createNotificationChannel(notificationManager)
        }
        //notification builder will be injected using DaggerHilt, so no need now explicitly
//        val notificationBuilder = Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
//            .setAutoCancel(false)
//            .setOngoing(true)
//            .setSmallIcon(R.drawable.ic_directions_run_black_24dp)
//            .setContentTitle("Running App")
//            .setContentText("00:00:00")
//            .setContentIntent(getMainActivityPendingIntent())

        startForeground(NOTIFICATION_ID, baseNotificationBuilder.build()    )

        timeRunInSeconds.observe(this, Observer{
            if(!serviceKilled){ //if we killed the service the notification will be removed but there are still the chances that this observer is called one more time and a new notification is being shown
                val notification = curNotificationBuilder
                    .setContentText(TrackingUtility.getFormattedStopWatchTime(it*1000L))
                notificationManager.notify(NOTIFICATION_ID, notification.build())
            }
        })
    }

    //it's also no required as it is inside the baseNotificationBuilder Function
//    private fun getMainActivityPendingIntent() : PendingIntent { // Added return type for clarity
//        val intent = Intent(this, MainActivity::class.java).also {
//            it.action = ACTION_SHOW_TRACKING_FRAGMENT
//        }
//
//        // Define the flags based on the Android version
//        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            // For Android 12 (S) and above, add FLAG_IMMUTABLE
//            FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        } else {
//            // For older versions, FLAG_UPDATE_CURRENT is sufficient
//            FLAG_UPDATE_CURRENT
//        }
//
//        return PendingIntent.getActivity(
//            this,
//            0, // Request code
//            intent, // The intent to launch
//            flags // Use the calculated flags
//        )
//    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }
}