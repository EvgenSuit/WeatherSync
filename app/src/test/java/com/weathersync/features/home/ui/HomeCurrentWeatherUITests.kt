package com.weathersync.features.home.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.Timestamp
import com.weathersync.R
import com.weathersync.common.ui.assertDisplayedLimitIsCorrect
import com.weathersync.common.ui.assertSnackbarIsNotDisplayed
import com.weathersync.common.ui.assertSnackbarTextEquals
import com.weathersync.common.ui.getString
import com.weathersync.common.ui.setContentWithSnackbar
import com.weathersync.common.utils.MainDispatcherRule
import com.weathersync.common.utils.createDescendingTimestamps
import com.weathersync.common.utils.fetchedWeatherUnits
import com.weathersync.features.home.HomeBaseRule
import com.weathersync.features.home.data.CurrentWeather
import com.weathersync.features.home.getMockedWeather
import com.weathersync.features.home.presentation.ui.HomeScreen
import com.weathersync.features.home.toCurrentWeather
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.GraphicsMode
import java.util.Locale

@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(AndroidJUnit4::class)
class HomeCurrentWeatherUITests {
    @get: Rule(order = 0)
    val dispatcherRule = MainDispatcherRule()
    @get: Rule(order = 1)
    val homeBaseRule = HomeBaseRule()
    @get: Rule
    val composeRule = createComposeRule()
    private val snackbarScope = TestScope()

    @Before
    fun beforeTest() {
        stopKoin()
    }

