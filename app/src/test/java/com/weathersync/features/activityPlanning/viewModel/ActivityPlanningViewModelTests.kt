package com.weathersync.features.activityPlanning.viewModel

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.weathersync.common.MainDispatcherRule
import com.weathersync.common.utils.createDescendingTimestamps
import com.weathersync.features.activityPlanning.ActivityPlanningBaseRule
import com.weathersync.features.activityPlanning.presentation.ActivityPlanningIntent
import com.weathersync.utils.AtLeastOneGenerationTagMissing
import com.weathersync.utils.CustomResult
import com.weathersync.utils.FirebaseEvent
import com.weathersync.utils.subscription.IsSubscribed
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import io.mockk.coVerify
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActivityPlanningViewModelTests {
    @get: Rule(order = 1)
    val activityPlanningBaseRule = ActivityPlanningBaseRule()
    @get: Rule(order = 0)
    val dispatcherRule = MainDispatcherRule(activityPlanningBaseRule.testDispatcher)
    private val input = "Some input"

    @Test
    fun generateRecommendations_notSubscribed_success() = runTest {
        performActivityPlanning(testScope = this,
            isSubscribed = false,
            success = true)
    }
    @Test
    fun generateRecommendations_isSubscribed_success() = runTest {
        activityPlanningBaseRule.apply {
            setupActivityPlanningRepository(
                isSubscribed = true,
                generatedSuggestions = generatedSuggestions)
            setupViewModel()
        }
        performActivityPlanning(testScope = this,
            isSubscribed = true,
            success = true)
    }
    @Test
    fun generateRecommendations_limitReached() = runTest {
        activityPlanningBaseRule.apply {
            setupLimitManager(
                timestamps = createDescendingTimestamps(
                limitManagerConfig = activityPlanningBaseRule.regularLimitManagerConfig,
                currTimeMillis = activityPlanningBaseRule.testClock.millis()
            ))
            setupActivityPlanningRepository(isSubscribed = false)
            setupViewModel()
        }
        performActivityPlanning(testScope = this,
            isSubscribed = false,
            success = true,
            isLimitReached = true)
        activityPlanningBaseRule.testHelper.verifyAnalyticsEvent(FirebaseEvent.ACTIVITY_PLANNING_LIMIT, false,
            "next_generation_time" to activityPlanningBaseRule.viewModel.uiState.value.limit.nextUpdateDateTime!!.toString())
        activityPlanningBaseRule.testHelper.verifyAnalyticsEvent(FirebaseEvent.PLAN_ACTIVITIES, true)
    }

    @Test
    fun generateRecommendations_errorHttpResponse_error() = runTest {
        val status = HttpStatusCode.Forbidden
        activityPlanningBaseRule.apply {
            setupForecastRepository(status = status)
            setupActivityPlanningRepository(isSubscribed = false)
            setupViewModel()
        }
        performActivityPlanning(this,
            isSubscribed = false,
            success = false,
            message = status.description)
        activityPlanningBaseRule.assertUrlAndDatesAreCorrect()
    }
    @Test
    fun generateRecommendations_geocoderException_error() = runTest {
        activityPlanningBaseRule.apply {
            setupForecastRepository(geocoderException = activityPlanningBaseRule.testHelper.testException)
            setupActivityPlanningRepository(isSubscribed = false)
            setupViewModel()
        }
        performActivityPlanning(this,
            isSubscribed = false,
            success = false)
    }
    @Test
    fun generateRecommendations_lastLocationException_error() = runTest {
        activityPlanningBaseRule.apply {
            setupForecastRepository(lastLocationException = activityPlanningBaseRule.testHelper.testException)
            setupActivityPlanningRepository(isSubscribed = false)
            setupViewModel()
        }
        performActivityPlanning(this,
            isSubscribed = false,
            success = false)
    }
    @Test
    fun generateRecommendations_suggestionsGenerationException() = runTest {
        activityPlanningBaseRule.apply {
            setupActivityPlanningRepository(
                isSubscribed = false,
                suggestionsGenerationException = activityPlanningBaseRule.testHelper.testException)
            setupViewModel()
        }
        performActivityPlanning(this,
            isSubscribed = false,
            success = false)
        activityPlanningBaseRule.assertUrlAndDatesAreCorrect()
        coVerify(inverse = true) { activityPlanningBaseRule.activityPlanningRepository.recordTimestamp() }
    }
    @Test
    fun generateRecommendations_atLeastOneTagMissing() = runTest {
        activityPlanningBaseRule.apply {
            setupActivityPlanningRepository(
                isSubscribed = false,
                generatedSuggestions = "Content without tags")
            setupViewModel()
        }
        performActivityPlanning(this,
            isSubscribed = false,
            success = false)
        activityPlanningBaseRule.assertUrlAndDatesAreCorrect()
        assertTrue(activityPlanningBaseRule.testHelper.exceptionSlot.captured is AtLeastOneGenerationTagMissing)
    }
    private fun performActivityPlanning(testScope: TestScope,
                                        isSubscribed: IsSubscribed,
                                        success: Boolean,
                                        message: String? = null,
                                        isLimitReached: Boolean = false) {
        activityPlanningBaseRule.viewModel.apply {
            handleIntent(ActivityPlanningIntent.Input(input))
            handleIntent(ActivityPlanningIntent.GenerateRecommendations)
        }
        activityPlanningBaseRule.advance(testScope)
        if (success) {
            assertEquals(CustomResult.Success, activityPlanningBaseRule.viewModel.uiState.value.generationResult)
            if (!isLimitReached) {
                assertEquals(activityPlanningBaseRule.activityPlanningSuggestions, activityPlanningBaseRule.viewModel.uiState.value.generatedText)
                activityPlanningBaseRule.assertUrlAndDatesAreCorrect(isSubscribed = isSubscribed)
            }
            else {
                assertNotEquals(activityPlanningBaseRule.activityPlanningSuggestions, activityPlanningBaseRule.viewModel.uiState.value.generatedText)
            }
            activityPlanningBaseRule.apply {
                activityPlanningRepository.apply {
                    coVerify { calculateLimit(isSubscribed) }
                    coVerify(inverse = isLimitReached) { getForecast(isSubscribed) }
                    coVerify(inverse = isLimitReached) { generateRecommendations(activity = any(), forecast = any()) }
                    coVerify(inverse = isLimitReached) { recordTimestamp() }
                }
                // verify that PLAN_ACTIVITIES was logged if the limit was not reached
                testHelper.verifyAnalyticsEvent(FirebaseEvent.PLAN_ACTIVITIES, isLimitReached)
                // verify that ACTIVITY_PLANNING_LIMIT was logged if the limit was reached (when isLimitReached is true, it would result
                // in verify call to accept inverse as false)
                testHelper.verifyAnalyticsEvent(FirebaseEvent.ACTIVITY_PLANNING_LIMIT, !isLimitReached)
            }
        } else {
            assertEquals(CustomResult.Error, activityPlanningBaseRule.viewModel.uiState.value.generationResult)
            assertEquals(null, activityPlanningBaseRule.viewModel.uiState.value.generatedText)
        }
        message?.let { m ->
            val exception = activityPlanningBaseRule.testHelper.exceptionSlot.captured
            exception.apply { if (this is ClientRequestException) assertEquals(m, this.response.status.description)
            else assertEquals(m, this.message)
            }
        }
    }
}