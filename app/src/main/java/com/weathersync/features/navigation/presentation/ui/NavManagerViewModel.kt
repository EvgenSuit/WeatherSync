package com.weathersync.features.navigation.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weathersync.features.navigation.AuthListener
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class NavManagerViewModel(
    authListener: AuthListener
): ViewModel() {
    val isUserNullInit = authListener.auth.currentUser == null
    val isUserNullFlow = authListener.isUserNullFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)
}