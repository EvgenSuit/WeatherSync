package com.weathersync.features.home.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.Timestamp
import com.weathersync.R
import com.weathersync.common.TestException
import com.weathersync.common.ui.assertSnackbarIsNotDisplayed
import com.weathersync.common.ui.assertSnackbarTextEquals
import com.weathersync.common.ui.getString
import com.weathersync.common.ui.printToLog
import com.weathersync.common.ui.setContentWithSnackbar
import com.weathersync.common.utils.MainDispatcherRule
import com.weathersync.common.utils.fetchedWeatherUnits
import com.weathersync.features.home.HomeBaseRule
import com.weathersync.features.home.data.Suggestions
import com.weathersync.features.home.getMockedWeather
import com.weathersync.features.home.presentation.HomeIntent
import com.weathersync.features.home.presentation.ui.HomeScreen
import com.weathersync.features.home.toCurrentWeather
import com.weathersync.features.home.toSuggestions
import com.weathersync.utils.AtLeastOneGenerationTagMissing
import com.weathersync.utils.LimitManagerConfig
import io.mockk.coVerify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class HomeSuggestionsUITests {
    private val limitManagerConfig = LimitManagerConfig(2, 24)
    @get: Rule(order = 0)
    val dispatcherRule = MainDispatcherRule()
    @get: Rule(order = 1)
    val homeBaseRule = HomeBaseRule()
    @get: Rule
    val composeRule = createComposeRule()
    private val snackbarScope = TestScope()


    @Test
    fun generateSuggestions_generationException_error() = runTest {
        homeBaseRule.manageLocationPermission(true)
        homeBaseRule.setupHomeRepository(suggestionsGenerationException = homeBaseRule.exception)
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
        homeBaseRule.setupHomeRepository(generatedSuggestions = "Content with no tags")
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
        val timestamps = List(limitManagerConfig.count+1) {
            Timestamp(Date(homeBaseRule.testClock.millis() + 10L * it))
        }
        homeBaseRule.setupLimitManager(
            timestamps = timestamps,
            limitManagerConfig = limitManagerConfig
        )
        homeBaseRule.setupHomeRepository()
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
        val timestamps = List(limitManagerConfig.count+1) {
            Timestamp(Date(homeBaseRule.testClock.millis() + 10L * it))
        }
        homeBaseRule.setupLimitManager(
            timestamps = timestamps,
            limitManagerConfig = limitManagerConfig
        )
        homeBaseRule.setupHomeRepository()
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