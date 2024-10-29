package com.weathersync.di

import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.weathersync.features.navigation.AuthListener
import com.weathersync.features.navigation.presentation.ui.NavManagerViewModel
import org.koin.dsl.module

val navModule = module {
    single { NavManagerViewModel(authListener = get()) }
    single { AuthListener(auth = Firebase.auth) }
}