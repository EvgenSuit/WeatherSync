package com.weathersync.features.activityPlanning.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.weathersync.common.ui.getString
import com.weathersync.common.ui.setContentWithSnackbar
import com.weathersync.common.utils.MainDispatcherRule
import com.weathersync.features.activityPlanning.ActivityPlanningBaseRule
import com.weathersync.features.activityPlanning.presentation.ui.ActivityPlanningScreen
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import com.weathersync.R
import com.weathersync.common.ui.assertSnackbarIsNotDisplayed
import com.weathersync.common.ui.assertSnackbarTextEquals
import com.weathersync.utils.AtLeastOneTagMissing
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActivityPlanningUITests {
    @get: Rule(order = 0)
    val mainDispatcherRule = MainDispatcherRule()
    @get: Rule(order = 1)
    val activityPlanningBaseRule = ActivityPlanningBaseRule()
    @get: Rule
    val composeRule = createComposeRule()
    private val snackbarScope = TestScope()

    @Test
    fun generateRecommendations_success() = runTest {
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope,
            uiContent = {
                ActivityPlanningScreen(viewModel = activityPlanningBaseRule.viewModel)
            }) {
            performActivityPlanning()
            activityPlanningBaseRule.assertUrlIsCorrect()

            // post advancement check
            assertEquals(activityPlanningBaseRule.activityPlanningSuggestions, activityPlanningBaseRule.viewModel.uiState.value.generatedText)
            onNodeWithText(activityPlanningBaseRule.activityPlanningSuggestions).assertIsDisplayed()
            assertSnackbarIsNotDisplayed(snackbarScope)
        }
    }
    @Test
    fun generateRecommendations_errorHttpResponse_error() = runTest {
        val status = HttpStatusCode.Forbidden
        activityPlanningBaseRule.setup(status = status)
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope,
            uiContent = {
                ActivityPlanningScreen(viewModel = activityPlanningBaseRule.viewModel)
            }) {
            performActivityPlanning(error = status.description)
            activityPlanningBaseRule.assertUrlIsCorrect()
        }
    }
    @Test
    fun generateRecommendations_geocoderException_error() = runTest {
        activityPlanningBaseRule.setup(geocoderException = activityPlanningBaseRule.exception)
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope,
            uiContent = {
                ActivityPlanningScreen(viewModel = activityPlanningBaseRule.viewModel) }) {
            performActivityPlanning(error = activityPlanningBaseRule.exception.message)
        }
    }
    @Test
    fun generateRecommendations_lastLocationException_error() = runTest {
        activityPlanningBaseRule.setup(lastLocationException = activityPlanningBaseRule.exception)
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope,
            uiContent = {
                ActivityPlanningScreen(viewModel = activityPlanningBaseRule.viewModel) }) {
            performActivityPlanning(error = activityPlanningBaseRule.exception.message)
        }
    }
    @Test
    fun generateRecommendations_suggestionsGenerationException_error() = runTest {
        activityPlanningBaseRule.setup(suggestionsGenerationException = activityPlanningBaseRule.exception)
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope,
            uiContent = {
                ActivityPlanningScreen(viewModel = activityPlanningBaseRule.viewModel) }) {
            performActivityPlanning(error = activityPlanningBaseRule.exception.message)
        }
    }
    @Test
    fun generateRecommendations_atLeastOneTagMissing_error() = runTest {
        activityPlanningBaseRule.setup(generatedSuggestions = "Content without tags")
        setContentWithSnackbar(composeRule = composeRule, snackbarScope = snackbarScope,
            uiContent = {
                ActivityPlanningScreen(viewModel = activityPlanningBaseRule.viewModel) }) {
            performActivityPlanning()
            activityPlanningBaseRule.assertUrlIsCorrect()
            assertTrue(activityPlanningBaseRule.exceptionSlot.captured is AtLeastOneTagMissing)
            assertSnackbarTextEquals(R.string.could_not_plan_activities, snackbarScope)
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
                val exception = activityPlanningBaseRule.exceptionSlot.captured
                exception.apply { if (this is ClientRequestException) assertEquals(e, this.response.status.description)
                else assertEquals(e, this.message)
                }
                assertSnackbarTextEquals(R.string.could_not_plan_activities, snackbarScope)
            }
        }
    }
}