package com.weathersync.features.home

import com.weathersync.common.home.mockEngine
import com.weathersync.common.home.mockLocationClient
import com.weathersync.common.utils.mockCrashlyticsManager
import com.weathersync.features.home.presentation.HomeViewModel
import io.ktor.http.HttpStatusCode
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class HomeBaseRule: TestWatcher() {
    val crashlyticsExceptionSlot = slot<Exception>()
    val exception = Exception("exception")
    val snackbarScope = TestScope()
    val crashlyticsManager = mockCrashlyticsManager(exceptionSlot = crashlyticsExceptionSlot)
    lateinit var viewModel: HomeViewModel
    fun advanceKtor(testScope: TestScope) = repeat(9999999) { testScope.advanceUntilIdle() }

    fun setup(
        status: HttpStatusCode = HttpStatusCode.OK,
        geocoderException: Exception? = null,
        lastLocationException: Exception? = null
    ) {
        val weatherRepository = WeatherRepository(
            engine = mockEngine(status),
            locationClient = mockLocationClient(
                geocoderException = geocoderException,
                lastLocationException = lastLocationException
            )
        )
        val homeRepository = HomeRepository(
            homeFirebaseClient = mockk(),
            weatherRepository = weatherRepository
        )
        viewModel = HomeViewModel(
            homeRepository = homeRepository,
            crashlyticsManager = crashlyticsManager
        )
    }

    override fun starting(description: Description?) {
        setup()
    }
}