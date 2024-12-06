package com.weathersync.utils.ai.data

/**
 * @param useStructuredOutput set this to true when generating suggestions for the home page
 */
data class GenerationOptions(
    val systemInstructions: String,
    val prompt: String,
    val useStructuredOutput: Boolean,
    val maxOutputTokens: Int,
    val temperature: Double = 1.0,
    val topP: Double = 1.0,
    val topK: Int = 10
)
