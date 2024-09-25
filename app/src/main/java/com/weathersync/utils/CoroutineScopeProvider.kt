package com.weathersync.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.plus

// When using a SupervisorJob, if a child coroutine fails, it does not cancel its siblings or the parent scope.
// This means other coroutines in the same scope can continue running, even if one of them fails
class CoroutineScopeProvider(private val scope: CoroutineScope? = null,
    private val dispatcher: CoroutineDispatcher) {
    operator fun invoke(inputScope: CoroutineScope): CoroutineScope =
        CoroutineScope((scope ?: inputScope).coroutineContext + dispatcher + SupervisorJob())
}