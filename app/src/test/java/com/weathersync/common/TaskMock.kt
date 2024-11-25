package com.weathersync.common

import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import io.mockk.every
import io.mockk.mockk

fun <T: Any> mockTask(data: T? = null, taskException: Exception? = null): Task<T> {
    return mockk {
        every { result } returns data
        every { exception } returns taskException
        every { isCanceled } returns false
        every { isComplete } returns true
        every { isSuccessful } returns (taskException == null)

        every { addOnCompleteListener(any()) } answers {
            val listener = arg<OnCompleteListener<T>>(0)
            listener.onComplete(this@mockk)
            this@mockk
        }
    }
}