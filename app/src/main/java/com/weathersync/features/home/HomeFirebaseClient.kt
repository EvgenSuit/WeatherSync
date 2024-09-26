package com.weathersync.features.home

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeFirebaseClient(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
}