package com.weathersync.features.navigation.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.weathersync.features.settings.data.ThemeManager
import com.weathersync.utils.subscription.data.SubscriptionInfoDatastore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class NavManagerViewModel(
    auth: FirebaseAuth,
    subscriptionInfoDatastore: SubscriptionInfoDatastore,
    themeManager: ThemeManager
): ViewModel() {
    val isUserNullInit = auth.currentUser == null
    val isUserSubscribedFlow = subscriptionInfoDatastore.isSubscribedFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)
    val isThemeDark = themeManager.themeFlow(true)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)
}