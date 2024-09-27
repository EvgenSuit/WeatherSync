package com.weathersync.features.home.ui

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.weathersync.R
import com.weathersync.common.home.city
import com.weathersync.common.home.country
import com.weathersync.common.home.mockedWeather
import com.weathersync.common.ui.assertSnackbarIsNotDisplayed
import com.weathersync.common.ui.getString
import com.weathersync.common.ui.setContentWithSnackbar
import com.weathersync.common.utils.MainDispatcherRule
import com.weathersync.features.home.HomeBaseRule
import com.weathersync.features.home.data.CurrentWeather
import com.weathersync.features.home.presentation.ui.HomeScreen
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.GraphicsMode
import java.lang.Exception

@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class HomeCurrentWeatherUITests {
    @get: Rule(order = 0)
    val dispatcherRule = MainDispatcherRule()
    @get: Rule(order = 1)
    val homeRule = HomeBaseRule()
    @get: Rule
    val composeRule = createComposeRule()

    @Before
    fun beforeTest() {
        stopKoin()
    }

    private fun manageLocationPermission(grant: Boolean) {
        val permission = Manifest.permission.ACCESS_COARSE_LOCATION
        val shadowApp = Shadows.shadowOf(RuntimeEnvironment.getApplication())
        shadowApp.apply { if (grant) grantPermissions(permission) else denyPermissions(permission) }
    }

    @Test
    fun fetchCurrentWeather_permissionGranted_success() = runTest {
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = homeRule.snackbarScope, uiContent = {
            HomeScreen(viewModel = homeRule.viewModel)
        }) {
            onNodeWithTag("CurrentWeatherProgress").assertIsDisplayed()
            manageLocationPermission(grant = true)
            onNodeWithText(getString(R.string.request_permission)).assertIsDisplayed().performClick()

            waitForIdle()
            onNodeWithText(getString(R.string.request_permission)).assertIsNotDisplayed()
            homeRule.advanceKtor(this@runTest)
            assertEquals(
                CurrentWeather(
                locality = "$city, $country",
                tempUnit = mockedWeather.currentWeatherUnits.temperature,
                windSpeedUnit = mockedWeather.currentWeatherUnits.windSpeed,
                temp = mockedWeather.currentWeather.temperature,
                windSpeed = mockedWeather.currentWeather.windSpeed,
                weatherCode = mockedWeather.currentWeather.weatherCode
            ), homeRule.viewModel.uiState.value.currentWeather)
            assertSnackbarIsNotDisplayed(snackbarScope = homeRule.snackbarScope)
        }
    }
    @Test
    fun fetchCurrentWeather_permissionDenied_requestPermissionShown() = runTest {
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = homeRule.snackbarScope, uiContent = {
            HomeScreen(viewModel = homeRule.viewModel)
        }) {
            onNodeWithTag("CurrentWeatherProgress").assertIsDisplayed()
            manageLocationPermission(grant = false)
            onNodeWithText(getString(R.string.request_permission)).assertIsDisplayed().performClick()

            waitForIdle()
            onNodeWithText(getString(R.string.request_permission)).assertIsDisplayed()
            homeRule.advanceKtor(this@runTest)
            assertEquals(null, homeRule.viewModel.uiState.value.currentWeather)
        }
    }
    @Test
    fun fetchCurrentWeather_permissionGranted_geocoderError_error() = runTest {
        homeRule.setup(geocoderException = homeRule.exception)
        manageLocationPermission(grant = true)
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = homeRule.snackbarScope, uiContent = {
            HomeScreen(viewModel = homeRule.viewModel)
        }) {
            performErrorWeatherFetch(message = homeRule.exception.message, testScope = this@runTest)
        }
    }
    @Test
    fun fetchCurrentWeather_permissionGranted_errorResponseStatus_error() = runTest {
        homeRule.setup(status = HttpStatusCode.Forbidden)
        manageLocationPermission(grant = true)
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = homeRule.snackbarScope, uiContent = {
            HomeScreen(viewModel = homeRule.viewModel)
        }) {
            performErrorWeatherFetch(message = HttpStatusCode.Forbidden.description, testScope = this@runTest)
        }
    }
    @Test
    fun fetchCurrentWeather_permissionGranted_lastLocationException_error() = runTest {
        homeRule.setup(lastLocationException = homeRule.exception)
        manageLocationPermission(grant = true)
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = homeRule.snackbarScope, uiContent = {
            HomeScreen(viewModel = homeRule.viewModel)
        }) {
            performErrorWeatherFetch(message = homeRule.exception.message, testScope = this@runTest)
        }
    }
    private fun ComposeContentTestRule.performErrorWeatherFetch(message: String?,
                                                                testScope: TestScope) {
        onNodeWithTag("CurrentWeatherProgress").assertIsDisplayed()
        waitForIdle()
        onNodeWithText(getString(R.string.request_permission)).assertIsNotDisplayed()
        homeRule.advanceKtor(testScope)
        assertEquals(message, homeRule.crashlyticsExceptionSlot.captured.message)
        assertEquals(null, homeRule.viewModel.uiState.value.currentWeather)
    }
}