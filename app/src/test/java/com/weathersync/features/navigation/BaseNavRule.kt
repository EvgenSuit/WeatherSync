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
import com.weathersync.features.subscription.SubscriptionInfoRepository
import com.weathersync.features.subscription.presentation.SubscriptionInfoViewModel
import com.weathersync.utils.subscription.SubscriptionManager
import com.weathersync.utils.subscription.data.OfferDetails
import com.weathersync.utils.subscription.data.PricingPhaseDetails
import com.weathersync.utils.subscription.data.SubscriptionDetails
import com.weathersync.utils.subscription.data.SubscriptionInfoDatastore
import com.weathersync.utils.subscription.data.subscriptionInfoDatastore
import com.weathersync.utils.weather.LimitManager
import com.weathersync.utils.weather.WeatherUnitsManager
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class BaseNavRule: TestWatcher() {
    val testHelper = TestHelper()
    val testDispatcher = StandardTestDispatcher()
    lateinit var auth: FirebaseAuth
    lateinit var viewModel: NavManagerViewModel
    val subscriptionInfoDatastore = SubscriptionInfoDatastore(
        dataStore = ApplicationProvider.getApplicationContext<Context>().subscriptionInfoDatastore
    )

    fun setupKoin(
        inputAuth: FirebaseAuth = mockAuth()
    ) {
        auth = inputAuth
        val subscriptionModule = module {
            factory { SubscriptionInfoViewModel(
                subscriptionInfoRepository = spyk(SubscriptionInfoRepository(
                    subscriptionManager = SubscriptionManager(
                        billingClientBuilder = mockk(relaxed = true),
                        subscriptionInfoDatastore = subscriptionInfoDatastore,
                        analyticsManager = mockk()
                    )
                )) {
                    coEvery { isBillingSetupFinished() } returns true
                    coEvery { getSubscriptionDetails() } returns listOf(
                        mockk(relaxed = true)
                    )
                },
                analyticsManager = mockk()
            ) }
        }
        val authModule = module {
            factory { AuthViewModel(
                regularAuthRepository = RegularAuthRepository(inputAuth),
                googleAuthRepository = mockk(relaxed = true),
                analyticsManager = mockk(relaxed = true)
            ) }
        }
        val homeModule = module {
            factory { HomeViewModel(
                homeRepository = mockk(relaxed = true),
                analyticsManager = mockk(relaxed = true),
                nextUpdateTimeFormatter = mockk(relaxed = true)
            ) }
        }
        val utilsModule = module {
            factory { mockk<LimitManager>(relaxed = true) }
            factory { mockk<WeatherUnitsManager>(relaxed = true) }
        }
        val activityPlanningModule = module {
            factory { ActivityPlanningViewModel(
                activityPlanningRepository = mockk(relaxed = true),
                analyticsManager = mockk(relaxed = true),
                nextUpdateTimeFormatter = mockk(relaxed = true)
            ) }
        }
        val settingsModule = module {
            factory { SettingsViewModel(
                settingsRepository = SettingsRepository(auth = auth, themeManager = get(), weatherUnitsManager = mockk(relaxed = true)),
                analyticsManager = mockk(relaxed = true)
            ) }
            single { ThemeManager(dataStore = ApplicationProvider.getApplicationContext<Context>().themeDatastore) }
        }
        val navModule = module {
            factory { viewModel }
        }
        startKoin {
            modules(
                subscriptionModule,
                authModule,
                utilsModule,
                homeModule,
                activityPlanningModule,
                settingsModule,
                navModule)
        }
    }

    fun setupViewModel() {
        viewModel = NavManagerViewModel(
            auth = auth,
            subscriptionInfoDatastore = subscriptionInfoDatastore
        )
    }

    override fun starting(description: Description?) {
        stopKoin()
        setupKoin()
        setupViewModel()
    }
}