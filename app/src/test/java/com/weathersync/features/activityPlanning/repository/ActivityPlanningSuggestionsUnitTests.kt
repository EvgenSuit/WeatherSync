package com.weathersync.features.activityPlanning.repository

import com.weathersync.common.MainDispatcherRule
import com.weathersync.common.TestException
import com.weathersync.common.testInterfaces.BaseGenerationTest
import com.weathersync.common.weather.fetchedWeatherUnits
import com.weathersync.features.activityPlanning.ActivityPlanningBaseRule
import com.weathersync.features.activityPlanning.ForecastDays
import com.weathersync.features.settings.data.WeatherUnit
import com.weathersync.utils.AtLeastOneGenerationTagMissing
import com.weathersync.utils.EmptyGeminiResponse
import com.weathersync.utils.subscription.IsSubscribed
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ActivityPlanningSuggestionsUnitTests: BaseGenerationTest {
    @get: Rule(order = 1)
    val activityPlanningBaseRule = ActivityPlanningBaseRule()
    @get: Rule(order = 0)
    val dispatcherRule = MainDispatcherRule(activityPlanningBaseRule.testDispatcher)

    @After
    fun after() {
        coVerify { activityPlanningBaseRule.weatherUnitsManager.getUnits() }
    }

    @Test(expected = TestException::class)
    override fun generateSuggestions_generationException() = runTest {
        activityPlanningBaseRule.setupActivityPlanningRepository(
            isSubscribed = false,
            suggestionsGenerationException = activityPlanningBaseRule.testHelper.testException)
        val forecast = activityPlanningBaseRule.activityPlanningRepository.getForecast(true)
        activityPlanningBaseRule.activityPlanningRepository.generateRecommendations("", forecast)
    }
    @Test(expected = EmptyGeminiResponse::class)
    override fun generateSuggestions_limitNotReached_emptyGeminiResponse() = runTest {
        activityPlanningBaseRule.setupActivityPlanningRepository(
            isSubscribed = false,
            generatedSuggestions = null)
        val forecast = activityPlanningBaseRule.activityPlanningRepository.getForecast(true)
        activityPlanningBaseRule.activityPlanningRepository.generateRecommendations("", forecast)
    }
    @Test(expected = AtLeastOneGenerationTagMissing::class)
    override fun generateSuggestions_limitNotReached_atLeastOneTagMissing() = runTest {
        val content = "Content with no tags"
        activityPlanningBaseRule.setupActivityPlanningRepository(
            isSubscribed = false,
            generatedSuggestions = content)
        val forecast = activityPlanningBaseRule.activityPlanningRepository.getForecast(true)
        activityPlanningBaseRule.activityPlanningRepository.generateRecommendations("", forecast)
    }
    @Test
    override fun generateSuggestions_notSubscribed_limitNotReached() = runTest {
        generateSuggestions_limitNotReachedUtil(isSubscribed = false)
    }
    @Test
    override fun generateSuggestions_isSubscribed_limitNotReached() = runTest {
        generateSuggestions_limitNotReachedUtil(isSubscribed = true)
    }

    private suspend fun generateSuggestions_limitNotReachedUtil(isSubscribed: IsSubscribed) {
        activityPlanningBaseRule.setupActivityPlanningRepository(
            isSubscribed = isSubscribed,
            generatedSuggestions = activityPlanningBaseRule.generatedSuggestions)
        val forecast = activityPlanningBaseRule.activityPlanningRepository.getForecast(isSubscribed)
        val generatedSuggestions = activityPlanningBaseRule.activityPlanningRepository.generateRecommendations("", forecast)
        assertEquals(activityPlanningBaseRule.activityPlanningSuggestions, generatedSuggestions)
        coVerify { activityPlanningBaseRule.generativeModel.generateContent(any<String>()) }
        assertTrue(forecast.forecast.all {
            it.temp.unit == fetchedWeatherUnits.first { it is WeatherUnit.Temperature }.unitName &&
                    it.windSpeed.unit == fetchedWeatherUnits.first { it is WeatherUnit.WindSpeed }.unitName &&
                    it.visibility.unit == fetchedWeatherUnits.first { it is WeatherUnit.Visibility }.unitName
        })
        assertEquals((if (isSubscribed) ForecastDays.PREMIUM else ForecastDays.REGULAR).days, forecast.forecastDays)
    }
}