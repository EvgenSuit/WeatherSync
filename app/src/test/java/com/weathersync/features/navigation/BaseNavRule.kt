package com.weathersync.features.navigation

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.auth.FirebaseAuth
import com.weathersync.common.TestHelper
import com.weathersync.common.auth.mockAuth
import com.weathersync.features.activityPlanning.presentation.ActivityPlanningViewModel
import com.weathersync.features.auth.RegularAuthRepository
import com.weathersync.features.auth.presentation.AuthViewModel
import com.weathersync.features.home.presentation.HomeViewModel
import com.weathersync.features.navigation.presentation.ui.NavManagerViewModel
import com.weathersync.features.settings.SettingsRepository
import com.weathersync.features.settings.data.ThemeManager
import com.weathersync.features.settings.data.themeDatastore
import com.weathersync.features.settings.presentation.SettingsViewModel
import com.weathersync.utils.LimitManager
import com.weathersync.utils.WeatherUnitsManager
import io.mockk.mockk
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class BaseNavRule: TestWatcher() {
    val testHelper = TestHelper()
    lateinit var auth: FirebaseAuth
    lateinit var viewModel: NavManagerViewModel

    fun setupKoin(
        inputAuth: FirebaseAuth = mockAuth()
    ) {
        auth = inputAuth
        val authModule = module {
            factory { AuthViewModel(
                regularAuthRepository = RegularAuthRepository(inputAuth),
                googleAuthRepository = mockk(relaxed = true),
                crashlyticsManager = mockk(relaxed = true)
            ) }
        }
        val homeModule = module {
            factory { HomeViewModel(
                homeRepository = mockk(relaxed = true),
                crashlyticsManager = mockk(relaxed = true)
            ) }
        }
        val utilsModule = module {
            factory { mockk<LimitManager>(relaxed = true) }
            factory { mockk<WeatherUnitsManager>(relaxed = true) }
        }
        val activityPlanningModule = module {
            factory { ActivityPlanningViewModel(
                activityPlanningRepository = mockk(relaxed = true),
                crashlyticsManager = mockk(relaxed = true)
            ) }
        }
        val settingsModule = module {
            factory { SettingsViewModel(
                settingsRepository = SettingsRepository(auth = auth, themeManager = get(), weatherUnitsManager = mockk(relaxed = true)),
                crashlyticsManager = mockk(relaxed = true)
            ) }
            single { ThemeManager(dataStore = ApplicationProvider.getApplicationContext<Context>().themeDatastore) }
        }
        val navModule = module {
            factory { viewModel }
        }
        startKoin {
            modules(authModule,
                utilsModule,
                homeModule,
                activityPlanningModule,
                settingsModule,
                navModule)
        }
    }

    fun setupViewModel() {
        viewModel = NavManagerViewModel(auth = auth)
    }

    override fun starting(description: Description?) {
        stopKoin()
        setupKoin()
        setupViewModel()
    }
}