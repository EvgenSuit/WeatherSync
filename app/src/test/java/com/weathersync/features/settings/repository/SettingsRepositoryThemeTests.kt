package com.weathersync.features.settings.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.weathersync.features.settings.SettingsBaseRule
import com.weathersync.features.settings.data.Dark
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsRepositoryThemeTests {
    @get: Rule
    val settingsBaseRule = SettingsBaseRule()

    @Test
    fun collectDefaultTheme_isCorrect() = runTest {
        assertEquals(true, settingsBaseRule.settingsRepository.themeFlow(true).first())
    }
    @Test
    fun setTheme_isCorrect() = runTest {
        for (dark in listOf(false, true)) {
            settingsBaseRule.settingsRepository.setTheme(dark = dark)
            assertEquals(dark, settingsBaseRule.settingsRepository.themeFlow(true).first())
        }
    }

}