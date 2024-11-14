package com.weathersync.utils.ads

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.weathersync.utils.FirebaseEvent
import com.weathersync.utils.subscription.IsSubscribed
import kotlinx.coroutines.flow.map


val Context.adsDataStore by preferencesDataStore("ads_dataStore")
private val showInterstitialAd = booleanPreferencesKey("show_interstitial_ad")
class AdsDatastoreManager(
    private val dataStore: DataStore<Preferences>
) {
    fun showInterstitialAdFlow() = dataStore.data.map { it[showInterstitialAd] ?: false }
    suspend fun setShowInterstitialAd(event: FirebaseEvent? = null, isSubscribed: IsSubscribed? = null) {
        // don't show ads if subscribed to premium
        val showAd = listOf(FirebaseEvent.FETCH_CURRENT_WEATHER,
            FirebaseEvent.PLAN_ACTIVITIES).contains(event) && isSubscribed != null && !isSubscribed
        dataStore.edit { it[showInterstitialAd] = showAd }
    }
}