package com.weathersync.utils.subscription.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.weathersync.utils.subscription.IsSubscribed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.subscriptionInfoDatastore by preferencesDataStore("subscription_info")
private val isSubscribedKey = booleanPreferencesKey("is_subscribed")
class SubscriptionInfoDatastore(
    private val dataStore: DataStore<Preferences>
) {
    suspend fun setIsSubscribed(isSubscribed: IsSubscribed) =
        dataStore.edit {
            it[isSubscribedKey] = isSubscribed
        }
    fun isSubscribedFlow(): Flow<IsSubscribed?> =
        dataStore.data.map { it[isSubscribedKey] }
}