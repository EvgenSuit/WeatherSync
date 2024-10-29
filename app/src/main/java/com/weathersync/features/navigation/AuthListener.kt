package com.weathersync.features.navigation

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

class AuthListener(
    val auth: FirebaseAuth
) {
    fun isUserNullFlow() = callbackFlow {
        val authListener = FirebaseAuth.AuthStateListener { emittedAuth ->
            trySend(emittedAuth.currentUser == null)
        }
        auth.addAuthStateListener(authListener)
        awaitClose {
            auth.removeAuthStateListener(authListener)
        }
    }
}