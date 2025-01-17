package com.weathersync.features.activityPlanning.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.Timestamp
import com.weathersync.R
import com.weathersync.common.MainDispatcherRule
import com.weathersync.common.ui.assertDisplayedLimitIsCorrect
import com.weathersync.common.ui.assertSnackbarIsNotDisplayed
import com.weathersync.common.ui.assertSnackbarTextEquals
import com.weathersync.common.ui.getString
import com.weathersync.common.ui.setContentWithSnackbar
import com.weathersync.common.utils.createDescendingTimestamps
import com.weathersync.features.activityPlanning.ActivityPlanningBaseRule
import com.weathersync.features.activityPlanning.presentation.ui.ActivityPlanningScreen
import com.weathersync.utils.ads.AdBannerType
import com.weathersync.utils.weather.limits.GenerationType
import com.weathersync.utils.weather.limits.NextUpdateTimeFormatter
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class ActivityPlanningUITests {
    @get: Rule(order = 1)
    val activityPlanningBaseRule = ActivityPlanningBaseRule()
    @get: Rule(order = 0)
    val mainDispatcherRule = MainDispatcherRule(activityPlanningBaseRule.testDispatcher)
    @get: Rule
    val composeRule = createComposeRule()
    private val snackbarScope = TestScope()

    @Test
    fun generateRecommendations_notSubscribed_successAdsShown() = runTest {
        activityPlanningBaseRule.apply {
            subscriptionInfoDatastore.setIsSubscribed(false)
            setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope,
                uiContent = {
                    ActivityPlanningScreen(viewModel = viewModel)
                }) {
                performActivityPlanning()
                assertUrlAndDatesAreCorrect()

                // post advancement checks
                onNodeWithText(activityPlanningSuggestions).assertIsDisplayed()
                assertSnackbarIsNotDisplayed(snackbarScope)
                onNodeWithTag("Next generation time").assertIsNotDisplayed()
                onNodeWithTag(AdBannerType.ActivityPlanning.name).assertExists()
            }
        }
    }
    @Test
    fun generateRecommendations_notSubscribed_limitReachedUpgradeToPremiumShow() = runTest {
        activityPlanningBaseRule.apply {
            setupLimitManager(timestamps = createDescendingTimestamps(
                limitManagerConfig = GenerationType.ActivityRecommendations.regularLimitManagerConfig,
                currTimeMillis = testClock.millis()))
            setupActivityPlanningRepository(isSubscribed = false)
            setupViewModel()

            subscriptionInfoDatastore.setIsSubscribed(false)
            setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope,
                uiContent = {
                    ActivityPlanningScreen(viewModel = viewModel)
                }) {
                performActivityPlanning()

                // post advancement checks
                onNodeWithText(activityPlanningSuggestions).assertDoesNotExist()
                assertSnackbarIsNotDisplayed(snackbarScope)
                onNodeWithText(getString(R.string.next_generation_available_at, viewModel.uiState.value.formattedNextGenerationTime!!)).assertIsDisplayed()
                onNodeWithText(getString(R.string.upgrade_to_premium)).assertIsDisplayed()
                onNodeWithTag(AdBannerType.ActivityPlanning.name).assertExists()
            }
        }
    }
    @Test
    fun generateRecommendations_subscribed_limitReachedNextGenerationShown() = runTest {
        activityPlanningBaseRule.apply {
            setupLimitManager(timestamps = createDescendingTimestamps(
                limitManagerConfig = GenerationType.ActivityRecommendations.premiumLimitManagerConfig,
                currTimeMillis = testClock.millis()))
            setupActivityPlanningRepository(isSubscribed = true)
            setupViewModel()

            subscriptionInfoDatastore.setIsSubscribed(true)
            setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope,
                uiContent = {
                    ActivityPlanningScreen(viewModel = viewModel)
                }) {
                performActivityPlanning()

                // post advancement checks
                onNodeWithText(activityPlanningSuggestions).assertDoesNotExist()
                assertSnackbarIsNotDisplayed(snackbarScope)
                onNodeWithText(getString(R.string.next_generation_available_at, viewModel.uiState.value.formattedNextGenerationTime!!)).assertIsDisplayed()
                onNodeWithText(getString(R.string.upgrade_to_premium)).assertDoesNotExist()
                onNodeWithTag(AdBannerType.ActivityPlanning.name).assertDoesNotExist()
            }
        }
    }
    @Test
    fun generateRecommendations_subscribed_successAdsNotShown() = runTest {
        activityPlanningBaseRule.apply {
            subscriptionInfoDatastore.setIsSubscribed(true)
            setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope,
                uiContent = {
                    ActivityPlanningScreen(viewModel = viewModel)
                }) {
                performActivityPlanning()
                assertUrlAndDatesAreCorrect()

                // post advancement checks
                onNodeWithText(activityPlanningSuggestions).assertIsDisplayed()
                assertSnackbarIsNotDisplayed(snackbarScope)
                onNodeWithTag("Next generation time").assertIsNotDisplayed()
                onNodeWithText(AdBannerType.ActivityPlanning.name).assertDoesNotExist()
            }
        }
    }
    @Test
    fun generateRecommendations_USLocale_limitReached() = runTest {
        generateRecommendations_limitReached(Locale.US)
    }
    @Test
    fun generateRecommendations_UKLocale_limitReached() = runTest {
        generateRecommendations_limitReached(Locale.UK)
    }
    @Test
    fun generateRecommendations_forecastError() = runTest {
        val status = HttpStatusCode.Forbidden
        activityPlanningBaseRule.apply {
            setupForecastRepository(status = status)
            setupActivityPlanningRepository(isSubscribed = false)
            setupViewModel()
            setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope,
                uiContent = {
                    ActivityPlanningScreen(viewModel = activityPlanningBaseRule.viewModel)
                }) {
                performActivityPlanning(error = status.description)
                assertUrlAndDatesAreCorrect()
            }
        }
    }
    @Test
    fun generateRecommendations_generationError() = runTest {
        val status = HttpStatusCode.Forbidden
        activityPlanningBaseRule.apply {
            setupForecastRepository(status = status)
            setupActivityPlanningRepository(isSubscribed = false)
            setupViewModel()
            setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope,
                uiContent = {
                    ActivityPlanningScreen(viewModel = activityPlanningBaseRule.viewModel)
                }) {
                performActivityPlanning(error = status.description)
                assertUrlAndDatesAreCorrect()
            }
        }
    }
    @Test
    fun generateRecommendations_geocoderException_error() = runTest {
        activityPlanningBaseRule.apply {
            setupForecastRepository(geocoderException = testHelper.testException)
            setupActivityPlanningRepository(isSubscribed = false)
            setupViewModel()
            setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope,
                uiContent = {
                    ActivityPlanningScreen(viewModel = viewModel)
                }) {
                performActivityPlanning(error = testHelper.testException.message)
            }
        }
    }
    @Test
    fun generateRecommendations_lastLocationException_error() = runTest {
        activityPlanningBaseRule.apply {
            setupForecastRepository(lastLocationException = testHelper.testException)
            setupActivityPlanningRepository(isSubscribed = false)
            setupViewModel()
            setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope,
                uiContent = {
                    ActivityPlanningScreen(viewModel = viewModel)
                }) {
                performActivityPlanning(error = testHelper.testException.message)
            }
        }
    }
    @Test
    fun generateRecommendations_suggestionsGenerationException() = runTest {
        activityPlanningBaseRule.apply {
            setupActivityPlanningRepository(
                isSubscribed = false,
                generationHttpStatusCode = HttpStatusCode.Forbidden
            )
            setupViewModel()
            setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope,
                uiContent = {
                    ActivityPlanningScreen(viewModel = viewModel)
                }) {
                performActivityPlanning(error = HttpStatusCode.Forbidden.description)
            }
        }
    }

    private fun TestScope.performActivityPlanning(error: String? = null) {
        composeRule.apply {
            val inputText = "Some text"
            val textField = onNodeWithTag("ActivityTextField")
            val button = onNodeWithText(getString(R.string.find_optimal_times))
            button.assertIsDisplayed().assertIsNotEnabled()
            textField.assertIsDisplayed().performTextReplacement(inputText)

            // post click check
            button.performClick().assertIsNotDisplayed()
            onNodeWithText(getString(R.string.planning_activities)).assertIsDisplayed()
            onNodeWithTag("ActivityPlannerProgress").assertIsDisplayed()

            activityPlanningBaseRule.advance(this@performActivityPlanning)

            error?.let { e ->
                onNodeWithText(activityPlanningBaseRule.activityPlanningSuggestions).assertIsNotDisplayed()
                val exception = activityPlanningBaseRule.testHelper.exceptionSlot.captured
                exception.apply { if (this is ClientRequestException) assertEquals(e, this.response.status.description)
                else assertEquals(e, this.message)
                }
                assertSnackbarTextEquals(R.string.could_not_plan_activities, snackbarScope)
            }
        }
    }
    private fun TestScope.generateRecommendations_limitReached(locale: Locale) {
        val timestamps = createDescendingTimestamps(
            limitManagerConfig = activityPlanningBaseRule.regularLimitManagerConfig,
            currTimeMillis = activityPlanningBaseRule.testClock.millis()
        )
        activityPlanningBaseRule.apply {
            setupLimitManager(timestamps = timestamps)
            setupActivityPlanningRepository(isSubscribed = false)
            setupViewModel(locale = locale)
        }
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope,
            uiContent = {
                ActivityPlanningScreen(viewModel = activityPlanningBaseRule.viewModel)
            }) {
            performActivityPlanning()

            // post advancement checks
            onNodeWithText(activityPlanningBaseRule.activityPlanningSuggestions).assertIsNotDisplayed()
            assertSnackbarIsNotDisplayed(snackbarScope)
            checkDisplayedLimit(timestamps, locale = locale)
        }
    }
    private fun ComposeContentTestRule.checkDisplayedLimit(
        timestamps: List<Timestamp>,
        locale: Locale
    ) {
        val nextUpdateDate = activityPlanningBaseRule.testHelper.assertNextUpdateTimeIsCorrect(
            receivedNextUpdateDateTime = activityPlanningBaseRule.viewModel.uiState.value.limit.nextUpdateDateTime,
            limitManagerConfig = activityPlanningBaseRule.regularLimitManagerConfig,
            timestamps = timestamps)
        assertEquals(nextUpdateDate.expectedNextUpdateDate, nextUpdateDate.receivedNextUpdateDate)
        assertDisplayedLimitIsCorrect(
            resId = R.string.next_generation_available_at,
            formattedNextUpdateDate = NextUpdateTimeFormatter(
                clock = activityPlanningBaseRule.testClock,
                locale = locale
            ).format(nextUpdateDate.expectedNextUpdateDate))
    }
}