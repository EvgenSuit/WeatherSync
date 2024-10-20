package com.weathersync.features.activityPlanning.viewModel

import com.weathersync.common.utils.MainDispatcherRule
import com.weathersync.common.utils.createDescendingTimestamps
import com.weathersync.features.activityPlanning.ActivityPlanningBaseRule
import com.weathersync.features.activityPlanning.presentation.ActivityPlanningIntent
import com.weathersync.utils.AtLeastOneGenerationTagMissing
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import io.mockk.coVerify
import io.mockk.coVerifyAll
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test


class ActivityPlanningViewModelTests {
    @get: Rule(order = 0)
    val dispatcherRule = MainDispatcherRule()
    @get: Rule(order = 1)
    val activityPlanningBaseRule = ActivityPlanningBaseRule()
    private val input = "Some input"

    @Test
    fun generateRecommendations_success() = runTest {
        activityPlanningBaseRule.viewModel.apply {
            handleIntent(ActivityPlanningIntent.Input(input))
            handleIntent(ActivityPlanningIntent.GenerateRecommendations)
        }
        activityPlanningBaseRule.advance(this)
        assertEquals(activityPlanningBaseRule.activityPlanningSuggestions, activityPlanningBaseRule.viewModel.uiState.value.generatedText)
        activityPlanningBaseRule.assertUrlIsCorrect()
    }
    @Test
    fun generateRecommendations_limitReached() = runTest {
        activityPlanningBaseRule.apply {
            setupLimitManager(timestamps = createDescendingTimestamps(
                limitManagerConfig = activityPlanningBaseRule.limitManagerConfig,
                currTimeMillis = activityPlanningBaseRule.testClock.millis()
            ), limitManagerConfig = activityPlanningBaseRule.limitManagerConfig)
            setupActivityPlanningRepository()
            setupViewModel()
            viewModel.apply {
                handleIntent(ActivityPlanningIntent.Input(input))
                handleIntent(ActivityPlanningIntent.GenerateRecommendations)
            }
        }
        activityPlanningBaseRule.advance(this)
        assertEquals(null, activityPlanningBaseRule.viewModel.uiState.value.generatedText)
        coVerifyAll(inverse = true) {
            activityPlanningBaseRule.apply {
                activityPlanningRepository.recordTimestamp()
                geminiRepository.generateRecommendations(activity = any(), forecast = any())
            }
        }
    }

    @Test
    fun generateRecommendations_errorHttpResponse_error() = runTest {
        val status = HttpStatusCode.Forbidden
        activityPlanningBaseRule.apply {
            setupForecastRepository(status = status)
            setupActivityPlanningRepository()
            setupViewModel()
            viewModel.apply {
                handleIntent(ActivityPlanningIntent.Input(input))
                handleIntent(ActivityPlanningIntent.GenerateRecommendations)
            }
        }
        performActivityPlanning(this, status.description)
        activityPlanningBaseRule.assertUrlIsCorrect()
    }
    @Test
    fun generateRecommendations_geocoderException_error() = runTest {
        activityPlanningBaseRule.apply {
            setupForecastRepository(geocoderException = activityPlanningBaseRule.exception)
            setupActivityPlanningRepository()
            setupViewModel()
            viewModel.apply {
                handleIntent(ActivityPlanningIntent.Input(input))
                handleIntent(ActivityPlanningIntent.GenerateRecommendations)
            }
        }
        performActivityPlanning(this, activityPlanningBaseRule.exception.message)
    }
    @Test
    fun generateRecommendations_lastLocationException_error() = runTest {
        activityPlanningBaseRule.apply {
            setupForecastRepository(lastLocationException = activityPlanningBaseRule.exception)
            setupActivityPlanningRepository()
            setupViewModel()
            viewModel.apply {
                handleIntent(ActivityPlanningIntent.Input(input))
                handleIntent(ActivityPlanningIntent.GenerateRecommendations)
            }
        }
        performActivityPlanning(this, activityPlanningBaseRule.exception.message)
    }
    @Test
    fun generateRecommendations_suggestionsGenerationException() = runTest {
        activityPlanningBaseRule.apply {
            setupActivityPlanningRepository(suggestionsGenerationException = activityPlanningBaseRule.exception)
            setupViewModel()
            viewModel.apply {
                handleIntent(ActivityPlanningIntent.Input(input))
                handleIntent(ActivityPlanningIntent.GenerateRecommendations)
            }
        }
        performActivityPlanning(this, activityPlanningBaseRule.exception.message)
        activityPlanningBaseRule.assertUrlIsCorrect()
        coVerify(inverse = true) { activityPlanningBaseRule.activityPlanningRepository.recordTimestamp() }
    }
    @Test
    fun generateRecommendations_atLeastOneTagMissing() = runTest {
        activityPlanningBaseRule.apply {
            setupActivityPlanningRepository(generatedSuggestions = "Content without tags")
            setupViewModel()
            viewModel.apply {
                handleIntent(ActivityPlanningIntent.Input(input))
                handleIntent(ActivityPlanningIntent.GenerateRecommendations)
            }
        }
        performActivityPlanning(this)
        activityPlanningBaseRule.assertUrlIsCorrect()
        assertTrue(activityPlanningBaseRule.exceptionSlot.captured is AtLeastOneGenerationTagMissing)
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