package com.weathersync.features.settings.viewModel

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.weathersync.common.utils.MainDispatcherRule
import com.weathersync.features.settings.SettingsBaseRule
import com.weathersync.features.settings.presentation.ui.SettingsIntent
import io.mockk.coVerify
import io.mockk.coVerifyAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsViewModelThemeTests {
    @get: Rule(order = 0)
    val dispatcherRule = MainDispatcherRule()

    @get: Rule(order = 1)
    val settingsBaseRule = SettingsBaseRule()

    @Test
    fun collectDefaultTheme_isCorrect() = runTest {
        settingsBaseRule.viewModel.uiState.test {
            skipItems(1)
            assertEquals(true, awaitItem().isThemeDark)
        }
    }

    @Test
    fun setTheme_isCorrect() = runTest {
        settingsBaseRule.viewModel.apply {
            uiState.test {
                skipItems(1)
                assertEquals(true, awaitItem().isThemeDark)
                for (dark in listOf(false, true)) {
                    // set theme to light on the first run
                    handleIntent(SettingsIntent.SwitchTheme)
                    repeat(9999999) {
                        advanceUntilIdle()
                    }
                    coVerify { settingsBaseRule.settingsRepository.setTheme(dark) }
                    assertEquals(dark, awaitItem().isThemeDark!!)
                }
            }
        }
    }
}