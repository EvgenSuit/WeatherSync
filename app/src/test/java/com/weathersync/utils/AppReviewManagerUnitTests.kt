package com.weathersync.utils

import com.google.android.play.core.review.ReviewManager
import com.google.firebase.firestore.FirebaseFirestore
import com.weathersync.common.TestClock
import com.weathersync.common.TestException
import com.weathersync.common.auth.mockAuth
import com.weathersync.common.auth.userId
import com.weathersync.common.mockTask
import com.weathersync.common.utils.mockTimeAPI
import com.weathersync.utils.appReview.AppReviewFirestoreRef
import com.weathersync.utils.appReview.AppReviewManager
import com.weathersync.utils.appReview.data.RateDialog
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.Date
import kotlin.test.assertFailsWith

class AppReviewManagerUnitTests {
    private val testClock = TestClock()
    private val exception = TestException()
    private var auth = mockAuth()
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
        timeAPI = mockTimeAPI(
            statusCode = statusCode,
            currTimeMillis = testClock.millis()
        )
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

    private fun setupReviewManager() {
        appReviewManager = AppReviewManager(
            timeAPI = timeAPI,
            auth = auth,
            firestore = firestore,
            manager = manager
        )
    }
    private fun setup() {
        testClock.setInstant(Instant.ofEpochSecond(15 * 24 * 60 * 60))
        setupFakeReviewManager()
        setupTimeAPI(statusCode = HttpStatusCode.OK)
        setupFirestore(timeMillis = testClock.millis())
        setupReviewManager()
    }

    @Before
    fun before() {
        setup()
    }

    @Test
    fun entryNull_isNotEligible() = runTest {
        setupFirestore(timeMillis = null)
        setupReviewManager()
        appReviewManager.requestReviewFlow(mockk())
        verify(exactly = 1) { firestore.collection(userId).document(AppReviewFirestoreRef.SHOW_RATE_DIALOG.refName)
            .set(any<RateDialog>()) }
        verify(inverse = true) { manager.requestReviewFlow() }
    }
    @Test
    fun entryNotNull_isNotEligible() = runTest {
        // test a scenario where first entry date and the current date are equal
        appReviewManager.requestReviewFlow(mockk())
        verify(inverse = true) { manager.requestReviewFlow() }
    }
    @Test
    fun didShow_isNotEligible() = runTest {
        setupFirestore(didShow = true)
        setupReviewManager()
        appReviewManager.requestReviewFlow(mockk())
        verify(inverse = true) { manager.requestReviewFlow() }
    }
    @Test
    fun test_isEligible() = runTest {
        setupFirestore(timeMillis = 0)
        setupReviewManager()
        appReviewManager.requestReviewFlow(mockk())
        verify(exactly = 1) { firestore.collection(userId).document(AppReviewFirestoreRef.SHOW_RATE_DIALOG.refName)
            .set(RateDialog(null, didShow = true)) }
        verify { manager.requestReviewFlow() }
        verify { manager.launchReviewFlow(any(), any()) }
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