package com.example.runningapp.ui.theme.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.example.runningapp.ui.theme.ui.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.example.runningapp.R
import com.example.runningapp.databinding.FragmentRunBinding

@AndroidEntryPoint
class RunFragment : Fragment(R.layout.fragment_run) {
    private val viewModel : MainViewModel by viewModels()
    private var _binding : FragmentRunBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentRunBinding.bind(view)

        _binding?.fab?.setOnClickListener {
            findNavController().navigate(R.id.action_runFragment_to_trackingFragment)
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear the binding
        _binding = null
    }
}