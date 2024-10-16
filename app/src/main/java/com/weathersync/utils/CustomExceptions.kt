package com.weathersync.utils

class EmptyGeminiResponse(message: String): Exception(message)
class AtLeastOneGenerationTagMissing(message: String) : Exception(message)
