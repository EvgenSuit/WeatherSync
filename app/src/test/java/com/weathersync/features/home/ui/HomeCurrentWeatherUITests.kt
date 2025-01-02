package com.weathersync.features.home.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.firebase.Timestamp
import com.weathersync.R
import com.weathersync.common.MainDispatcherRule
import com.weathersync.common.ui.assertDisplayedLimitIsCorrect
import com.weathersync.common.ui.assertSnackbarIsNotDisplayed
import com.weathersync.common.ui.assertSnackbarTextEquals
import com.weathersync.common.ui.getString
import com.weathersync.common.ui.setContentWithSnackbar
import com.weathersync.common.utils.createDescendingTimestamps
import com.weathersync.common.weather.fetchedWeatherUnits
import com.weathersync.features.home.HomeBaseRule
import com.weathersync.features.home.data.CurrentWeather
import com.weathersync.features.home.getMockedWeather
import com.weathersync.features.home.presentation.ui.HomeScreen
import com.weathersync.features.home.toCurrentWeather
import com.weathersync.utils.ads.AdBannerType
import com.weathersync.utils.weather.limits.QueryType
import com.weathersync.utils.weather.limits.NextUpdateTimeFormatter
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.util.Locale

@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(RobolectricTestRunner::class)
class HomeCurrentWeatherUITests {
    @get: Rule(order = 1)
    val homeBaseRule = HomeBaseRule()
    @get: Rule(order = 0)
    val dispatcherRule = MainDispatcherRule(homeBaseRule.testDispatcher)
    @get: Rule
    val composeRule = createComposeRule()
    private val snackbarScope = TestScope()

