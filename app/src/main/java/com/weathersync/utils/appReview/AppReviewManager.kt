package com.weathersync.utils.appReview

import com.google.android.play.core.review.ReviewManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.weathersync.MainActivity
import com.weathersync.utils.TimeAPI
import com.weathersync.utils.UnknownReviewException
import com.weathersync.utils.appReview.data.RateDialog
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.tasks.await
import java.time.Duration
import java.util.Date
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

enum class AppReviewFirestoreRef(val refName: String) {
    SHOW_RATE_DIALOG("showRateDialog")
}

class AppReviewManager(
    private val timeAPI: TimeAPI,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val manager: ReviewManager
) {
    private suspend fun authListener() = callbackFlow {
        val listener = AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose {
            auth.removeAuthStateListener(listener)
        }
    }
    suspend fun requestReviewFlow(activity: MainActivity) {
        authListener().collectLatest { currUser ->
            if (currUser != null) {
                val ref = firestore.collection(currUser.uid).document(AppReviewFirestoreRef.SHOW_RATE_DIALOG.refName)
                val currTime = timeAPI.getRealDateTime()
                if (ref.isEligible(currTime)) {
                    launchFlow(activity = activity, ref = ref)
                }
            }
        }
    }

    private suspend fun launchFlow(activity: MainActivity, ref: DocumentReference) {
        val request = manager.requestReviewFlow()
        val reviewInfo = suspendCoroutine { continuation ->
            request.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    continuation.resume(task.result)
                } else {
                    continuation.resumeWithException(task.exception ?: UnknownReviewException())
                }
            }
        }
        reviewInfo?.let {
            val flow = manager.launchReviewFlow(activity, it)
            suspendCoroutine { continuation ->
                flow.addOnCompleteListener {
                    continuation.resume(Unit)
                }
            }
            // we don't care about firstEntryDate anymore
           ref.set(RateDialog(firstEntryDate = null, didShow = true)).await()
        }
    }

    private suspend fun DocumentReference.isEligible(currTime: Date): Boolean {
        val rateDialog = this.get().await().toObject(RateDialog::class.java)
        if (rateDialog != null && rateDialog.didShow) return false
        if (rateDialog?.firstEntryDate == null) {
            this.set(RateDialog(currTime))
            return false
        }
        // check if the duration between firstEntryDate and currTime exceeds n days
        val before = currTime.toInstant().minus(Duration.ofDays(2))
        return rateDialog.firstEntryDate.toInstant().isBefore(before)
    }
}