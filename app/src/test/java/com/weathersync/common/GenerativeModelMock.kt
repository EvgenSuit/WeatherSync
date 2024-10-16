package com.weathersync.common

import com.google.ai.client.generativeai.GenerativeModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk

fun mockGenerativeModel(
    generatedContent: String?,
    generationException: Exception? = null
): GenerativeModel = mockk {
    coEvery { generateContent(any<String>()) } answers {
        if (generationException != null) throw generationException
        else mockk {
            every { text } returns generatedContent
        }
    }
}