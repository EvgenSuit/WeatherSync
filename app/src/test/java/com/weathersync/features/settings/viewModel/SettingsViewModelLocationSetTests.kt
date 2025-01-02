package com.weathersync.features.settings.viewModel

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.weathersync.R
import com.weathersync.common.MainDispatcherRule
import com.weathersync.common.TestException
import com.weathersync.common.ui.UIText
import com.weathersync.common.utils.createDescendingTimestamps
import com.weathersync.common.weather.LocationInfo
import com.weathersync.common.weather.fullLocation
import com.weathersync.common.weather.locationInfo
import com.weathersync.features.settings.SettingsBaseRule
import com.weathersync.features.settings.presentation.ui.SettingsIntent
import com.weathersync.ui.SettingsUIEvent
import com.weathersync.utils.CustomResult
import com.weathersync.utils.FirebaseEvent
import com.weathersync.utils.NoGoogleMapsGeocodingResult
import com.weathersync.utils.weather.limits.NextUpdateTimeFormatter
import com.weathersync.utils.weather.limits.QueryType
import io.mockk.coVerify
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class SettingsViewModelLocationSetTests {
    @get: Rule(order = 1)
    val settingsBaseRule = SettingsBaseRule()
    @get: Rule(order = 0)
    val dispatcherRule = MainDispatcherRule(settingsBaseRule.testDispatcher)
    private val inputLocation = LocationInfo().fullLocation()
    private var uiEventsListener: Job? = null
    private val collectedUiEvents = mutableListOf<SettingsUIEvent>()

    private fun TestScope.launchUIEventsListener() {
        uiEventsListener = launch {
            settingsBaseRule.viewModel.uiEvents.collect { collectedUiEvents.add(it) }
        }
    }
    private fun cancelUIEventsListener() = uiEventsListener!!.cancel()

    @Test
    fun setLocation_inputLocationNull_locationSetToCurrentOne() = runTest {
        settingsBaseRule.apply {
            viewModel.handleIntent(SettingsIntent.SetCurrLocationAsDefault)
            launchUIEventsListener()
            viewModel.uiState.test {
                skipItems(1)

                assertEquals(CustomResult.InProgress, awaitItem().locationSetResult)
                assertEquals(CustomResult.Success, awaitItem().locationSetResult)
                // allow for ui events to be fully collected
                advanceUntilIdle()
                assertEquals(listOf(SettingsUIEvent.ManageSetLocationSheet(show = false), SettingsUIEvent.ShowSnackbar(
                    UIText.StringResource(R.string.location_set_successfully, LocationInfo().fullLocation()))), collectedUiEvents)
                cancelUIEventsListener()

                coVerify(inverse = true) { settingsRepository.isSubscribed() }
                coVerify(inverse = true) { settingsRepository.calculateLocationSetLimits(any()) }
                coVerify(inverse = true) { settingsRepository.incrementLocationSetLimits() }
                coVerify(exactly = 1) { settingsRepository.setCurrLocationAsDefault() }
                testHelper.verifyAnalyticsEvent(event = FirebaseEvent.SET_CURR_LOCATION_AS_DEFAULT, inverse = false)
            }
        }
    }
    @Test
    fun setLocation_inputLocationNotNull_limitNotReached_locationSetToGivenOne() = runTest {
        settingsBaseRule.apply {
            viewModel.handleIntent(SettingsIntent.SetLocation(inputLocation))
            launchUIEventsListener()

            viewModel.uiState.test {
                skipItems(1)

                assertEquals(CustomResult.InProgress, awaitItem().locationSetResult)
                assertEquals(CustomResult.Success, awaitItem().locationSetResult)
                // allow for ui events to be fully collected
                advanceUntilIdle()
                assertEquals(listOf(SettingsUIEvent.ManageSetLocationSheet(show = false), SettingsUIEvent.ShowSnackbar(
                    UIText.StringResource(R.string.location_set_successfully, inputLocation))), collectedUiEvents)
                cancelUIEventsListener()

                coVerify(exactly = 1) { settingsRepository.isSubscribed() }
                coVerify(exactly = 1) { settingsRepository.calculateLocationSetLimits(any()) }
                coVerify(exactly = 1) { settingsRepository.setLocation(inputLocation) }
                coVerify(inverse = true) { settingsRepository.setCurrLocationAsDefault() }
                coVerify(exactly = 1) { settingsRepository.incrementLocationSetLimits() }
                testHelper.verifyAnalyticsEvent(event = FirebaseEvent.SET_CUSTOM_LOCATION, inverse = false)
            }
        }
    }
    @Test
    fun setLocation_inputLocationNotNull_limitNotReached_noGoogleMapsGeocodingResult() = runTest {
        settingsBaseRule.apply {
            setupRepository(areLocationManagerResultsEmpty = true)
            setupViewModel()
            advanceUntilIdle()
            viewModel.handleIntent(SettingsIntent.SetLocation(inputLocation))
            launchUIEventsListener()

            viewModel.uiState.test {
                skipItems(1)

                assertEquals(CustomResult.InProgress, awaitItem().locationSetResult)
                assertEquals(CustomResult.Error, awaitItem().locationSetResult)
                // allow for ui events to be fully collected
                advanceUntilIdle()
                assertEquals(listOf(SettingsUIEvent.ManageSetLocationSheet(show = false),
                    SettingsUIEvent.ShowSnackbar(UIText.StringResource(R.string.could_not_set_location))), collectedUiEvents)
                cancelUIEventsListener()

                coVerify(exactly = 1) { settingsRepository.isSubscribed() }
                coVerify(exactly = 1) { settingsRepository.calculateLocationSetLimits(any()) }
                coVerify(exactly = 1) { settingsRepository.setLocation(inputLocation) }
                coVerify(inverse = true) { settingsRepository.setCurrLocationAsDefault() }
                coVerify(exactly = 1) { settingsRepository.incrementLocationSetLimits() }
                assertTrue(testHelper.exceptionSlot.captured is NoGoogleMapsGeocodingResult)
                testHelper.verifyAnalyticsEvent(event = FirebaseEvent.SET_CUSTOM_LOCATION, inverse = true)
            }
        }
    }
    @Test
    fun setLocation_inputLocationNotNull_limitNotReached_otherError() = runTest {
        settingsBaseRule.apply {
            setupRepository(locationManagerException = testHelper.testException,
                isSubscribed = true)
            setupViewModel()
            advanceUntilIdle()
            viewModel.handleIntent(SettingsIntent.SetLocation(inputLocation))
            launchUIEventsListener()

            viewModel.uiState.test {
                skipItems(1)

                assertEquals(CustomResult.InProgress, awaitItem().locationSetResult)
                assertEquals(CustomResult.Error, awaitItem().locationSetResult)
                // allow for ui events to be fully collected
                advanceUntilIdle()
                assertEquals(listOf(SettingsUIEvent.ManageSetLocationSheet(show = false),
                    SettingsUIEvent.ShowSnackbar(UIText.StringResource(R.string.could_not_set_location))), collectedUiEvents)
                cancelUIEventsListener()

                coVerify(exactly = 1) { settingsRepository.isSubscribed() }
                coVerify(exactly = 1) { settingsRepository.calculateLocationSetLimits(any()) }
                coVerify(exactly = 1) { settingsRepository.setLocation(inputLocation) }
                coVerify(inverse = true) { settingsRepository.setCurrLocationAsDefault() }
                coVerify(inverse = true) { settingsRepository.incrementLocationSetLimits() }
                assertTrue(testHelper.exceptionSlot.captured is TestException)
                testHelper.verifyAnalyticsEvent(event = FirebaseEvent.SET_CUSTOM_LOCATION, inverse = true)
            }
        }
    }
    @Test
    fun setLocation_inputLocationNotNull_limitReached_locationSetToGivenOne() = runTest {
        settingsBaseRule.apply {
            setupLimitManager(timestamps = createDescendingTimestamps(
                limitManagerConfig = QueryType.LocationSet.premiumLimitManagerConfig,
                currTimeMillis = testHelper.testClock.millis()
            ))
            setupRepository(isSubscribed = true)
            setupViewModel()
            // make sure the scope is free (advance init method of the view model)
            advanceUntilIdle()
            viewModel.handleIntent(SettingsIntent.SetLocation(inputLocation))
            launchUIEventsListener()

            viewModel.uiState.test {
                skipItems(1)

                assertEquals(CustomResult.InProgress, awaitItem().locationSetResult)

                val limit = limitManager.calculateLimit(isSubscribed = true, queryType = QueryType.LocationSet)
                val formattedLimit = NextUpdateTimeFormatter(clock = testHelper.testClock, locale = Locale.US)
                    .format(limit.nextUpdateDateTime!!)
                assertEquals(formattedLimit, awaitItem().nextWorldwideSetTime)
                assertEquals(CustomResult.None, awaitItem().locationSetResult)

                // allow for ui events to be fully collected
                advanceUntilIdle()
                assertTrue(collectedUiEvents.isEmpty())
                cancelUIEventsListener()

                coVerify(exactly = 1) { settingsRepository.isSubscribed() }
                coVerify(exactly = 1) { settingsRepository.calculateLocationSetLimits(any()) }
                coVerify(inverse = true) { settingsRepository.setLocation(inputLocation) }
                coVerify(inverse = true) { settingsRepository.setCurrLocationAsDefault() }
                coVerify(inverse = true) { settingsRepository.incrementLocationSetLimits() }

                testHelper.verifyAnalyticsEvent(event = FirebaseEvent.SET_CUSTOM_LOCATION_LIMIT, inverse = false)
            }
        }
    }
}