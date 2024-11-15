package com.weathersync.utils

class LocationRequestException(message: String): Exception(message)

class EmptyGeminiResponse(message: String): Exception(message)
class AtLeastOneGenerationTagMissing(message: String) : Exception(message)

class AdLoadError(message: String): Exception(message)
class AdShowError(message: String): Exception(message)

class BillingServiceDisconnected(message: String): Exception(message)
class BillingServiceInitException(debugMessage: String): Exception(debugMessage)
class SubscriptionCheckException(debugMessage: String): Exception(debugMessage)
class PurchasesUpdatedException(message: String): Exception(message)
class PurchaseException(message: String): Exception(message)