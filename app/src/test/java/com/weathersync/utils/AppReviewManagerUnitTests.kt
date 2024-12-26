package com.weathersync.utils

import com.google.android.play.core.review.ReviewManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.weathersync.common.TestClock
import com.weathersync.common.TestException
import com.weathersync.common.TestHelper
import com.weathersync.common.auth.mockAuth
import com.weathersync.common.auth.userId
import com.weathersync.common.mockTask
import com.weathersync.common.utils.mockTimeAPI
import com.weathersync.utils.appReview.AppReviewFirestoreRef
import com.weathersync.utils.appReview.AppReviewManager
import com.weathersync.utils.appReview.data.RateDialog
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.Date
import kotlin.test.assertFailsWith

class AppReviewManagerUnitTests {
    private val exception = TestException()
    private val testHelper = TestHelper()
    private lateinit var appReviewManager: AppReviewManager
    private lateinit var timeAPI: TimeAPI
    private lateinit var firestore: FirebaseFirestore
    private lateinit var manager: ReviewManager

    private fun setupFakeReviewManager(
        requestReviewFlowException: Exception? = null,
        launchReviewFlowException: Exception? = null
    ) {
        manager = mockk<ReviewManager> {
            every { requestReviewFlow() } returns mockTask(mockk(relaxed = true), requestReviewFlowException)
            every { launchReviewFlow(any(), any()) } returns mockTask(mockk(relaxed = true), launchReviewFlowException)
        }
    }
    private fun setupTimeAPI(statusCode: HttpStatusCode) {
        timeAPI = spyk(mockTimeAPI(
            statusCode = statusCode,
            currTimeMillis = testHelper.testClock.millis()
        ))
    }
    private fun setupFirestore(
        didShow: Boolean = false,
        timeMillis: Long? = null,
        fetchException: Exception? = null,
    ) {
        firestore = mockk {
            every { collection(userId).document(AppReviewFirestoreRef.SHOW_RATE_DIALOG.refName).get() } returns mockTask(
                data = mockk {
                    every { toObject(RateDialog::class.java) } returns if (timeMillis != null) RateDialog(Date(timeMillis), didShow) else null
                },
                taskException = fetchException
            )
            every { collection(userId).document(AppReviewFirestoreRef.SHOW_RATE_DIALOG.refName)
                .set(any<RateDialog>()) } returns mockTask()
        }
    }

    private fun setupReviewManager(auth: FirebaseAuth = mockAuth()) {
        appReviewManager = AppReviewManager(
            timeAPI = timeAPI,
            auth = auth,
            firestore = firestore,
            manager = manager
        )
    }
    private fun setup() {
        testHelper.testClock.setInstant(Instant.ofEpochSecond(15 * 24 * 60 * 60))
        setupFakeReviewManager()
        setupTimeAPI(statusCode = HttpStatusCode.OK)
        setupFirestore(timeMillis = testHelper.testClock.millis())
        setupReviewManager()
    }

    @Before
    fun before() {
        setup()
    }

    @Test
    fun userNull_reviewRequestNotMade() = runTest {
        setupFirestore()
        setupReviewManager(auth = mockAuth(user = null))
        val job = launch { appReviewManager.requestReviewFlow(mockk()) }
        testHelper.advance(this)

        verify(inverse = true) { firestore.collection(userId).document(AppReviewFirestoreRef.SHOW_RATE_DIALOG.refName)
            .set(any<RateDialog>()) }
        coVerify(inverse = true) { timeAPI.getRealDateTime() }
        verify(inverse = true) { manager.requestReviewFlow() }
        job.cancel()
    }

    @Test
    fun entryNull_isNotEligible() = runTest {
        setupFirestore(timeMillis = null)
        setupReviewManager()

        val job = launch { appReviewManager.requestReviewFlow(mockk()) }
        testHelper.advance(this)

        verify(exactly = 1) { firestore.collection(userId).document(AppReviewFirestoreRef.SHOW_RATE_DIALOG.refName)
            .set(any<RateDialog>()) }
        verify(inverse = true) { manager.requestReviewFlow() }

        job.cancel()
    }

    @Test
    fun entryNotNull_isNotEligible() = runTest {
        // test a scenario where first entry date and the current date are equal
        val job = launch { appReviewManager.requestReviewFlow(mockk()) }
        testHelper.advance(this)
        verify(inverse = true) { manager.requestReviewFlow() }
        job.cancel()
    }
    @Test
    fun didShow_isNotEligible() = runTest {
        setupFirestore(didShow = true)
        setupReviewManager()
        val job = launch { appReviewManager.requestReviewFlow(mockk()) }
        testHelper.advance(this)
        verify(inverse = true) { manager.requestReviewFlow() }
        job.cancel()
    }
    @Test
    fun test_isEligible() = runTest {
        setupFirestore(timeMillis = 0)
        setupReviewManager()
        val job = launch { appReviewManager.requestReviewFlow(mockk()) }
        testHelper.advance(this)
        verify(exactly = 1) { firestore.collection(userId).document(AppReviewFirestoreRef.SHOW_RATE_DIALOG.refName)
            .set(RateDialog(null, didShow = true)) }
        verify { manager.requestReviewFlow() }
        verify { manager.launchReviewFlow(any(), any()) }
        job.cancel()
    }
    @Test
    fun test_fetchException() = runTest {
        setupFirestore(fetchException = exception)
        setupReviewManager()
        assertFailsWith<TestException> { appReviewManager.requestReviewFlow(mockk()) }
        verify(inverse = true) { manager.requestReviewFlow() }
    }
    @Test
    fun test_timeFetchException() = runTest {
        setupTimeAPI(statusCode = HttpStatusCode.Forbidden)
        setupReviewManager()
        assertFailsWith<ClientRequestException> { appReviewManager.requestReviewFlow(mockk()) }
        verify(inverse = true) { manager.requestReviewFlow() }
    }
}