    @Test
    fun fetchCurrentWeather_success() = runTest {
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope, uiContent = {
            HomeScreen(viewModel = homeBaseRule.viewModel)
        }) {
            onNodeWithTag("CurrentWeatherProgress").assertIsDisplayed()
            homeBaseRule.manageLocationPermission(grant = true)
            onNodeWithText(getString(R.string.request_permission)).assertIsDisplayed().performClick()

            waitForIdle()
            onNodeWithText(getString(R.string.request_permission)).assertIsNotDisplayed()
            homeBaseRule.advance(this@runTest)
            val currentWeather = getMockedWeather(fetchedWeatherUnits).toCurrentWeather()
            assertEquals(currentWeather, homeBaseRule.viewModel.uiState.value.currentWeather)
            assertSnackbarIsNotDisplayed(snackbarScope = snackbarScope)

            assertCorrectCurrentWeatherUI(currentWeather)
        }
    }
    @Test
    fun fetchCurrentWeather_permissionDenied_requestPermissionShown() = runTest {
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope, uiContent = {
            HomeScreen(viewModel = homeBaseRule.viewModel)
        }) {
            onNodeWithTag("CurrentWeatherProgress").assertIsDisplayed()
            waitForIdle()
            onNodeWithText(getString(R.string.request_permission)).assertIsDisplayed()
            homeBaseRule.advance(this@runTest)
            assertEquals(null, homeBaseRule.viewModel.uiState.value.currentWeather)
            onNodeWithTag("CurrentWeatherProgress").assertIsDisplayed()
        }
    }
    @Test
    fun getCurrentWeather_USLocale_limitReached_localWeatherIsNull() = runTest {
        homeBaseRule.manageLocationPermission(true)
        val timestamps = createDescendingTimestamps(
            limitManagerConfig = homeBaseRule.limitManagerConfig,
            currTimeMillis = homeBaseRule.testClock.millis()
        )
        homeBaseRule.setupLimitManager(
            locale = Locale.US,
            timestamps = timestamps,
            limitManagerConfig = homeBaseRule.limitManagerConfig)
        homeBaseRule.setupHomeRepository()
        homeBaseRule.setupViewModel()
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope, uiContent = {
            HomeScreen(viewModel = homeBaseRule.viewModel)
        }) {
            onNodeWithTag("CurrentWeatherProgress").assertIsDisplayed()
            waitForIdle()
            onNodeWithText(getString(R.string.request_permission)).assertIsNotDisplayed()
            homeBaseRule.advance(this@runTest)
            onNodeWithTag("CurrentWeatherProgress").assertIsDisplayed()
            assertSnackbarIsNotDisplayed(snackbarScope = snackbarScope)

            checkDisplayedLimit(timestamps = timestamps, locale = Locale.US)
        }
    }
    @Test
    fun getCurrentWeather_UKLocale_limitReached_localWeatherIsNull() = runTest {
        homeBaseRule.manageLocationPermission(true)
        val timestamps = createDescendingTimestamps(
            limitManagerConfig = homeBaseRule.limitManagerConfig,
            currTimeMillis = homeBaseRule.testClock.millis()
        )
        homeBaseRule.setupLimitManager(
            locale = Locale.UK,
            timestamps = timestamps,
            limitManagerConfig = homeBaseRule.limitManagerConfig)
        homeBaseRule.setupHomeRepository()
        homeBaseRule.setupViewModel()
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope, uiContent = {
            HomeScreen(viewModel = homeBaseRule.viewModel)
        }) {
            onNodeWithTag("CurrentWeatherProgress").assertIsDisplayed()
            waitForIdle()
            onNodeWithText(getString(R.string.request_permission)).assertIsNotDisplayed()
            homeBaseRule.advance(this@runTest)
            onNodeWithTag("CurrentWeatherProgress").assertIsDisplayed()
            assertSnackbarIsNotDisplayed(snackbarScope = snackbarScope)

            checkDisplayedLimit(timestamps = timestamps, locale = Locale.UK)
        }
    }
    @Test
    fun getCurrentWeather_limitReached_localWeatherIsNotNull() = runTest {
        homeBaseRule.currentWeatherLocalDB.currentWeatherDao().insertWeather(getMockedWeather(fetchedWeatherUnits).toCurrentWeather())
        homeBaseRule.manageLocationPermission(true)
        val timestamps = createDescendingTimestamps(
            limitManagerConfig = homeBaseRule.limitManagerConfig,
            currTimeMillis = homeBaseRule.testClock.millis()
        )
        homeBaseRule.setupLimitManager(
            locale = Locale.US,
            timestamps = timestamps,
            limitManagerConfig = homeBaseRule.limitManagerConfig)
        homeBaseRule.setupHomeRepository()
        homeBaseRule.setupViewModel()
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope, uiContent = {
            HomeScreen(viewModel = homeBaseRule.viewModel)
        }) {
            onNodeWithTag("CurrentWeatherProgress").assertIsDisplayed()
            waitForIdle()
            onNodeWithText(getString(R.string.request_permission)).assertIsNotDisplayed()
            homeBaseRule.advance(this@runTest)
            assertSnackbarIsNotDisplayed(snackbarScope = snackbarScope)
            assertCorrectCurrentWeatherUI(getMockedWeather(fetchedWeatherUnits).toCurrentWeather())

            checkDisplayedLimit(timestamps = timestamps, locale = Locale.US)
        }
    }
    @Test
    fun fetchCurrentWeather_geocoderError() = runTest {
        homeBaseRule.manageLocationPermission(grant = true)
        homeBaseRule.setupWeatherRepository(geocoderException = homeBaseRule.exception)
        homeBaseRule.setupHomeRepository()
        homeBaseRule.setupViewModel()
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope, uiContent = {
            HomeScreen(viewModel = homeBaseRule.viewModel)
        }) {
            performWeatherAndSuggestionsFetch(testScope = this@runTest)
            assertEquals(null, homeBaseRule.viewModel.uiState.value.currentWeather)
            assertSnackbarTextEquals(R.string.could_not_fetch_current_weather, snackbarScope = snackbarScope)
            onNodeWithTag("CurrentWeatherProgress").assertIsDisplayed()
        }
    }
    @Test
    fun fetchCurrentWeather_errorResponseStatus_error() = runTest {
        homeBaseRule.manageLocationPermission(grant = true)
        homeBaseRule.setupWeatherRepository(status = HttpStatusCode.Forbidden)
        homeBaseRule.setupHomeRepository()
        homeBaseRule.setupViewModel()
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope, uiContent = {
            HomeScreen(viewModel = homeBaseRule.viewModel)
        }) {
            performWeatherAndSuggestionsFetch(testScope = this@runTest)
            assertEquals(null, homeBaseRule.viewModel.uiState.value.currentWeather)
            assertSnackbarTextEquals(R.string.could_not_fetch_current_weather, snackbarScope = snackbarScope)
            onNodeWithTag("CurrentWeatherProgress").assertIsDisplayed()
        }
    }
    @Test
    fun fetchCurrentWeather_lastLocationException_error() = runTest {
        homeBaseRule.manageLocationPermission(grant = true)
        homeBaseRule.setupWeatherRepository(lastLocationException = homeBaseRule.exception)
        homeBaseRule.setupHomeRepository()
        homeBaseRule.setupViewModel()
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope, uiContent = {
            HomeScreen(viewModel = homeBaseRule.viewModel)
        }) {
            performWeatherAndSuggestionsFetch(testScope = this@runTest)
            assertEquals(null, homeBaseRule.viewModel.uiState.value.currentWeather)
            assertSnackbarTextEquals(R.string.could_not_fetch_current_weather, snackbarScope = snackbarScope)
            onNodeWithTag("CurrentWeatherProgress").assertIsDisplayed()
        }
    }
    private fun ComposeContentTestRule.performWeatherAndSuggestionsFetch(testScope: TestScope) {
        onNodeWithTag("CurrentWeatherProgress").assertIsDisplayed()
        waitForIdle()
        onNodeWithText(getString(R.string.request_permission)).assertIsNotDisplayed()
        homeBaseRule.advance(testScope)
    }
    private fun ComposeContentTestRule.assertCorrectCurrentWeatherUI(currentWeather: CurrentWeather) {
        onNodeWithText("${currentWeather.temp} ${currentWeather.tempUnit}").assertIsDisplayed()
        onNodeWithText(currentWeather.locality).assertIsDisplayed()
        onNodeWithText(getString(R.string.wind_speed, "${currentWeather.windSpeed} ${currentWeather.windSpeedUnit}")).assertIsDisplayed()
    }
    private fun ComposeContentTestRule.checkDisplayedLimit(
        timestamps: List<Timestamp>,
        locale: Locale,
    ) {
        val nextUpdateDate = homeBaseRule.testHelper.calculateNextUpdateDate(
            receivedNextUpdateDateTime = homeBaseRule.viewModel.uiState.value.limit.formattedNextUpdateTime,
            limitManagerConfig = homeBaseRule.limitManagerConfig,
            timestamps = timestamps,
            locale = locale)
        assertDisplayedLimitIsCorrect(
            resId = R.string.next_update_time,
            expectedNextUpdateDate = nextUpdateDate.expectedNextUpdateDate,
            locale = locale)
    }
}