package com.example.runningapp.ui.theme.ui.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.util.Calendar
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.runningapp.R
import com.example.runningapp.databinding.FragmentTrackingBinding
import com.example.runningapp.db.Run
import com.example.runningapp.services.TrackingService
import com.example.runningapp.ui.theme.ui.viewmodels.MainViewModel
import com.google.android.gms.maps.GoogleMap
import dagger.hilt.android.AndroidEntryPoint
import kotlin.getValue
import com.example.runningapp.other.Constants.ACTION_START_OR_RESUME_SERVICE
import com.example.runningapp.other.Constants.ACTION_PAUSE_SERVICE
import com.example.runningapp.other.Constants.ACTION_STOP_SERVICE
import com.example.runningapp.other.Constants.MAP_ZOOM
import com.example.runningapp.other.Constants.POLYLINE_COLOUR
import com.example.runningapp.other.Constants.POLYLINE_WIDTH
import com.example.runningapp.other.TrackingUtility
import com.example.runningapp.services.polyLine
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.round

const val CANCEL_TRACKING_DIALOG_TAG = "CancelDialog"

@AndroidEntryPoint
class TrackingFragment : Fragment(R.layout.fragment_tracking) {
    private val viewModel : MainViewModel by viewModels()
    private var _binding : FragmentTrackingBinding? = null

    private var map : GoogleMap? = null

    private var curTimeInMillis = 0L

    @set:Inject
    private var weight = 80f

    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>

    private var isTracking = false
    private var pathPoints = mutableListOf<polyLine>()

    private var menu : Menu? = null //global reference to menu


