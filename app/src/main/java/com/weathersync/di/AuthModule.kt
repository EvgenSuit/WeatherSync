package com.weathersync.di

import android.content.Context
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.weathersync.BuildConfig
import com.weathersync.features.auth.GoogleAuthRepository
import com.weathersync.features.auth.RegularAuthRepository
import com.weathersync.features.auth.presentation.AuthViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module


private fun getOneTapClient(context: Context): SignInClient = Identity.getSignInClient(context)
private fun getSignInRequest() = BeginSignInRequest.Builder()
    .setGoogleIdTokenRequestOptions(
        BeginSignInRequest
            .GoogleIdTokenRequestOptions.builder()
            .setSupported(true)
            .setServerClientId(BuildConfig.WEB_CLIENT_ID)
            .setFilterByAuthorizedAccounts(false)
    .build())
    .setAutoSelectEnabled(true).build()
val authModule = module {
    factory { AuthViewModel(
        regularAuthRepository = get(),
        googleAuthRepository = get(),
        crashlyticsManager = get()) }
    factory { RegularAuthRepository(Firebase.auth) }
    factory { GoogleAuthRepository(
        auth = Firebase.auth,
        oneTapClient = getOneTapClient(androidContext()),
        beginSignInRequest = getSignInRequest()) }
}