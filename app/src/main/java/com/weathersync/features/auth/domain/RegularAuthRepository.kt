package com.weathersync.features.auth.domain

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class RegularAuthRepository(
    private val auth: FirebaseAuth
) {
    suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }
    suspend fun signUp(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password).await()
    }
}