package com.weathersync.features.settings.viewModel

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.weathersync.common.MainDispatcherRule
import com.weathersync.features.settings.SettingsBaseRule
import com.weathersync.features.settings.ThemeTest
import com.weathersync.features.settings.presentation.ui.SettingsIntent
import io.mockk.coVerify
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsViewModelThemeTests: ThemeTest {
    @get: Rule(order = 0)
    val dispatcherRule = MainDispatcherRule()

    @get: Rule(order = 1)
    val settingsBaseRule = SettingsBaseRule()

    @Test
    override fun collectDefaultTheme_isCorrect() = runTest {
        settingsBaseRule.viewModel.themeState.test {
            // skip 1 items since the first item emitted is the default state
            skipItems(1)
            assertEquals(true, awaitItem())
        }
    }

    @Test
    override fun setTheme_isCorrect() = runTest {
        settingsBaseRule.viewModel.apply {
            themeState.test {
                skipItems(1)
                assertEquals(true, awaitItem())
                for (dark in listOf(false, true)) {
                    // set theme to light on the first run
                    handleIntent(SettingsIntent.SwitchTheme)
                    advanceUntilIdle()
                    coVerify { settingsBaseRule.settingsRepository.setTheme(dark) }
                    assertEquals(dark, awaitItem()!!)
                }
            }
        }
    }
}