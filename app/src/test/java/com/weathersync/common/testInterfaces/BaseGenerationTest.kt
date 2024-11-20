package com.weathersync.common.testInterfaces

import org.junit.Test

interface BaseGenerationTest {
    @Test
    fun generateSuggestions_notSubscribed_generationException()
    @Test
    fun generateSuggestions_isSubscribed_generationException()

    @Test
    fun generateSuggestions_notSubscribed_emptyRegularResponse()
    @Test
    fun generateSuggestions_isSubscribed_emptyPremiumResponse()

    @Test
    fun generateSuggestions_notSubscribed_limitNotReached()
    @Test
    fun generateSuggestions_isSubscribed_limitNotReached()
}