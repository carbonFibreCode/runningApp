package com.example.runningapp.ui.theme.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.runningapp.R
import com.example.runningapp.databinding.FragmentSetupBinding

class SetupFragment : Fragment(R.layout.fragment_setup) {

    private var _binding: FragmentSetupBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentSetupBinding.bind(view)


        _binding?.tvContinue?.setOnClickListener {
            findNavController().navigate(R.id.action_setupFragment_to_runFragment)
        }


    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear the binding when the view is destroyed
        _binding = null
    }
}