package com.weathersync.features.activityPlanning.repository

import com.weathersync.common.TestException
import com.weathersync.common.utils.BaseGenerationTest
import com.weathersync.common.utils.fetchedWeatherUnits
import com.weathersync.features.activityPlanning.ActivityPlanningBaseRule
import com.weathersync.features.settings.data.WeatherUnit
import com.weathersync.utils.AtLeastOneGenerationTagMissing
import com.weathersync.utils.EmptyGeminiResponse
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ActivityPlanningSuggestionsUnitTests: BaseGenerationTest {
    @get: Rule
    val activityPlanningBaseRule = ActivityPlanningBaseRule()

    @After
    fun after() {
        coVerify { activityPlanningBaseRule.weatherUnitsManager.getUnits() }
    }

    @Test(expected = TestException::class)
    override fun generateSuggestions_generationException() = runTest {
        activityPlanningBaseRule.setupActivityPlanningRepository(suggestionsGenerationException = activityPlanningBaseRule.testHelper.testException)
        val forecast = activityPlanningBaseRule.activityPlanningRepository.getForecast()
        activityPlanningBaseRule.activityPlanningRepository.generateRecommendations("", forecast)
    }
    @Test(expected = EmptyGeminiResponse::class)
    override fun generateSuggestions_limitNotReached_emptyGeminiResponse() = runTest {
        activityPlanningBaseRule.setupActivityPlanningRepository(generatedSuggestions = null)
        val forecast = activityPlanningBaseRule.activityPlanningRepository.getForecast()
        activityPlanningBaseRule.activityPlanningRepository.generateRecommendations("", forecast)
    }
    @Test(expected = AtLeastOneGenerationTagMissing::class)
    override fun generateSuggestions_limitNotReached_atLeastOneTagMissing() = runTest {
        val content = "Content with no tags"
        activityPlanningBaseRule.setupActivityPlanningRepository(generatedSuggestions = content)
        val forecast = activityPlanningBaseRule.activityPlanningRepository.getForecast()
        activityPlanningBaseRule.activityPlanningRepository.generateRecommendations("", forecast)
    }
    @Test
    override fun generateSuggestions_limitNotReached() = runTest {
        val forecast = activityPlanningBaseRule.activityPlanningRepository.getForecast()
        val generatedSuggestions = activityPlanningBaseRule.activityPlanningRepository.generateRecommendations("", forecast)
        assertEquals(activityPlanningBaseRule.activityPlanningSuggestions, generatedSuggestions)
        coVerify { activityPlanningBaseRule.generativeModel.generateContent(any<String>()) }
        assertTrue(forecast.forecast.all {
            it.temp.unit == fetchedWeatherUnits.first { it is WeatherUnit.Temperature }.unitName &&
                    it.windSpeed.unit == fetchedWeatherUnits.first { it is WeatherUnit.WindSpeed }.unitName &&
                    it.visibility.unit == fetchedWeatherUnits.first { it is WeatherUnit.Visibility }.unitName
        })
    }
}