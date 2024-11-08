package com.weathersync.utils

class EmptyGeminiResponse(message: String): Exception(message)
class AtLeastOneGenerationTagMissing(message: String) : Exception(message)


class BillingServiceDisconnected: Exception()
class BillingServiceInitException(debugMessage: String): Exception(debugMessage)
class SubscriptionCheckException(debugMessage: String): Exception(debugMessage)
class PurchasesUpdatedException(message: String): Exception(message)