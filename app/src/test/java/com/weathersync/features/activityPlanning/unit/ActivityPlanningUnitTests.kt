package com.weathersync.features.activityPlanning.unit

import com.weathersync.common.utils.MainDispatcherRule
import com.weathersync.features.activityPlanning.ActivityPlanningBaseRule
import com.weathersync.features.activityPlanning.presentation.ActivityPlanningIntent
import com.weathersync.utils.AtLeastOneTagMissing
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test


class ActivityPlanningUnitTests {
    @get: Rule(order = 0)
    val dispatcherRule = MainDispatcherRule()
    @get: Rule(order = 1)
    val activityPlanningBaseRule = ActivityPlanningBaseRule()


    @Test
    fun generateRecommendations_success() = runTest {
        activityPlanningBaseRule.viewModel.handleIntent(ActivityPlanningIntent.GenerateRecommendations)
        activityPlanningBaseRule.advance(this)
        assertEquals(activityPlanningBaseRule.activityPlanningSuggestions, activityPlanningBaseRule.viewModel.uiState.value.generatedText)
        activityPlanningBaseRule.assertUrlIsCorrect()
    }

    @Test
    fun generateRecommendations_errorHttpResponse_error() = runTest {
        val status = HttpStatusCode.Forbidden
        activityPlanningBaseRule.setup(status = status)
        performActivityPlanning(this, status.description)
        activityPlanningBaseRule.assertUrlIsCorrect()
    }
    @Test
    fun generateRecommendations_geocoderException_error() = runTest {
        activityPlanningBaseRule.setup(geocoderException = activityPlanningBaseRule.exception)
        performActivityPlanning(this, activityPlanningBaseRule.exception.message)
    }
    @Test
    fun generateRecommendations_lastLocationException_error() = runTest {
        activityPlanningBaseRule.setup(lastLocationException = activityPlanningBaseRule.exception)
        performActivityPlanning(this, activityPlanningBaseRule.exception.message)
    }
    @Test
    fun generateRecommendations_suggestionsGenerationException_error() = runTest {
        activityPlanningBaseRule.setup(suggestionsGenerationException = activityPlanningBaseRule.exception)
        performActivityPlanning(this, activityPlanningBaseRule.exception.message)
        activityPlanningBaseRule.assertUrlIsCorrect()
    }
    @Test
    fun generateRecommendations_atLeastOneTagMissing_error() = runTest {
        activityPlanningBaseRule.setup(generatedSuggestions = "Content without tags")
        performActivityPlanning(this)
        activityPlanningBaseRule.assertUrlIsCorrect()
        assertTrue(activityPlanningBaseRule.exceptionSlot.captured is AtLeastOneTagMissing)
    }
    private fun performActivityPlanning(testScope: TestScope, message: String? = null) {
        activityPlanningBaseRule.viewModel.handleIntent(ActivityPlanningIntent.GenerateRecommendations)
        activityPlanningBaseRule.advance(testScope)
        message?.let { m ->
            val exception = activityPlanningBaseRule.exceptionSlot.captured
            exception.apply { if (this is ClientRequestException) assertEquals(m, this.response.status.description)
            else assertEquals(m, this.message)
            }
        }
    }
}