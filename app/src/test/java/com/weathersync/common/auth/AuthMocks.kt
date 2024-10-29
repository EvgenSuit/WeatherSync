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
    exception: Exception? = null
): FirebaseAuth {
    val authStateListenerSlot = slot<AuthStateListener>()
    val user = mockk<FirebaseUser> {
        every { uid } returns userId
    }
    return mockk {
        every { currentUser } returns user
        every { signInWithEmailAndPassword(any(), any()) } answers {
            if (authStateListenerSlot.isCaptured) authStateListenerSlot.captured.onAuthStateChanged(
                mockk { every { currentUser } returns user }
            )
            mockTask(taskException = exception)
        }
        every { createUserWithEmailAndPassword(any(), any()) } answers {
            if (authStateListenerSlot.isCaptured) authStateListenerSlot.captured.onAuthStateChanged(
                mockk { every { currentUser } returns user }
            )
            mockTask(taskException = exception)
        }

        every { addAuthStateListener(capture(authStateListenerSlot)) } answers {
            every { currentUser } returns user
            authStateListenerSlot.captured.onAuthStateChanged(this@mockk)
        }
        every { removeAuthStateListener(any()) } returns Unit
        every { signOut() } answers {
            every { currentUser } returns null
            if (authStateListenerSlot.isCaptured) authStateListenerSlot.captured.onAuthStateChanged(
                mockk { every { currentUser } returns null }
            )
        }
    }
}
