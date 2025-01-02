package com.weathersync.utils

class SignInWithGoogleActivityResultException(message: String): Exception(message)

class LocationRequestException(message: String): Exception(message)

class NoGoogleMapsGeocodingResult(message: String): Exception(message)

class NullOpenAIResponse(message: String): Exception(message)
class NullGeminiResponse(message: String): Exception(message)

class AdLoadError(message: String): Exception(message)
class AdShowError(message: String): Exception(message)

class BillingServiceDisconnected(message: String): Exception(message)
class BillingServiceInitException(debugMessage: String): Exception(debugMessage)
class SubscriptionCheckException(debugMessage: String): Exception(debugMessage)
class PurchasesUpdatedException(message: String): Exception(message)
class PurchaseException(message: String): Exception(message)

class UnknownReviewException: Exception()

class NullFirebaseUser: Exception()