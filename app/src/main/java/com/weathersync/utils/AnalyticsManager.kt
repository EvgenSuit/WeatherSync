package com.weathersync.utils

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.setCustomKeys
import com.weathersync.utils.ads.AdsDatastoreManager
import com.weathersync.utils.subscription.IsSubscribed
import kotlin.coroutines.cancellation.CancellationException

enum class FirebaseEvent {
    MANUAL_SIGN_UP,
    MANUAL_SIGN_IN,
    SIGN_IN_WITH_GOOGLE,

    CURRENT_WEATHER_FETCH_LIMIT,
    FETCH_CURRENT_WEATHER,
    GENERATE_SUGGESTIONS,

    ACTIVITY_PLANNING_LIMIT,
    PLAN_ACTIVITIES,

    FETCH_WEATHER_UNITS,
    CHANGE_WEATHER_UNITS,

    SIGN_OUT,

    NONE
}

class AnalyticsManager(
    private val auth: FirebaseAuth,
    private val crashlytics: FirebaseCrashlytics,
    private val analytics: FirebaseAnalytics,
    private val adsDatastoreManager: AdsDatastoreManager
) {
    init {
        crashlytics.isCrashlyticsCollectionEnabled = true
    }
    private fun log(vararg info: Any) {
        val infoString = info.joinToString(separator = " | ") { element ->
            when (element) {
                is Array<*> -> element.joinToString() // Convert nested arrays to a string
                else -> element.toString()
            }
        }
        crashlytics.log(infoString)
    }

    suspend fun logEvent(event: FirebaseEvent,
                         showInterstitialAd: Boolean? = null,
                         vararg params: Pair<String, String>) {
        if (showInterstitialAd != null) adsDatastoreManager.setShowInterstitialAd(showAd = showInterstitialAd)
        analytics.logEvent(event.name.lowercase()) {
            param("user_id", auth.currentUser?.uid.toString())
            params.forEach { param(it.first, it.second) }
        }
    }

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