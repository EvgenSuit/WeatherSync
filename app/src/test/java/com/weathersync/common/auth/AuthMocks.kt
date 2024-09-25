package com.weathersync.common.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.weathersync.common.mockTask
import io.mockk.every
import io.mockk.mockk

val username = "username"
val userId = "uid"

val validEmail = "some@gmail.com"
val validPassword = "Password2077$"
val invalidEmails = listOf("", " ", ", ", "some", "some@", "some@gmail", "some@gmail.")
val invalidPasswords = listOf("", " ", ", ", "password with whitespace", "shortPassword", "PasswordWithoutNumbers", "PasswordWithoutSymbols2033")
fun mockAuth(
    exception: Exception? = null
): FirebaseAuth {
    val user = mockk<FirebaseUser> {
        every { uid } returns userId
    }
    return mockk {
        every { currentUser } returns user
        every { signInWithEmailAndPassword(any(), any()) } returns mockTask(taskException = exception)
        every { createUserWithEmailAndPassword(any(), any()) } returns mockTask(taskException = exception)
    }
}
