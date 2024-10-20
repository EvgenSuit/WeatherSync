package com.weathersync.features.activityPlanning.repository

import com.weathersync.common.TestException
import com.weathersync.common.utils.BaseGenerationTest
import com.weathersync.features.activityPlanning.ActivityPlanningBaseRule
import com.weathersync.utils.AtLeastOneGenerationTagMissing
import com.weathersync.utils.EmptyGeminiResponse
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ActivityPlanningSuggestionsUnitTests: BaseGenerationTest {
    @get: Rule
    val activityPlanningBaseRule = ActivityPlanningBaseRule()

    @Test(expected = TestException::class)
    override fun generateSuggestions_generationException() = runTest {
        activityPlanningBaseRule.setupActivityPlanningRepository(suggestionsGenerationException = activityPlanningBaseRule.exception)
        generateSuggestions()
    }
    @Test(expected = EmptyGeminiResponse::class)
    override fun generateSuggestions_limitNotReached_emptyGeminiResponse() = runTest {
        activityPlanningBaseRule.setupActivityPlanningRepository(generatedSuggestions = null)
        generateSuggestions()
    }
    @Test(expected = AtLeastOneGenerationTagMissing::class)
    override fun generateSuggestions_limitNotReached_atLeastOneTagMissing() = runTest {
        val content = "Content with no tags"
        activityPlanningBaseRule.setupActivityPlanningRepository(generatedSuggestions = content)
        generateSuggestions()
    }
    @Test
    override fun generateSuggestions_limitNotReached() = runTest {
        val generatedSuggestions = generateSuggestions()
        assertEquals(activityPlanningBaseRule.activityPlanningSuggestions, generatedSuggestions)
        coVerify { activityPlanningBaseRule.generativeModel.generateContent(any<String>()) }
    }
    private suspend fun generateSuggestions() =
        activityPlanningBaseRule.activityPlanningRepository.generateRecommendations("Some text")
}