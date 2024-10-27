package com.weathersync.features.settings.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

typealias Dark = Boolean

val Context.themeDatastore: DataStore<Preferences> by preferencesDataStore("settingsDataStore")
val isThemeDarkKey = booleanPreferencesKey("isThemeDark")

class ThemeManager(
    private val dataStore: DataStore<Preferences>
) {
    suspend fun setTheme(dark: Dark) =
        dataStore.edit { theme ->
            theme[isThemeDarkKey] = dark
        }
    fun themeFlow(isDarkByDefault: Dark): Flow<Dark> =
        dataStore.data.map { theme -> theme[isThemeDarkKey] ?: isDarkByDefault }
}