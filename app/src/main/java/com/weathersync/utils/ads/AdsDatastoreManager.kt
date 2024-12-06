package com.weathersync.utils.ads

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map


val Context.adsDataStore by preferencesDataStore("ads_dataStore")
private val showInterstitialAdKey = booleanPreferencesKey("show_interstitial_ad")
class AdsDatastoreManager(
    private val dataStore: DataStore<Preferences>
) {
    fun showInterstitialAdFlow() = dataStore.data.map { it[showInterstitialAdKey] ?: false }
    suspend fun setShowInterstitialAd(showAd: Boolean) {
        dataStore.edit { it[showInterstitialAdKey] = showAd }
    }
}