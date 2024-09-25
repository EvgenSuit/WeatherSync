package com.weathersync.features.auth

import android.content.Intent
import android.content.IntentSender
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class GoogleAuthRepository(
    private val auth: FirebaseAuth,
    private val oneTapClient: SignInClient,
    private val beginSignInRequest: BeginSignInRequest
) {
    suspend fun onTapSignIn(): IntentSender? =
        oneTapClient.beginSignIn(beginSignInRequest).await()?.pendingIntent?.intentSender

    suspend fun signInWithIntent(intent: Intent) {
        val googleIdToken = oneTapClient.getSignInCredentialFromIntent(intent).googleIdToken
        val googleCredentials = GoogleAuthProvider.getCredential(googleIdToken, null)
        auth.signInWithCredential(googleCredentials).await()
    }
}