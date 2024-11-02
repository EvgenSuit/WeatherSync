package com.weathersync.common.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.auth.FirebaseUser
import com.weathersync.common.mockTask
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot

val username = "username"
val userId = "uid"

val validEmail = "some@gmail.com"
val validPassword = "Password2077$"
val invalidEmails = listOf("", " ", ", ", "some", "some@", "some@gmail", "some@gmail.")
val invalidPasswords = listOf("", " ", ", ", "password with whitespace", "shortPassword", "PasswordWithoutNumbers", "PasswordWithoutSymbols2033")
fun mockAuth(
    exception: Exception? = null,
    user: FirebaseUser? = mockk<FirebaseUser> { every { uid } returns userId }
): FirebaseAuth {
    return mockk {
        every { currentUser } returns user
        every { signInWithEmailAndPassword(any(), any()) } answers {
            every { currentUser } returns user
            mockTask(taskException = exception)
        }
        every { createUserWithEmailAndPassword(any(), any()) } answers {
            every { currentUser } returns user
            mockTask(taskException = exception)
        }

        every { signOut() } answers {
            every { currentUser } returns null
        }
    }
}
