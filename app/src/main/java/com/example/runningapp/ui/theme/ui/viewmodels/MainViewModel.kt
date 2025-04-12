package com.example.runningapp.ui.theme.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.example.runningapp.repositories.MainRepository
import dagger.assisted.AssistedInject
import javax.inject.Inject

class MainViewModel @AssistedInject constructor(
    val mainRepository: MainRepository //we don't need to create a different method for repo in appmodule as it only depends on rundao and hilt knows how to create thje dao instance and hence also repository instane
) : ViewModel() {
}