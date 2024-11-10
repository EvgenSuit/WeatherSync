package com.weathersync.common.testInterfaces

import com.weathersync.common.TestException
import com.weathersync.utils.AtLeastOneGenerationTagMissing
import com.weathersync.utils.EmptyGeminiResponse
import org.junit.Test

interface BaseGenerationTest {
    @Test(expected = TestException::class)
    fun generateSuggestions_generationException()

    @Test(expected = EmptyGeminiResponse::class)
    fun generateSuggestions_limitNotReached_emptyGeminiResponse()

    @Test(expected = AtLeastOneGenerationTagMissing::class)
    fun generateSuggestions_limitNotReached_atLeastOneTagMissing()

    @Test
    fun generateSuggestions_notSubscribed_limitNotReached()
    @Test
    fun generateSuggestions_isSubscribed_limitNotReached()
}