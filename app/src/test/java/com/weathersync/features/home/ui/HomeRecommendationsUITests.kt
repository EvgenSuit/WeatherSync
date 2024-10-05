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
import com.weathersync.common.ui.assertSnackbarIsNotDisplayed
import com.weathersync.common.ui.assertSnackbarTextEquals
import com.weathersync.common.ui.setContentWithSnackbar
import com.weathersync.common.utils.MainDispatcherRule
import com.weathersync.features.home.HomeBaseRule
import com.weathersync.features.home.data.Suggestions
import com.weathersync.features.home.presentation.ui.HomeScreen
import com.weathersync.utils.AtLeastOneTagMissing
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class HomeRecommendationsUITests {
    @get: Rule(order = 0)
    val dispatcherRule = MainDispatcherRule()
    @get: Rule(order = 1)
    val homeBaseRule = HomeBaseRule()
    @get: Rule
    val composeRule = createComposeRule()


    @Test
    fun generateSuggestions_generationException_error() = runTest {
        homeBaseRule.setup(suggestionsGenerationException = homeBaseRule.exception)
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = homeBaseRule.snackbarScope,
            uiContent = { HomeScreen(viewModel = homeBaseRule.viewModel)} ) {
            onNodeWithTag("SuggestionsProgress", useUnmergedTree = true).assertIsDisplayed()
            homeBaseRule.getCurrentWeather(this@runTest, success = true)
            assertEquals(homeBaseRule.exception.message, homeBaseRule.crashlyticsExceptionSlot.captured.message)

            checkUISuggestions(homeBaseRule.viewModel.uiState.value.suggestions, displayed = false)
            assertSnackbarTextEquals(R.string.could_not_generate_suggestions, homeBaseRule.snackbarScope)
        }
    }
    @Test
    fun generateSuggestions_weatherFetchFailed_error() = runTest {
        homeBaseRule.setup(geocoderException = homeBaseRule.exception)
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = homeBaseRule.snackbarScope,
            uiContent = { HomeScreen(viewModel = homeBaseRule.viewModel)} ) {
            onNodeWithTag("SuggestionsProgress", useUnmergedTree = true).assertIsDisplayed()
            homeBaseRule.getCurrentWeather(this@runTest, success = false)
            assertEquals(homeBaseRule.exception.message, homeBaseRule.crashlyticsExceptionSlot.captured.message)

            checkUISuggestions(homeBaseRule.viewModel.uiState.value.suggestions, displayed = false)
            onNodeWithTag("SuggestionsProgress", useUnmergedTree = true).assertIsDisplayed()
            assertSnackbarTextEquals(R.string.could_not_fetch_current_weather, homeBaseRule.snackbarScope)
        }
    }
    @Test
    fun generateSuggestions_atLeastOneTagMissing_error() = runTest {
        homeBaseRule.setup(
            generatedSuggestions = "Content without tags"
        )
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = homeBaseRule.snackbarScope,
            uiContent = { HomeScreen(viewModel = homeBaseRule.viewModel)} ) {
            onNodeWithTag("SuggestionsProgress", useUnmergedTree = true).assertIsDisplayed()
            homeBaseRule.getCurrentWeather(this@runTest, success = true)

            assertTrue(homeBaseRule.crashlyticsExceptionSlot.captured is AtLeastOneTagMissing)
            checkUISuggestions(homeBaseRule.viewModel.uiState.value.suggestions, displayed = false)
            assertSnackbarTextEquals(R.string.could_not_generate_suggestions, homeBaseRule.snackbarScope)
    }
    }
    @Test
    fun generateSuggestions_success() = runTest {
        setContentWithSnackbar(composeRule = composeRule,
            snackbarScope = homeBaseRule.snackbarScope, uiContent = { HomeScreen(viewModel = homeBaseRule.viewModel) }) {
            onNodeWithTag("SuggestionsProgress", useUnmergedTree = true).assertIsDisplayed()
            homeBaseRule.getCurrentWeather(this@runTest, success = true)
            assertTrue(!homeBaseRule.crashlyticsExceptionSlot.isCaptured)

            val suggestions = homeBaseRule.viewModel.uiState.value.suggestions
            assertEquals(homeBaseRule.testSuggestions.recommendedActivities, suggestions.recommendedActivities)
            assertEquals(homeBaseRule.testSuggestions.unrecommendedActivities, suggestions.unrecommendedActivities)
            assertEquals(homeBaseRule.testSuggestions.whatToBring, suggestions.whatToBring)

            checkUISuggestions(suggestions, displayed = true)
            assertSnackbarIsNotDisplayed(homeBaseRule.snackbarScope)
        }
    }
    private fun ComposeContentTestRule.checkUISuggestions(suggestions: Suggestions,
                                                          displayed: Boolean) {
        for (recommendation in listOf(suggestions.recommendedActivities,
            suggestions.unrecommendedActivities, suggestions.whatToBring).flatten()) {
            onNodeWithText(recommendation, substring = true).performScrollTo().apply {
                if (displayed) assertIsDisplayed() else assertIsNotDisplayed()
            }
        }
    }
}