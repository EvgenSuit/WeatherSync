package com.weathersync.features.home.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.weathersync.common.MainDispatcherRule
import com.weathersync.common.testInterfaces.BaseGenerationTest
import com.weathersync.common.weather.fetchedWeatherUnits
import com.weathersync.features.home.HomeBaseRule
import com.weathersync.features.home.data.Suggestions
import com.weathersync.features.home.getMockedWeather
import com.weathersync.features.home.toCurrentWeather
import com.weathersync.utils.NullGeminiResponse
import com.weathersync.utils.NullOpenAIResponse
import com.weathersync.utils.ai.gemini.GeminiClient
import com.weathersync.utils.ai.openai.OpenAIClient
import com.weathersync.utils.subscription.IsSubscribed
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertFailsWith

@RunWith(AndroidJUnit4::class)
class HomeRepositorySuggestionsUnitTests: BaseGenerationTest {
    @get: Rule(order = 1)
    val homeBaseRule = HomeBaseRule()
    // use this rule in order to make runTest run with the specified dispatcher
    @get: Rule(order = 0)
    val dispatcherRule = MainDispatcherRule(homeBaseRule.testDispatcher)

    @Test
    override fun generateSuggestions_notSubscribed_generationException() = runTest {
        homeBaseRule.setupHomeRepository(httpStatusCode = HttpStatusCode.Forbidden)
        assertFailsWith<ClientRequestException> { generateSuggestions(isSubscribed = false) }
        homeBaseRule.testHelper.verifyAIClientCall(
            aiClient = homeBaseRule.aiClientProvider.getAIClient(false),
            expectedType = GeminiClient::class
        )
    }
    @Test
    override fun generateSuggestions_isSubscribed_generationException() = runTest {
        homeBaseRule.setupHomeRepository(httpStatusCode = HttpStatusCode.Forbidden)
        assertFailsWith<ClientRequestException> { generateSuggestions(isSubscribed = true) }
        homeBaseRule.testHelper.verifyAIClientCall(
            aiClient = homeBaseRule.aiClientProvider.getAIClient(true),
            expectedType = OpenAIClient::class
        )
    }
    @Test
    override fun generateSuggestions_notSubscribed_emptyRegularResponse() = runTest {
        homeBaseRule.setupHomeRepository(generatedSuggestions = null)
        assertEquals(null, homeBaseRule.currentWeatherLocalDB.currentWeatherDao().getSuggestions())
        assertFailsWith<NullGeminiResponse> { generateSuggestions(isSubscribed = false) }
        homeBaseRule.testHelper.verifyAIClientCall(
            aiClient = homeBaseRule.aiClientProvider.getAIClient(false),
            expectedType = GeminiClient::class
        )
    }
    @Test
    override fun generateSuggestions_isSubscribed_emptyPremiumResponse() = runTest {
        homeBaseRule.setupHomeRepository(generatedSuggestions = null)
        assertEquals(null, homeBaseRule.currentWeatherLocalDB.currentWeatherDao().getSuggestions())
        assertFailsWith<NullOpenAIResponse> { generateSuggestions(isSubscribed = true) }
        homeBaseRule.testHelper.verifyAIClientCall(
            aiClient = homeBaseRule.aiClientProvider.getAIClient(true),
            expectedType = OpenAIClient::class
        )
    }

    @Test
    override fun generateSuggestions_isSubscribed_limitNotReached() = runTest {
        val generatedSuggestions = generateSuggestions(isSubscribed = true)
        homeBaseRule.testHelper.verifyAIClientCall(
            aiClient = homeBaseRule.aiClientProvider.getAIClient(true),
            expectedType = OpenAIClient::class
        )
        assertEquals(homeBaseRule.testSuggestions, generatedSuggestions)
    }
    @Test
    override fun generateSuggestions_notSubscribed_limitNotReached() = runTest {
        val generatedSuggestions = generateSuggestions(isSubscribed = false)
        homeBaseRule.testHelper.verifyAIClientCall(
            aiClient = homeBaseRule.aiClientProvider.getAIClient(false),
            expectedType = GeminiClient::class
        )
        assertEquals(homeBaseRule.testSuggestions, generatedSuggestions)
    }
    @Test
    fun generateSuggestions_limitReached_localSuggestionsAreNull() = runTest {
        val dao = homeBaseRule.currentWeatherLocalDB.currentWeatherDao()
        dao.insertWeather(getMockedWeather(fetchedWeatherUnits).toCurrentWeather())

        homeBaseRule.setupHomeRepository(generatedSuggestions = homeBaseRule.testSuggestions)
        val suggestions = generateSuggestions(isLimitReached = true, isSubscribed = false)
        val localSuggestions = dao.getSuggestions()
        coVerify(inverse = true) { homeBaseRule.aiClientProvider.getAIClient(any())}
        assertEquals(Suggestions(), suggestions)
        assertEquals(null, localSuggestions)
    }
    @Test
    fun generateSuggestions_limitReached_localSuggestionsAreNotNull() = runTest {
        val dao = homeBaseRule.currentWeatherLocalDB.currentWeatherDao()
        dao.apply {
            insertWeather(getMockedWeather(fetchedWeatherUnits).toCurrentWeather())
            insertSuggestions(homeBaseRule.testSuggestions)
        }
        homeBaseRule.setupHomeRepository(generatedSuggestions = homeBaseRule.testSuggestions)
        val suggestions = generateSuggestions(isLimitReached = true, isSubscribed = false)
        val localSuggestions = dao.getSuggestions()
        coVerify(inverse = true) { homeBaseRule.aiClientProvider.getAIClient(any()) }
        assertEquals(homeBaseRule.testSuggestions, suggestions)
        assertEquals(homeBaseRule.testSuggestions, localSuggestions)
    }
    private suspend fun generateSuggestions(
        isLimitReached: Boolean = false,
        isSubscribed: IsSubscribed): Suggestions? {
        val weather = homeBaseRule.homeRepository.getCurrentWeather(isLimitReached) ?: return null
        return homeBaseRule.homeRepository.generateSuggestions(
            isLimitReached = isLimitReached,
            isSubscribed = isSubscribed,
            currentWeather = weather)
    }
}