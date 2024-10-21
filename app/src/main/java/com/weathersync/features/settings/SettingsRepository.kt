package com.weathersync.features.settings

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.weathersync.features.settings.data.Dark
import com.weathersync.features.settings.data.ThemeManager

class SettingsRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val themeManager: ThemeManager
) {
    suspend fun setTheme(dark: Dark) = themeManager.setTheme(dark)
    fun themeFlow(isDarkByDefault: Dark) = themeManager.themeFlow(isDarkByDefault)
}