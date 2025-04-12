package com.example.runningapp.ui.theme.ui.fragments

import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.runningapp.R
import com.example.runningapp.ui.theme.ui.viewmodels.MainViewModel
import com.example.runningapp.ui.theme.ui.viewmodels.StatisticsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlin.getValue

@AndroidEntryPoint
class StatisticsFragment : Fragment() {
    private val viewModel : StatisticsViewModel by viewModels()
}