    @Test
    fun fetchCurrentWeather_notSubscribed_successAdsShown() = runTest {
        homeBaseRule.apply {
            subscriptionInfoDatastore.setIsSubscribed(false)
            setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope, uiContent = {
                HomeScreen(viewModel = viewModel)
            }) {
                onNodeWithTag("CurrentWeatherProgress").assertIsDisplayed()
                manageLocationPermission(grant = true)
                onNodeWithText(getString(R.string.request_permission)).assertIsDisplayed().performClick()

                waitForIdle()
                onNodeWithText(getString(R.string.request_permission)).assertIsNotDisplayed()
                advance(this@runTest)
                val currentWeather = getMockedWeather(fetchedWeatherUnits).toCurrentWeather()
                assertEquals(currentWeather, homeBaseRule.viewModel.uiState.value.currentWeather)
                assertSnackbarIsNotDisplayed(snackbarScope = snackbarScope)

                onNodeWithTag(AdBannerType.Home.name).assertExists()
                assertCorrectCurrentWeatherUI(currentWeather)
            }
        }
    }
    @Test
    fun fetchCurrentWeather_notSubscribed_limitReachedNextGenerationAndUpgradeShown() = runTest {
        homeBaseRule.apply {
            setupLimitManager(
                timestamps = createDescendingTimestamps(
                    limitManagerConfig = QueryType.CurrentWeather(false).regularLimitManagerConfig,
                    currTimeMillis = testClock.millis()
                )
            )
            setupWeatherRepository()
            setupHomeRepository(isSubscribed = false)
            setupViewModel()
            subscriptionInfoDatastore.setIsSubscribed(false)
            setContentWithSnackbar(
                composeRule = composeRule,
                snackbarScope = snackbarScope,
                uiContent = {
                    HomeScreen(viewModel = homeBaseRule.viewModel)
                }) {
                onNodeWithTag("CurrentWeatherProgress").assertIsDisplayed()
                manageLocationPermission(grant = true)
                onNodeWithText(getString(R.string.request_permission)).assertIsDisplayed()
                    .performClick()

                waitForIdle()
                onNodeWithText(getString(R.string.request_permission)).assertIsNotDisplayed()
                advance(this@runTest)
                assertEquals(null, homeBaseRule.viewModel.uiState.value.currentWeather)
                assertSnackbarIsNotDisplayed(snackbarScope = snackbarScope)

                onNodeWithTag(AdBannerType.Home.name).assertExists()
                launch {
                    onNodeWithText(getString(R.string.next_update_time, viewModel.uiState.value.formattedNextUpdateTime!!)).assertIsDisplayed()
                    onNodeWithText(getString(R.string.upgrade_to_premium)).assertIsDisplayed()
                }
            }
        }
    }
    @Test
    fun fetchCurrentWeather_subscribed_limitReachedNextGenerationShown() = runTest {
        homeBaseRule.apply {
            setupLimitManager(
                timestamps = createDescendingTimestamps(
                    limitManagerConfig = QueryType.CurrentWeather(false).premiumLimitManagerConfig,
                    currTimeMillis = testClock.millis()
                )
            )
            setupWeatherRepository()
            setupHomeRepository(isSubscribed = true)
            setupViewModel()
            subscriptionInfoDatastore.setIsSubscribed(true)
            setContentWithSnackbar(
                composeRule = composeRule,
                snackbarScope = snackbarScope,
                uiContent = {
                    HomeScreen(viewModel = viewModel)
                }) {
                onNodeWithTag("CurrentWeatherProgress").assertIsDisplayed()
                manageLocationPermission(grant = true)
                onNodeWithText(getString(R.string.request_permission)).assertIsDisplayed()
                    .performClick()

                waitForIdle()
                onNodeWithText(getString(R.string.request_permission)).assertIsNotDisplayed()
                advance(this@runTest)
                assertEquals(null, viewModel.uiState.value.currentWeather)
                assertSnackbarIsNotDisplayed(snackbarScope = snackbarScope)

                onNodeWithTag(AdBannerType.Home.name).assertDoesNotExist()
                launch {
                    onNodeWithText(getString(R.string.next_update_time, viewModel.uiState.value.formattedNextUpdateTime!!)).assertIsDisplayed()
                    onNodeWithText(getString(R.string.upgrade_to_premium)).assertDoesNotExist()
                }
            }
        }
    }
    @Test
    fun fetchCurrentWeather_subscribed_successAdsNotShown() = runTest {
        homeBaseRule.apply {
            subscriptionInfoDatastore.setIsSubscribed(true)
            setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope, uiContent = {
                HomeScreen(viewModel = viewModel)
            }) {
                onNodeWithTag("CurrentWeatherProgress").assertIsDisplayed()
                manageLocationPermission(grant = true)
                onNodeWithText(getString(R.string.request_permission)).assertIsDisplayed().performClick()

                waitForIdle()
                onNodeWithText(getString(R.string.request_permission)).assertIsNotDisplayed()
                advance(this@runTest)
                val currentWeather = getMockedWeather(fetchedWeatherUnits).toCurrentWeather()
                assertEquals(currentWeather, viewModel.uiState.value.currentWeather)
                assertSnackbarIsNotDisplayed(snackbarScope = snackbarScope)

                onNodeWithTag(AdBannerType.Home.name).assertDoesNotExist()
                assertCorrectCurrentWeatherUI(currentWeather)
            }
        }
    }
    @Test
    fun fetchCurrentWeather_permissionDenied_requestPermissionShown() = runTest {
        homeBaseRule.apply {
            setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope, uiContent = {
                HomeScreen(viewModel = viewModel)
            }) {
                onNodeWithTag("CurrentWeatherProgress").assertIsDisplayed()
                waitForIdle()
                onNodeWithText(getString(R.string.request_permission)).assertIsDisplayed()
                advance(this@runTest)
                assertEquals(null, viewModel.uiState.value.currentWeather)
                onNodeWithTag("CurrentWeatherProgress").assertIsDisplayed()
            }
        }
    }
    @Test
    fun getCurrentWeather_USLocale_limitReached_localWeatherIsNull() = runTest {
        getCurrentWeather_localWeatherIsNullUtil(locale = Locale.US)
    }
    @Test
    fun getCurrentWeather_UKLocale_limitReached_localWeatherIsNull() = runTest {
        getCurrentWeather_localWeatherIsNullUtil(locale = Locale.UK)
    }

    @Test
    fun fetchCurrentWeather_geocoderError() = runTest {
        homeBaseRule.apply {
            manageLocationPermission(grant = true)
            setupWeatherRepository(geocoderException = homeBaseRule.exception)
            setupHomeRepository(isSubscribed = false)
            setupViewModel()
        }
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
        homeBaseRule.apply {
            manageLocationPermission(grant = true)
            setupWeatherRepository(status = HttpStatusCode.Forbidden)
            setupHomeRepository(isSubscribed = false)
            setupViewModel()
        }
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
        homeBaseRule.apply {
            manageLocationPermission(grant = true)
            setupWeatherRepository(lastLocationException = homeBaseRule.exception)
            setupHomeRepository(isSubscribed = false)
            setupViewModel()
        }
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope, uiContent = {
            HomeScreen(viewModel = homeBaseRule.viewModel)
        }) {
            performWeatherAndSuggestionsFetch(testScope = this@runTest)
            assertEquals(null, homeBaseRule.viewModel.uiState.value.currentWeather)
            assertSnackbarTextEquals(R.string.could_not_fetch_current_weather, snackbarScope = snackbarScope)
            onNodeWithTag("CurrentWeatherProgress").assertIsDisplayed()
        }
    }
    private fun TestScope.getCurrentWeather_localWeatherIsNullUtil(locale: Locale) {
        homeBaseRule.apply {
            manageLocationPermission(true)
            val timestamps = createDescendingTimestamps(
                limitManagerConfig = homeBaseRule.regularLimitManagerConfig,
                currTimeMillis = homeBaseRule.testClock.millis()
            )
            setupLimitManager(timestamps = timestamps)
            setupHomeRepository(isSubscribed = false)
            setupViewModel(locale = locale)
            setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope, uiContent = {
                HomeScreen(viewModel = viewModel)
            }) {
                onNodeWithTag("CurrentWeatherProgress").assertIsDisplayed()
                waitForIdle()
                onNodeWithText(getString(R.string.request_permission)).assertIsNotDisplayed()
                advance(this@getCurrentWeather_localWeatherIsNullUtil)
                onNodeWithTag("CurrentWeatherProgress").assertIsDisplayed()
                assertSnackbarIsNotDisplayed(snackbarScope = snackbarScope)

                checkDisplayedLimit(timestamps = timestamps, locale = locale)
            }
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
        val nextUpdateDate = homeBaseRule.testHelper.assertNextUpdateTimeIsCorrect(
            receivedNextUpdateDateTime = homeBaseRule.viewModel.uiState.value.limit.nextUpdateDateTime,
            limitManagerConfig = homeBaseRule.regularLimitManagerConfig,
            timestamps = timestamps)
        assertDisplayedLimitIsCorrect(
            resId = R.string.next_update_time,
            formattedNextUpdateDate = NextUpdateTimeFormatter(
                clock = homeBaseRule.testClock,
                locale = locale
            ).format(nextUpdateDate.expectedNextUpdateDate))
    }
}