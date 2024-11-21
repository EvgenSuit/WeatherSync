package com.weathersync.features.navigation.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
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
): ViewModel() {
    val isUserNullInit = auth.currentUser == null
    val isUserSubscribedFlow = subscriptionInfoDatastore.isSubscribedFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)
}