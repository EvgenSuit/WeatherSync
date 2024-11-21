package com.weathersync.features.activityPlanning.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.weathersync.common.MainDispatcherRule
import com.weathersync.common.testInterfaces.BaseGenerationTest
import com.weathersync.common.weather.fetchedWeatherUnits
import com.weathersync.features.activityPlanning.ActivityPlanningBaseRule
import com.weathersync.features.activityPlanning.ForecastDays
import com.weathersync.features.settings.data.WeatherUnit
import com.weathersync.utils.NullGeminiResponse
import com.weathersync.utils.NullOpenAIResponse
import com.weathersync.utils.ai.gemini.GeminiClient
import com.weathersync.utils.ai.openai.OpenAIClient
import com.weathersync.utils.subscription.IsSubscribed
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertFailsWith

@RunWith(AndroidJUnit4::class)
class ActivityPlanningSuggestionsUnitTests: BaseGenerationTest {
    @get: Rule(order = 1)
    val activityPlanningBaseRule = ActivityPlanningBaseRule()
    @get: Rule(order = 0)
    val dispatcherRule = MainDispatcherRule(activityPlanningBaseRule.testDispatcher)

    @After
    fun after() {
        coVerify { activityPlanningBaseRule.weatherUnitsManager.getUnits() }
    }

    @Test
    override fun generateSuggestions_notSubscribed_generationException() = runTest {
        activityPlanningBaseRule.setupActivityPlanningRepository(generationHttpStatusCode = HttpStatusCode.Forbidden)
        val forecast = activityPlanningBaseRule.activityPlanningRepository.getForecast(false)
        assertFailsWith<ClientRequestException> {
            activityPlanningBaseRule.activityPlanningRepository.generateRecommendations(
                activity = "",
                isSubscribed = false,
                forecast = forecast)
        }
        activityPlanningBaseRule.testHelper.verifyAIClientCall(
            aiClient = activityPlanningBaseRule.aiClientProvider.getAIClient(false),
            expectedType = GeminiClient::class
        )
    }
    @Test
    override fun generateSuggestions_isSubscribed_generationException() = runTest {
        activityPlanningBaseRule.setupActivityPlanningRepository(generationHttpStatusCode = HttpStatusCode.Forbidden)
        val forecast = activityPlanningBaseRule.activityPlanningRepository.getForecast(true)
        assertFailsWith<ClientRequestException> {
            activityPlanningBaseRule.activityPlanningRepository.generateRecommendations(
                activity = "",
                isSubscribed = true,
                forecast = forecast)
        }
        activityPlanningBaseRule.testHelper.verifyAIClientCall(
            aiClient = activityPlanningBaseRule.aiClientProvider.getAIClient(true),
            expectedType = OpenAIClient::class
        )
    }
    @Test
    override fun generateSuggestions_notSubscribed_emptyRegularResponse() = runTest {
        activityPlanningBaseRule.setupActivityPlanningRepository(generatedSuggestions = null)
        val forecast = activityPlanningBaseRule.activityPlanningRepository.getForecast(false)
        assertFailsWith<NullGeminiResponse> {
            activityPlanningBaseRule.activityPlanningRepository.generateRecommendations(
                activity = "",
                isSubscribed = false,
                forecast = forecast)
        }
        activityPlanningBaseRule.testHelper.verifyAIClientCall(
            aiClient = activityPlanningBaseRule.aiClientProvider.getAIClient(false),
            expectedType = GeminiClient::class
        )
    }
    @Test
    override fun generateSuggestions_isSubscribed_emptyPremiumResponse() = runTest {
        activityPlanningBaseRule.setupActivityPlanningRepository(generatedSuggestions = null)
        val forecast = activityPlanningBaseRule.activityPlanningRepository.getForecast(true)
        assertFailsWith<NullOpenAIResponse> {
            activityPlanningBaseRule.activityPlanningRepository.generateRecommendations(
                activity = "",
                isSubscribed = true,
                forecast = forecast)
        }
        activityPlanningBaseRule.testHelper.verifyAIClientCall(
            aiClient = activityPlanningBaseRule.aiClientProvider.getAIClient(true),
            expectedType = OpenAIClient::class
        )
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
            generatedSuggestions = activityPlanningBaseRule.activityPlanningSuggestions)
        val forecast = activityPlanningBaseRule.activityPlanningRepository.getForecast(isSubscribed)
        val generatedSuggestions = activityPlanningBaseRule.activityPlanningRepository.generateRecommendations(
            activity = "",
            isSubscribed = isSubscribed,
            forecast = forecast)
        assertEquals(activityPlanningBaseRule.activityPlanningSuggestions, generatedSuggestions)
       // coVerify { activityPlanningBaseRule.generativeModel.generateContent(any<String>()) }
        assertTrue(forecast.forecast.all {
            it.temp.unit == fetchedWeatherUnits.first { it is WeatherUnit.Temperature }.unitName &&
                    it.windSpeed.unit == fetchedWeatherUnits.first { it is WeatherUnit.WindSpeed }.unitName &&
                    it.visibility.unit == fetchedWeatherUnits.first { it is WeatherUnit.Visibility }.unitName
        })
        assertEquals((if (isSubscribed) ForecastDays.PREMIUM else ForecastDays.REGULAR).days, forecast.forecastDays)
    }
}