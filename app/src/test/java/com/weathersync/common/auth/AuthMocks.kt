package com.weathersync.common.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.auth.FirebaseUser
import com.weathersync.common.mockTask
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot

val userId = "uid"

fun mockAuth(
    exception: Exception? = null,
    user: FirebaseUser? = mockk<FirebaseUser> { every { uid } returns userId },
    authStateListenerSlot: CapturingSlot<AuthStateListener> = slot<AuthStateListener>()
): FirebaseAuth {
    return mockk {
        every { currentUser } returns user
        every { signOut() } answers {
            every { currentUser } returns null
        }
        every { addAuthStateListener(capture(authStateListenerSlot)) } answers {
            authStateListenerSlot.captured.onAuthStateChanged(this@mockk)
        }
        every { removeAuthStateListener(any()) } returns Unit
    }
}
