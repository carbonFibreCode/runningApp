package com.example.runningapp.ui.theme.ui


import android.app.ComponentCaller
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.runningapp.databinding.ActivityMainBinding
import com.example.runningapp.R
import com.example.runningapp.other.Constants.ACTION_SHOW_TRACKING_FRAGMENT
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private lateinit var navController : NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.findNavController()

        navigateToTrackingFragmentIfNeeded(intent)

        binding.bottomNavigationView.setupWithNavController(navController)
        binding.bottomNavigationView.setOnNavigationItemReselectedListener{/* NO-OP */}

        navHostFragment.findNavController()
            .addOnDestinationChangedListener { _, destination, _ ->
                when(destination.id){
                    R.id.settingsFragment, R.id.statisticsFragment, R.id.runFragment ->
                        binding.bottomNavigationView.visibility = View.VISIBLE
                    else -> binding.bottomNavigationView.visibility = View.GONE
                }
            }
        applyWindowInsets()
    }

    override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
        super.onNewIntent(intent, caller)
        navigateToTrackingFragmentIfNeeded(intent)
    }

    private fun applyWindowInsets() {
        // Get initial padding values of the views you want to apply insets to
        // We need these because updatePadding adds to the existing padding.
        val initialAppBarPaddingTop = binding.appBarLayout.paddingTop


        ViewCompat.setOnApplyWindowInsetsListener(binding.rootView) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Apply top inset as top padding to the AppBarLayout
            view.updatePadding(top = initialAppBarPaddingTop + insets.top)

            windowInsets // Return original insets allows children to also react if needed
        }
    }

    private fun navigateToTrackingFragmentIfNeeded(intent: Intent?) {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        if(intent?.action == ACTION_SHOW_TRACKING_FRAGMENT){
            navController.navigate(R.id.action_global_trackingFragment)
        }
    }
}

