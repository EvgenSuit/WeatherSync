package com.weathersync.utils

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.setCustomKeys
import kotlin.coroutines.cancellation.CancellationException

class CrashlyticsManager(
    private val auth: FirebaseAuth,
    private val crashlytics: FirebaseCrashlytics,
    private val analytics: FirebaseAnalytics,
) {
    init {
        crashlytics.isCrashlyticsCollectionEnabled = true
    }
    fun log(vararg info: Any) {
        val infoString = info.joinToString(separator = " | ") { element ->
            when (element) {
                is Array<*> -> element.joinToString() // Convert nested arrays to a string
                else -> element.toString()
            }
        }
        crashlytics.log(infoString)
    }
    fun logEvent(event: String, bundle: Bundle) = analytics.logEvent(event, bundle)
    fun recordException(e: Exception, vararg info: Any) {
        if (e is CancellationException) return
        crashlytics.apply {
            setCustomKeys {
                key("user_id", auth.currentUser?.uid.toString())
            }
            log(info)
            recordException(e)
        }
    }
}