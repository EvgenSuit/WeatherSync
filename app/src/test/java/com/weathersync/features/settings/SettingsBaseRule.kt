package com.weathersync.features.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.weathersync.common.TestHelper
import com.weathersync.common.auth.mockAuth
import com.weathersync.common.utils.location.mockLocationManager
import com.weathersync.common.utils.mockLimitManager
import com.weathersync.common.utils.mockLimitManagerFirestore
import com.weathersync.common.utils.mockSubscriptionManager
import com.weathersync.common.utils.mockTimeAPI
import com.weathersync.common.weather.mockAndroidLocationClient
import com.weathersync.common.weather.mockWeatherUnitsManager
import com.weathersync.features.home.domain.WeatherUpdater
import com.weathersync.features.settings.data.ThemeManager
import com.weathersync.features.settings.data.themeDatastore
import com.weathersync.features.settings.presentation.SettingsViewModel
import com.weathersync.utils.subscription.IsSubscribed
import com.weathersync.utils.subscription.data.SubscriptionInfoDatastore
import com.weathersync.utils.subscription.data.subscriptionInfoDatastore
import com.weathersync.utils.weather.WeatherUnitsManager
import com.weathersync.utils.weather.limits.LimitManager
import com.weathersync.utils.weather.limits.NextUpdateTimeFormatter
import com.weathersync.utils.weather.location.LocationManager
import io.ktor.http.HttpStatusCode
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.koin.core.context.stopKoin
import java.util.Locale

class SettingsBaseRule: TestWatcher() {
    val testDispatcher = StandardTestDispatcher()
    lateinit var auth: FirebaseAuth
    lateinit var viewModel: SettingsViewModel
    lateinit var settingsRepository: SettingsRepository
    lateinit var limitManagerFirestore: FirebaseFirestore
    lateinit var limitManager: LimitManager
    lateinit var weatherUnitsManager: WeatherUnitsManager
    lateinit var subscriptionInfoDatastore: SubscriptionInfoDatastore
    lateinit var locationManager: LocationManager
    val testHelper = TestHelper()

    fun setupViewModel(locale: Locale = Locale.US) {
        subscriptionInfoDatastore = SubscriptionInfoDatastore(ApplicationProvider.getApplicationContext<Context>().subscriptionInfoDatastore)
        viewModel = SettingsViewModel(
            settingsRepository = settingsRepository,
            nextUpdateTimeFormatter = NextUpdateTimeFormatter(
                clock = testHelper.testClock,
                locale = locale
            ),
            subscriptionInfoDatastore = subscriptionInfoDatastore,
            analyticsManager = testHelper.getAnalyticsManager(mockk(relaxed = true))
        )
    }
    fun setupLimitManager(
        timestamps: List<Timestamp> = listOf(),
        timeApiStatusCode: HttpStatusCode = HttpStatusCode.OK,
        exception: Exception? = null) {
        limitManagerFirestore = mockLimitManagerFirestore(
            timestamps = timestamps,
            exception = exception
        )
        limitManager = spyk(mockLimitManager(
            limitManagerFirestore = limitManagerFirestore,
            currentWeatherDAO = mockk(),
            weatherUpdater = mockk(),
            timeAPI = mockTimeAPI(statusCode = timeApiStatusCode, currTimeMillis = testHelper.testClock.millis() )
        ))
    }
    fun setupRepository(
        isSubscribed: IsSubscribed = false,
        inputAuth: FirebaseAuth = mockAuth(),
        areLocationManagerResultsEmpty: Boolean = false,
        geocoderException: Exception? = null,
        lastLocationException: Exception? = null,
        locationManagerException: Exception? = null,
    ) {
        auth = inputAuth
        locationManager = mockLocationManager(
            areLocationManagerResultsEmpty = areLocationManagerResultsEmpty,
            locationManagerException = locationManagerException,
            androidLocationClient = mockAndroidLocationClient(
                geocoderException = geocoderException,
                lastLocationException = lastLocationException
            ),
            dispatcher = testDispatcher
        )
        settingsRepository = spyk(
            SettingsRepository(
                auth = auth,
            themeManager = ThemeManager(
                dataStore = ApplicationProvider.getApplicationContext<Context>()
                .themeDatastore),
                subscriptionManager = mockSubscriptionManager(isSubscribed),
                limitManager = limitManager,
                locationManager = locationManager,
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
        unmockkAll()
        setupWeatherUnitsManager()
        setupLimitManager()
        setupRepository()
        setupViewModel()
    }
}