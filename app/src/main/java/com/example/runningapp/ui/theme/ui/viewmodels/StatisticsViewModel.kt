package com.example.runningapp.ui.theme.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.example.runningapp.repositories.MainRepository
import dagger.assisted.AssistedInject

class StatisticsViewModel @AssistedInject constructor(
    val mainRepository: MainRepository
) : ViewModel() {
}