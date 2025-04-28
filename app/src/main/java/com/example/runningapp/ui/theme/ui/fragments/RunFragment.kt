package com.example.runningapp.ui.theme.ui.fragments

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.runningapp.ui.theme.ui.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.example.runningapp.R
import com.example.runningapp.databinding.FragmentRunBinding
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import android.util.Log
import android.widget.AdapterView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.runningapp.adapters.RunAdapter
import com.example.runningapp.other.SortType
import com.example.runningapp.other.TrackingUtility

@AndroidEntryPoint
class RunFragment : Fragment(R.layout.fragment_run) {
    private val viewModel : MainViewModel by viewModels()
    private var _binding : FragmentRunBinding? = null

    private lateinit var runAdapter : RunAdapter

    //declaring the activity result launchers
    private lateinit var foregroundLocationPermissionLauncher:
            ActivityResultLauncher<Array<String>>
    private lateinit var backgroundLocationPermissionLauncher:
            ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerPermissionLauncher()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentRunBinding.bind(view)

        setupRecyclerView()

        when(viewModel.sortType){
            SortType.DATE -> _binding?.spFilter?.setSelection(0)
            SortType.RUNNING_TIME -> _binding?.spFilter?.setSelection(1)
            SortType.AVG_SPEED -> _binding?.spFilter?.setSelection(3)
            SortType.CALORIES_BURNED -> _binding?.spFilter?.setSelection(4)
            SortType.DISTANCE -> _binding?.spFilter?.setSelection(2)
        }

        _binding?.spFilter?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(
                adapterView: AdapterView<*>?,
                view: View?,
                pos: Int,
                id: Long
            ) {
                when(pos){
                    0 -> viewModel.sortRuns(SortType.DATE)
                    1 -> viewModel.sortRuns(SortType.RUNNING_TIME)
                    2 -> viewModel.sortRuns(SortType.DISTANCE)
                    3 -> viewModel.sortRuns(SortType.AVG_SPEED)
                    4 -> viewModel.sortRuns(SortType.CALORIES_BURNED)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }

        viewModel.runs.observe(viewLifecycleOwner, Observer{
            runAdapter.submitList(it)
        })

        _binding?.fab?.setOnClickListener {
            // Check permissions before navigating or starting tracking that requires them
            // Depending on app flow, you might request permissions here instead of onViewCreated start
            checkAndRequestPermissionsBeforeAction()
        }
        requesLocationPermission()

    }

    private fun registerPermissionLauncher(){
        foregroundLocationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            permissions ->
            val fineLocationgranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseLocationgranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

            when{
                fineLocationgranted || coarseLocationgranted -> {
                    checkAndRequestBackgroundPermission()
                }
                else -> {
                    handleForegroundDenial()
                }
            }
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            backgroundLocationPermissionLauncher =
                registerForActivityResult(ActivityResultContracts.RequestPermission()){
                    isGranted ->
                    if(isGranted){
                        Log.d("RunFragment", "Background location permission granted")
                        //// startBackgroundTracking() // Example action
                    }
                    else {
                        Log.d("RunFragment", "Background location permission denied")
                        showPermissionDeniedDialog(
                            "Background location access was denied. Tracking while the app is in the background will not work."
                        )
                    }
                }
        }
    }

    // --- Permission Request Orchestration ---
    private fun requesLocationPermission(){
        if(TrackingUtility.hasForegroundLocationPermission(requireContext())){
            checkAndRequestBackgroundPermission()
        }
        else{
            requestForegroundPermission()
        }
    }

    private fun requestForegroundPermission(){
        val permissionsToRequest = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val showRationaleFine = shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
        val showRationaleCoarse = shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)

        if(showRationaleFine || showRationaleCoarse){
            showRationaleDialog(
                title = "Location permission Needed",
                message = "This app needs access to your location (Precise or Approximate) to track your runs.",
                onConfirm = {
                    foregroundLocationPermissionLauncher.launch(permissionsToRequest)
                }
            )
        } else{
            foregroundLocationPermissionLauncher.launch(permissionsToRequest)
        }

    }

    private fun checkAndRequestBackgroundPermission() {
        // Only needed on Android Q+ and if foreground is already granted
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || !TrackingUtility.hasForegroundLocationPermission(requireContext())) {
            return
        }

        if (TrackingUtility.hasBackgroundLocationPermission(requireContext())) {
            Log.d("RunFragment", "Background permission already granted.")
            // Both foreground and background granted, proceed with full functionality
            // startBackgroundTracking() // Example action
        } else {
            Log.d("RunFragment", "Requesting background permission.")
            // Background permission not granted, request it (might need rationale first)
            // Rationale for background is tricky as system often directs to settings
            showRationaleDialog(
                title = "Background Location Needed",
                message = "To track your run even when the app is minimized or the screen is off, please grant 'Allow all the time' location access. This might take you to the app settings.",
                onConfirm = {
                    backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
            )
        }
    }

    //handling denials
    private fun handleForegroundDenial() {
        // Check if it was permanently denied (user checked "Don't ask again")
        val permanentlyDenied = !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) &&
                !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)

        if (permanentlyDenied) {
            showSettingsDialog(
                "Location Permission Required",
                "Foreground location permission has been permanently denied. Please grant access (Precise or Approximate) in the app settings to enable run tracking."
            )
        } else {
            // Explain why the feature is disabled or ask again later
            showPermissionDeniedDialog("Foreground location permission was denied. Run tracking requires this access.")
        }

    }

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

    private fun checkAndRequestPermissionsBeforeAction() {
        // This function would be called by your FAB click listener
        if (TrackingUtility.hasForegroundLocationPermission(requireContext())) {
            // If background is also needed for this specific action, check it too
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || TrackingUtility.hasBackgroundLocationPermission(requireContext())) {
                // Permissions granted, proceed with action
                findNavController().navigate(R.id.action_runFragment_to_trackingFragment)
            } else {
                // Need background permission first
                checkAndRequestBackgroundPermission() // Request it again, maybe show rationale
            }
        } else {
            // Need foreground permission first
            requestForegroundPermission() // Request it again, maybe show rationale
        }
    }

    private fun setupRecyclerView() {
        runAdapter = RunAdapter()
        _binding?.rvRuns?.apply {
            adapter = runAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }




    override fun onDestroyView() {
        super.onDestroyView()
        // Clear the binding
        _binding = null
    }
}