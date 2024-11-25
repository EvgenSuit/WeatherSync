package com.weathersync.di

import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.weathersync.features.navigation.presentation.ui.NavManagerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module

val navModule = module {
    single { NavManagerViewModel(
        auth = Firebase.auth,
        subscriptionInfoDatastore = get(),
        themeManager = get()
    ) }
}