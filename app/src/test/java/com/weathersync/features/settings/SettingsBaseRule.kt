package com.weathersync.features.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.weathersync.common.auth.mockAuth
import com.weathersync.common.utils.mockCrashlyticsManager
import com.weathersync.features.settings.data.ThemeManager
import com.weathersync.features.settings.data.themeDatastore
import com.weathersync.features.settings.presentation.SettingsViewModel
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.koin.core.context.stopKoin

class SettingsBaseRule: TestWatcher() {
    lateinit var viewModel: SettingsViewModel
    lateinit var settingsRepository: SettingsRepository
    fun advance(scope: TestScope) = repeat(9999999) {
        scope.advanceUntilIdle()
    }

    fun setupViewModel() {
        viewModel = SettingsViewModel(
            settingsRepository = settingsRepository,
            crashlyticsManager = mockCrashlyticsManager()
        )
    }
    fun setupRepository() {
        settingsRepository = spyk(SettingsRepository(
            auth = mockAuth(),
            firestore = mockk(),
            themeManager = ThemeManager(dataStore = ApplicationProvider.getApplicationContext<Context>()
                .themeDatastore)
        ))
    }

    override fun starting(description: Description?) {
        stopKoin()
        setupRepository()
        setupViewModel()
    }
}