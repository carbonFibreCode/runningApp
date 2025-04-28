package com.example.runningapp.ui.theme.ui.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.runningapp.R
import com.example.runningapp.databinding.FragmentSetupBinding
import com.example.runningapp.other.Constants.KEY_FIRST_TIME_TOGGLE
import com.example.runningapp.other.Constants.KEY_NAME
import com.example.runningapp.other.Constants.KEY_WEIGHT
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SetupFragment : Fragment(R.layout.fragment_setup) {

    private var _binding: FragmentSetupBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var sharedPref : SharedPreferences

    @set:Inject
    var isFirstAppOpen = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(!isFirstAppOpen){
            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.setupFragment, true)
                .build()
            findNavController().navigate(
                R.id.action_setupFragment_to_runFragment,
                savedInstanceState,
                navOptions
            )
        }

        _binding = FragmentSetupBinding.bind(view)


        _binding?.btnContinue?.setOnClickListener {
            val success = writePersonalDataToSharedPref()
            if(success){
                findNavController().navigate(R.id.action_setupFragment_to_runFragment)
            }
            else{
                Snackbar.make(requireView(), "Please fill all the Fields", Snackbar.LENGTH_SHORT).show()
            }

        }
    }

    private fun writePersonalDataToSharedPref() : Boolean{
        val name = _binding?.etName?.text.toString()
        val weight = _binding?.etWeight?.text.toString()
        if(name.isEmpty() || weight.isEmpty()){
            return false
        }

        sharedPref.edit()
            .putString(KEY_NAME, name)
            .putFloat(KEY_WEIGHT, weight.toFloat())
            .putBoolean(KEY_FIRST_TIME_TOGGLE, false)
            .apply()

        //modify the toolbar text
        val toolbarText = "Let's go $name"
        val activityRootView = requireActivity().window.decorView.rootView
        val toolbarTitleTextView = activityRootView.findViewById<TextView>(R.id.tvToolbarTitle)
        toolbarTitleTextView?.text = toolbarText
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear the binding when the view is destroyed
        _binding = null
    }
}