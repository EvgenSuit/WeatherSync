package com.weathersync.features.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.weathersync.common.TestHelper
import com.weathersync.common.auth.mockAuth
import com.weathersync.common.utils.mockWeatherUnitsManager
import com.weathersync.features.settings.data.ThemeManager
import com.weathersync.features.settings.data.themeDatastore
import com.weathersync.features.settings.presentation.SettingsViewModel
import com.weathersync.utils.WeatherUnitsManager
import io.mockk.spyk
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.koin.core.context.stopKoin

class SettingsBaseRule: TestWatcher() {
    val auth = mockAuth()
    lateinit var viewModel: SettingsViewModel
    lateinit var settingsRepository: SettingsRepository
    lateinit var weatherUnitsManager: WeatherUnitsManager
    val testHelper = TestHelper()

    fun setupViewModel() {
        viewModel = SettingsViewModel(
            settingsRepository = settingsRepository,
            crashlyticsManager = testHelper.crashlyticsManager
        )
    }
    fun setupRepository() {
        settingsRepository = spyk(
            SettingsRepository(
                auth = auth,
            themeManager = ThemeManager(dataStore = ApplicationProvider.getApplicationContext<Context>()
                .themeDatastore),
                weatherUnitsManager = weatherUnitsManager
        ))
    }
    fun setupWeatherUnitsManager(
        unitsFetchException: Exception? = null,
        unitSetException: Exception? = null
    ) {
        weatherUnitsManager = mockWeatherUnitsManager(
            unitsFetchException = unitsFetchException,
            unitSetException = unitSetException
        )
    }

    override fun starting(description: Description) {
        stopKoin()
        setupWeatherUnitsManager()
        setupRepository()
        setupViewModel()
    }
}