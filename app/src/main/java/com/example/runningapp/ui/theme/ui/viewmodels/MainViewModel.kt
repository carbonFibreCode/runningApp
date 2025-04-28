package com.example.runningapp.ui.theme.ui.viewmodels

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runningapp.db.Run
import com.example.runningapp.other.SortType
import com.example.runningapp.repositories.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject  constructor(
    val mainRepository: MainRepository //we don't need to create a different method for repo in appmodule as it only depends on rundao and hilt knows how to create thje dao instance and hence also repository instane
) : ViewModel() {

    private val runsSortedByDate = mainRepository.getAllRunsSortedByDate()
    private val runsSortedByDistance = mainRepository.getAllRunsSortedByDistance()
    private val runsSortedByCaloriesBurned = mainRepository.getAllRunsSortedByCaloriesBurned()
    private val runsSortedByTimeInMillis = mainRepository.getAllRunsSortedByTimeInMillis()
    private val runsSortedByAvgSpeed = mainRepository.getAllRunsSortedByAvgSpeed()

    fun insertRun(run: Run) = viewModelScope.launch {
        mainRepository.insertRun(run)
    }

    //we will create the mediator live data to observe on the basis of the sort selected in spinner drop down menu.

    val runs = MediatorLiveData<List<Run>>()

    var sortType = SortType.DATE //this will be our default sort type BUT we'll need to create a functionality to observe changes on sortType

    init {
        runs.addSource(runsSortedByDate){ //we will observe runs in the run fragment, the lambda callback will be invoked every time there's a change in runsSortedByDate
            result ->
            if(sortType == SortType.DATE){
                result?.let {
                    runs.value = it
                }
            }
        }

        runs.addSource(runsSortedByTimeInMillis){ //we will observe runs in the run fragment, the lambda callback will be invoked every time there's a change in runsSortedByDate
                result ->
            if(sortType == SortType.RUNNING_TIME){
                result?.let {
                    runs.value = it
                }
            }
        }

        runs.addSource(runsSortedByDistance){ //we will observe runs in the run fragment, the lambda callback will be invoked every time there's a change in runsSortedByDate
                result ->
            if(sortType == SortType.DISTANCE){
                result?.let {
                    runs.value = it
                }
            }
        }

        runs.addSource(runsSortedByAvgSpeed){ //we will observe runs in the run fragment, the lambda callback will be invoked every time there's a change in runsSortedByDate
                result ->
            if(sortType == SortType.AVG_SPEED){
                result?.let {
                    runs.value = it
                }
            }
        }

        runs.addSource(runsSortedByCaloriesBurned){ //we will observe runs in the run fragment, the lambda callback will be invoked every time there's a change in runsSortedByDate
                result ->
            if(sortType == SortType.CALORIES_BURNED){
                result?.let {
                    runs.value = it
                }
            }
        }
    }

    fun sortRuns(sortType : SortType) = when(sortType){
        SortType.DATE -> runsSortedByDate.value?.let { runs.value = it }
        SortType.RUNNING_TIME -> runsSortedByTimeInMillis.value?.let { runs.value = it }
        SortType.DISTANCE -> runsSortedByDistance.value?.let { runs.value = it }
        SortType.AVG_SPEED -> runsSortedByAvgSpeed.value?.let { runs.value = it }
        SortType.CALORIES_BURNED -> runsSortedByCaloriesBurned.value?.let { runs.value = it }
    }.also {
        this.sortType = sortType
    }
}