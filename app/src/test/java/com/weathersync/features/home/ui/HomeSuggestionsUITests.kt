package com.weathersync.features.home.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.weathersync.R
import com.weathersync.common.MainDispatcherRule
import com.weathersync.common.TestException
import com.weathersync.common.ui.assertSnackbarIsNotDisplayed
import com.weathersync.common.ui.assertSnackbarTextEquals
import com.weathersync.common.ui.getString
import com.weathersync.common.ui.setContentWithSnackbar
import com.weathersync.common.utils.createDescendingTimestamps
import com.weathersync.common.weather.fetchedWeatherUnits
import com.weathersync.features.home.HomeBaseRule
import com.weathersync.features.home.data.Suggestions
import com.weathersync.features.home.getMockedWeather
import com.weathersync.features.home.presentation.ui.HomeScreen
import com.weathersync.features.home.toCurrentWeather
import com.weathersync.features.home.toSuggestions
import com.weathersync.utils.AtLeastOneGenerationTagMissing
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeSuggestionsUITests {
    @get: Rule(order = 1)
    val homeBaseRule = HomeBaseRule()
    @get: Rule(order = 0)
    val dispatcherRule = MainDispatcherRule(homeBaseRule.testDispatcher)
    @get: Rule
    val composeRule = createComposeRule()
    private val snackbarScope = TestScope()


    @Test
    fun generateSuggestions_generationException_error() = runTest {
        homeBaseRule.manageLocationPermission(true)
        homeBaseRule.setupHomeRepository(
            isSubscribed = false,
            suggestionsGenerationException = homeBaseRule.exception)
        homeBaseRule.setupViewModel()
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope,
            uiContent = { HomeScreen(viewModel = homeBaseRule.viewModel)} ) {
            onNodeWithTag("SuggestionsProgress").assertIsDisplayed()
            // advance permission check
            waitForIdle()
            homeBaseRule.advance(this@runTest)
            assertTrue(homeBaseRule.crashlyticsExceptionSlot.captured is TestException)

            assertSuggestionsUI(homeBaseRule.viewModel.uiState.value.suggestions, displayed = false)
            assertSnackbarTextEquals(R.string.could_not_generate_suggestions, snackbarScope)
            onNodeWithTag("SuggestionsProgress").assertIsDisplayed()
        }
    }
    @Test
    fun generateSuggestions_atLeastOneTagMissing_error() = runTest {
        homeBaseRule.manageLocationPermission(true)
        homeBaseRule.setupHomeRepository(
            isSubscribed = false,
            generatedSuggestions = "Content with no tags")
        homeBaseRule.setupViewModel()
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope,
            uiContent = { HomeScreen(viewModel = homeBaseRule.viewModel)} ) {
            onNodeWithTag("SuggestionsProgress", useUnmergedTree = true).assertIsDisplayed()
            waitForIdle()
            homeBaseRule.advance(this@runTest)

            assertTrue(homeBaseRule.crashlyticsExceptionSlot.captured is AtLeastOneGenerationTagMissing)
            assertSuggestionsUI(homeBaseRule.viewModel.uiState.value.suggestions, displayed = false)
            assertSnackbarTextEquals(R.string.could_not_generate_suggestions, snackbarScope)
            onNodeWithTag("SuggestionsProgress").assertIsDisplayed()
        }
    }
    @Test
    fun generateSuggestions_success() = runTest {
        homeBaseRule.manageLocationPermission(true)
        setContentWithSnackbar(composeRule = composeRule,
            snackbarScope = snackbarScope, uiContent = { HomeScreen(viewModel = homeBaseRule.viewModel) }) {
            onNodeWithTag("SuggestionsProgress", useUnmergedTree = true).assertIsDisplayed()
            waitForIdle()
            homeBaseRule.advance(this@runTest)
            assertTrue(!homeBaseRule.crashlyticsExceptionSlot.isCaptured)

            val suggestions = homeBaseRule.viewModel.uiState.value.suggestions
            assertEquals(homeBaseRule.testSuggestions.recommendedActivities, suggestions?.recommendedActivities)
            assertEquals(homeBaseRule.testSuggestions.unrecommendedActivities, suggestions?.unrecommendedActivities)
            assertEquals(homeBaseRule.testSuggestions.whatToBring, suggestions?.whatToBring)

            assertSuggestionsUI(suggestions, displayed = true)
            assertSnackbarIsNotDisplayed(snackbarScope)
        }
    }
   /* @Test
    fun refreshSuggestions_localLimitReached_success() = runTest {
        homeBaseRule.manageLocationPermission(true)
        setContentWithSnackbar(composeRule = composeRule,
            snackbarScope = snackbarScope, uiContent = { HomeScreen(viewModel = homeBaseRule.viewModel) }) {
            onNodeWithTag("SuggestionsProgress", useUnmergedTree = true).assertIsDisplayed()
            waitForIdle()
            homeBaseRule.advance(this@runTest)
            assertTrue(!homeBaseRule.crashlyticsExceptionSlot.isCaptured)

            val suggestions = homeBaseRule.viewModel.uiState.value.suggestions
            assertEquals(homeBaseRule.testSuggestions.recommendedActivities, suggestions?.recommendedActivities)
            assertEquals(homeBaseRule.testSuggestions.unrecommendedActivities, suggestions?.unrecommendedActivities)
            assertEquals(homeBaseRule.testSuggestions.whatToBring, suggestions?.whatToBring)

            assertSuggestionsUI(suggestions, displayed = true)
            assertSnackbarIsNotDisplayed(snackbarScope)

            onRoot().printToLog()
            onNodeWithTag("List").performTouchInput { swipeDown() }
            onNode(hasScrollAction()).performTouchInput { swipeDown() }
            *//*onRoot().performTouchInput { swipeDown() }
            homeBaseRule.advance(this@runTest)
            coVerify(exactly = 1) { homeBaseRule.homeRepository.getCurrentWeather(isLimitReached = true) }*//*
        }
    }*/

    @Test
    fun generateSuggestions_accountLimitReached_localSuggestionsAreNull() = runTest {
        homeBaseRule.manageLocationPermission(true)
        val timestamps = createDescendingTimestamps(
            limitManagerConfig = homeBaseRule.regularLimitManagerConfig,
            currTimeMillis = homeBaseRule.testClock.millis()
        )
        homeBaseRule.setupLimitManager(timestamps = timestamps)
        homeBaseRule.setupHomeRepository(isSubscribed = false)
        homeBaseRule.setupViewModel()
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope, uiContent = {
            HomeScreen(viewModel = homeBaseRule.viewModel)
        }) {
            onNodeWithTag("SuggestionsProgress").assertIsDisplayed()
            waitForIdle()
            onNodeWithText(getString(R.string.request_permission)).assertIsNotDisplayed()
            homeBaseRule.advance(this@runTest)
            onNodeWithTag("SuggestionsProgress").assertIsDisplayed()
            assertSnackbarIsNotDisplayed(snackbarScope = snackbarScope)
        }
    }
    @Test
    fun generateSuggestions_accountLimitReached_localSuggestionsAreNotNull() = runTest {
        homeBaseRule.currentWeatherLocalDB.currentWeatherDao().apply {
            insertWeather(getMockedWeather(fetchedWeatherUnits).toCurrentWeather())
            insertSuggestions(homeBaseRule.testSuggestions.toSuggestions())
        }
        homeBaseRule.manageLocationPermission(true)
        val timestamps = createDescendingTimestamps(
            limitManagerConfig = homeBaseRule.regularLimitManagerConfig,
            currTimeMillis = homeBaseRule.testClock.millis()
        )
        homeBaseRule.setupLimitManager(timestamps = timestamps)
        homeBaseRule.setupHomeRepository(isSubscribed = false)
        homeBaseRule.setupViewModel()
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope, uiContent = {
            HomeScreen(viewModel = homeBaseRule.viewModel)
        }) {
            onNodeWithTag("SuggestionsProgress").assertIsDisplayed()
            waitForIdle()
            onNodeWithText(getString(R.string.request_permission)).assertIsNotDisplayed()
            homeBaseRule.advance(this@runTest)
            assertSnackbarIsNotDisplayed(snackbarScope = snackbarScope)
            assertSuggestionsUI(homeBaseRule.testSuggestions.toSuggestions(), displayed = true)
        }
    }
    private fun ComposeContentTestRule.assertSuggestionsUI(suggestions: Suggestions?,
                                                           displayed: Boolean) {
        if (suggestions == null) return
        for (recommendation in listOf(suggestions.recommendedActivities,
            suggestions.unrecommendedActivities, suggestions.whatToBring).flatten()) {
            onNodeWithText(recommendation, substring = true).performScrollTo().apply {
                if (displayed) assertIsDisplayed() else assertIsNotDisplayed()
            }
        }
    }
}