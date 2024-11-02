package com.weathersync.features.navigation.presentation.ui

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth

class NavManagerViewModel(
    auth: FirebaseAuth
): ViewModel() {
    val isUserNullInit = auth.currentUser == null
}