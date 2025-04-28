package com.example.runningapp.ui.theme.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.example.runningapp.repositories.MainRepository
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    val mainRepository: MainRepository
) : ViewModel() {

    val totalTimeRun = mainRepository.getTotalTimeInMillis()
    val totalDistance = mainRepository.getTotalDistance()
    val totalCaloriesBurned = mainRepository.getTotalCaloriesBurned()
    val totalAvgSpeed = mainRepository.getTotalAvgSpeed()

    val runsSortedByDate = mainRepository.getAllRunsSortedByDate()
    val runsSortedByDistance = mainRepository.getAllRunsSortedByDistance()
    val runsSortedByTime = mainRepository.getAllRunsSortedByTimeInMillis()
    val runsSortedByCaloriesBurned = mainRepository.getAllRunsSortedByCaloriesBurned()

}