    override fun onCreateView( //inside of this function, we setHasOptions to true, then only onCreateOptionsMenu will be called
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerNotificationPermissionLauncher()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {//savedinstancestate contain the information if we rotated the device, we will check for it's nullability and call for dialog to survive rotation
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentTrackingBinding.bind(view)

        _binding?.mapView?.onCreate(savedInstanceState)

        if(savedInstanceState != null){
            val cancelTrakcingDialog = parentFragmentManager.findFragmentByTag(CANCEL_TRACKING_DIALOG_TAG) as CancelTrackingDialog?
            cancelTrakcingDialog?.setYesListener {
                stopRun()
            }
        }

        _binding?.btnToggleRun?.setOnClickListener {
            checkPermissionsAndStartService()
        }

        _binding?.btnFinishRun?.setOnClickListener {
            zoomToSeeWholeTrack()
            endRunAndSaveToDb()
        }

        _binding?.mapView?.getMapAsync{
            map = it
            addAllPolylines() //it gets called when the fragment is created like when we rotate the device
        }

        subscribeToObservers()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.toolbar_tracking_menu, menu)
        this.menu = menu
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        if (curTimeInMillis > 0L){//condition means that we've already started to run and now we should be able to cancel it if we wish to do so
            this.menu?.getItem(0)?.isVisible = true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId){
            R.id.miCancelTracking -> {
                showCancelTrackingDialog()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showCancelTrackingDialog(){
        CancelTrackingDialog().apply {
            setYesListener {
                stopRun()
            }
        }.show(parentFragmentManager, CANCEL_TRACKING_DIALOG_TAG)
    }

    private fun stopRun(){
        _binding?.tvTimer?.text = "00:00:00"
        sendCommandToService(ACTION_STOP_SERVICE)
        findNavController().navigate(R.id.action_trackingFragment_to_runFragment)
    }

    private fun subscribeToObservers(){
        TrackingService.isTracking.observe(viewLifecycleOwner, Observer{isTrackingValue
            ->
          updateTracking(isTrackingValue)
        })

        TrackingService.pathPoints.observe(viewLifecycleOwner, Observer{pathPointsValue
            ->
            pathPoints = pathPointsValue
            addLatestPolyline()
            moveCameraToUser()
        })

        TrackingService.timeRunInMillis.observe ( viewLifecycleOwner, Observer{
            curTimeInMillis = it
            val formattedTime = TrackingUtility.getFormattedStopWatchTime(curTimeInMillis, true)
            _binding?.tvTimer?.text = formattedTime
        } )
    }

    private fun toggleRun() { //function to send command to toggle tracking on/off
        if(isTracking){
            menu?.getItem(0)?.isVisible = true
            sendCommandToService(ACTION_PAUSE_SERVICE)
        }
        else{
            sendCommandToService(ACTION_START_OR_RESUME_SERVICE)
        }
    }

    private fun updateTracking(isTracking : Boolean){ //update tracking state whether user is tracked ore not
        this.isTracking = isTracking
        if(!isTracking && curTimeInMillis > 0L){
            _binding?.btnToggleRun?.text = "Start"
            _binding?.btnFinishRun?.visibility = View.VISIBLE
        } else if(isTracking) {
            _binding?.btnToggleRun?.text = "Stop"
            menu?.getItem(0)?.isVisible = true
            _binding?.btnFinishRun?.visibility = View.GONE
        }
    }

    private fun moveCameraToUser() {
        if(pathPoints.isNotEmpty() && pathPoints.last().isNotEmpty()){
            map?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    pathPoints.last().last(),
                    MAP_ZOOM
                )
            )
        }
    }

    private fun zoomToSeeWholeTrack(){
        if (pathPoints.isEmpty() || pathPoints.last().isEmpty()) {
            return // Don't zoom if there are no points
        }
        val bounds = LatLngBounds.Builder()
        for(polyline in pathPoints){
            for (pos in polyline){
                bounds.include(pos)
            }
        }

        _binding?.mapView?.let {mapView ->
            if(mapView.width > 0 && mapView.height > 0){
                map?.moveCamera( //we are directly moving the camera not animating it, as we will take the screenshot instantly and animation may overlap with that
                    CameraUpdateFactory.newLatLngBounds(
                        bounds.build(),
                        mapView.width,
                        mapView.height,
                        (mapView.height * 0.05f).toInt()
                    )
                )
            } else{
                Timber.w("MapView dimensions not ready for zoomToSeeWholeTrack.")
            }
        } ?: Timber.e("Cannot zoomToSeeWholeTrack: mapView binding is null.")
    }

    private fun endRunAndSaveToDb(){
        val rootView = view //Capture the view safely for use in Snack bar inside callbacks
        //Null check for rootView
        if (rootView == null) {
            Timber.e("Cannot save run: Fragment view is null.")
            stopRun() // stop if view is gone
            return
        }
        map?.snapshot {
            bmp->
            if (bmp == null){
                Timber.e("Map snapshot failed")
                Snackbar.make(rootView, "Error saving run: Could not generate map image.", Snackbar.LENGTH_LONG).show()
                stopRun()
                return@snapshot
            }
            //we need to calculate the total distance of our run, to do that we will loop through our poly lines list, we'll use the utility function for that
            var distanceInMeters = 0
            for (polyline in pathPoints){
                distanceInMeters += TrackingUtility.calculatePolylineLength(polyline).toInt()
            }
            val avgSpeed = if (curTimeInMillis > 0) {
                round((distanceInMeters/1000f) / (curTimeInMillis / 1000f / 60 / 60) * 10) / 10f
            }
            else{
                0f
            }
            val dateTimeStamp = Calendar.getInstance().timeInMillis
            val caloriesBurned = ((distanceInMeters / 1000f) * weight).toInt()
            val run = Run(bmp, dateTimeStamp, avgSpeed, distanceInMeters, curTimeInMillis, caloriesBurned)

            viewModel.insertRun(run)
            Snackbar.make(
                rootView, // <-- Use the Fragment's root view via binding
                "Run saved Successfully",
                Snackbar.LENGTH_LONG
            ).show()
            stopRun()
        } ?: run {
            Timber.e("cannot save run : Map is null")
            Snackbar.make(rootView, "Error saving run: Map not available.", Snackbar.LENGTH_LONG).show()
            stopRun()
        }
    }

    private fun addAllPolylines() { // add all the polylines if the device is rotated the activity ios recreated and all the drawing will be lost and we need to re-create.
        for (polyline in pathPoints){
            val polylineOptions = PolylineOptions()
                .color(POLYLINE_COLOUR)
                .width(POLYLINE_WIDTH)
                .addAll(polyline)
            map?.addPolyline(polylineOptions)
        }
    }

    private fun addLatestPolyline(){
        if(pathPoints.isNotEmpty() && pathPoints.last().size > 1){//ensuring that the pathpoints list is not empty and the last line has more than one point in it
            val preLastLatLng = pathPoints.last()[pathPoints.last().size-2] //accessing the 2nd last point last polyline list
            val lastLatLng = pathPoints.last().last() // accessing the last point of last polyline
            //define the colour and othjer aspects of the poolyline to be drawn
            val polylineOptions = PolylineOptions()
                .color(POLYLINE_COLOUR)
                .width(POLYLINE_WIDTH)
                .add(preLastLatLng)
                .add(lastLatLng)
            map?.addPolyline(polylineOptions)
        }
    }

    private fun sendCommandToService(action: String) =
        Intent(requireContext(), TrackingService::class.java).also {
            it.action = action
            requireContext().startService(it)
        }

    private fun registerNotificationPermissionLauncher() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            notificationPermissionLauncher =
                registerForActivityResult(ActivityResultContracts.RequestPermission()){
                    isGranted ->
                    if(isGranted){
                        Toast.makeText(context, "Notification Granted", Toast.LENGTH_SHORT).show()
                    }else {
                        handleNotificationDenial() // Show settings dialog etc.
                    }
                }
        }
    }

    private fun checkPermissionsAndStartService(){
        if(!TrackingUtility.hasForegroundLocationPermission(requireContext())){
            //// 1. Verify Location (should have been granted in RunFragment)
            showPermissionDeniedDialog("Location permission is required but missing. Please return and grant permission.")
            return
        }
        else if(!TrackingUtility.hasBackgroundLocationPermission((requireContext()))){
            showPermissionDeniedDialog("Background Location permission is required but missing. Please return and grant permission.")
            return
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            if(ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ){
                requestNotificationPermission()
                return
            }
        } // else{
//            Toast.makeText(context, "Notification Granted", Toast.LENGTH_SHORT).show()
//        }
            toggleRun()
    }

    private fun requestNotificationPermission(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            if(shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)){
                showRationaleDialog(
                    title = "Notification Permission",
                    message = "This app needs to show notifications to display your run progress while tracking.",
                    onConfirm = { notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                )
            } else{
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun handleNotificationDenial() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permanentlyDenied = !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
            if (permanentlyDenied) {
                showSettingsDialog("Notification Permission Required", "Notifications have been permanently denied. Please enable them in app settings to see tracking progress.")
            } else {
                showPermissionDeniedDialog("Notification permission was denied. Tracking progress won't be shown in the status bar.")
            }
        }
    }
    //helper dialogs
    private fun showRationaleDialog(title: String, message: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { _, _ -> onConfirm() } // Execute the confirm action (usually re-launching permission request)
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSettingsDialog(title: String, message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Go to Settings") { _, _ ->
                // Intent to open app settings
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                // Create a Uri pointing to your app's specific settings page
                val uri = Uri.fromParts("package", requireActivity().packageName, null)
                intent.data = uri
                startActivity(intent) // User needs to manually grant permission in settings
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun showPermissionDeniedDialog(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Permission Denied")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }



    override fun onResume() {
        super.onResume()
        _binding?.mapView?.onResume()
    }

    override fun onStart() {
        super.onStart()
        _binding?.mapView?.onStart()
    }

    override fun onStop() {
        super.onStop()
        _binding?.mapView?.onStop()
    }

    override fun onPause() {
        super.onPause()
        _binding?.mapView?.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        _binding?.mapView?.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding?.mapView?.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        _binding?.mapView?.onSaveInstanceState(outState)
        _binding = null
    }
}