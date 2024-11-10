package com.weathersync.features.home.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.weathersync.common.MainDispatcherRule
import com.weathersync.common.TestException
import com.weathersync.common.testInterfaces.BaseGenerationTest
import com.weathersync.common.weather.fetchedWeatherUnits
import com.weathersync.features.home.HomeBaseRule
import com.weathersync.features.home.data.Suggestions
import com.weathersync.features.home.getMockedWeather
import com.weathersync.features.home.toCurrentWeather
import com.weathersync.features.home.toSuggestions
import com.weathersync.utils.AtLeastOneGenerationTagMissing
import com.weathersync.utils.EmptyGeminiResponse
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeRepositorySuggestionsUnitTests: BaseGenerationTest {
    @get: Rule(order = 1)
    val homeBaseRule = HomeBaseRule()
    @get: Rule(order = 0)
    val dispatcherRule = MainDispatcherRule(homeBaseRule.testDispatcher)

    @Test(expected = TestException::class)
    override fun generateSuggestions_generationException() = runTest {
        homeBaseRule.setupHomeRepository(
            isSubscribed = false,
            suggestionsGenerationException = homeBaseRule.exception)
        generateSuggestions()
        coVerify { homeBaseRule.generativeModel.generateContent(any<String>()) }
        assertEquals(null, homeBaseRule.currentWeatherLocalDB.currentWeatherDao().getSuggestions())
    }
    @Test(expected = EmptyGeminiResponse::class)
    override fun generateSuggestions_limitNotReached_emptyGeminiResponse() = runTest {
        homeBaseRule.setupHomeRepository(
            isSubscribed = false,
            generatedSuggestions = null)
        assertEquals(null, homeBaseRule.currentWeatherLocalDB.currentWeatherDao().getSuggestions())
        generateSuggestions()
        coVerify { homeBaseRule.generativeModel.generateContent(any<String>()) }
        assertEquals(null, homeBaseRule.currentWeatherLocalDB.currentWeatherDao().getSuggestions())
    }
    @Test(expected = AtLeastOneGenerationTagMissing::class)
    override fun generateSuggestions_limitNotReached_atLeastOneTagMissing() = runTest {
        val content = "Content with no tags"
        homeBaseRule.setupHomeRepository(
            isSubscribed = false,
            generatedSuggestions = content)
        generateSuggestions()
        coVerify { homeBaseRule.generativeModel.generateContent(any<String>()) }
        assertEquals(null, homeBaseRule.currentWeatherLocalDB.currentWeatherDao().getSuggestions())
    }

    @Test
    override fun generateSuggestions_isSubscribed_limitNotReached() = runTest {
        homeBaseRule.setupHomeRepository(
            isSubscribed = true,
            generatedSuggestions = homeBaseRule.generatedSuggestions)
        val generatedSuggestions = generateSuggestions()
        val localSuggestions = homeBaseRule.currentWeatherLocalDB.currentWeatherDao().getSuggestions()
        coVerify { homeBaseRule.generativeModel.generateContent(any<String>()) }
        for (suggestions in listOf(generatedSuggestions, localSuggestions)) {
            assertEquals(homeBaseRule.testSuggestions.recommendedActivities, suggestions!!.recommendedActivities)
            assertEquals(homeBaseRule.testSuggestions.unrecommendedActivities, suggestions.unrecommendedActivities)
            assertEquals(homeBaseRule.testSuggestions.whatToBring, suggestions.whatToBring)
        }
    }
    @Test
    override fun generateSuggestions_notSubscribed_limitNotReached() = runTest {
        val generatedSuggestions = generateSuggestions()
        val localSuggestions = homeBaseRule.currentWeatherLocalDB.currentWeatherDao().getSuggestions()
        coVerify { homeBaseRule.generativeModel.generateContent(any<String>()) }
        for (suggestions in listOf(generatedSuggestions, localSuggestions)) {
            assertEquals(homeBaseRule.testSuggestions.recommendedActivities, suggestions!!.recommendedActivities)
            assertEquals(homeBaseRule.testSuggestions.unrecommendedActivities, suggestions.unrecommendedActivities)
            assertEquals(homeBaseRule.testSuggestions.whatToBring, suggestions.whatToBring)
        }
    }
    @Test
    fun generateSuggestions_limitReached_localSuggestionsAreNull() = runTest {
        val dao = homeBaseRule.currentWeatherLocalDB.currentWeatherDao()
        dao.insertWeather(getMockedWeather(fetchedWeatherUnits).toCurrentWeather())

        homeBaseRule.setupHomeRepository(
            isSubscribed = false,
            generatedSuggestions = homeBaseRule.generatedSuggestions)
        val suggestions = generateSuggestions(isLimitReached = true)
        val localSuggestions = dao.getSuggestions()
        coVerify(exactly = 0) { homeBaseRule.generativeModel.generateContent(any<String>()) }
        assertEquals(null, suggestions)
        assertEquals(null, localSuggestions)
    }
    @Test
    fun generateSuggestions_limitReached_localSuggestionsAreNotNull() = runTest {
        val dao = homeBaseRule.currentWeatherLocalDB.currentWeatherDao()
        dao.apply {
                insertWeather(getMockedWeather(fetchedWeatherUnits).toCurrentWeather())
                insertSuggestions(homeBaseRule.testSuggestions.toSuggestions())
        }
        homeBaseRule.setupHomeRepository(
            isSubscribed = false,
            generatedSuggestions = homeBaseRule.generatedSuggestions)
        val suggestions = generateSuggestions(isLimitReached = true)
        val localSuggestions = dao.getSuggestions()
        coVerify(exactly = 0) { homeBaseRule.generativeModel.generateContent(any<String>()) }
        assertEquals(homeBaseRule.testSuggestions.toSuggestions(), suggestions)
        assertEquals(homeBaseRule.testSuggestions.toSuggestions(), localSuggestions)
    }
    private suspend fun generateSuggestions(isLimitReached: Boolean = false): Suggestions? {
        val weather = homeBaseRule.homeRepository.getCurrentWeather(isLimitReached) ?: return null
        return homeBaseRule.homeRepository.generateSuggestions(isLimitReached, weather)
    }
}