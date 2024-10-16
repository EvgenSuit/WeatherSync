package com.weathersync.utils

interface GeminiRepository {

    fun extractContentWithTags(
        prompt: String,
        content: String,
        tags: List<String>
    ): List<List<String>> {
        val extractedContent = mutableMapOf<String, String>()
        tags.forEach { tag ->
            val escapedTag = Regex.escape(tag)
            val regex = Regex("(?<=${escapedTag})(.*?)(?=${escapedTag})", RegexOption.DOT_MATCHES_ALL)
            val match = regex.find(content)?.value?.trim() ?: ""
            extractedContent[tag] = match.trim()
        }
        if (extractedContent.values.any { it.isEmpty() })
            throw AtLeastOneGenerationTagMissing("There's at least 1 tag missing in response from Gemini: \n" +
                    " Extracted content: $extractedContent. \n Prompt: ${prompt.take(200)}..... \n Plain response: $content")
        return extractedContent.map { it.value.trim().split("\n") }
    }
}