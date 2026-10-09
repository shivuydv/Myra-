package com.example

import kotlinx.coroutines.delay

fun interface MyraScreenSnapshotProvider { suspend fun capture(): MyraPostActionVerificationSnapshot? }

class MyraAdaptiveVerificationRetryEngine(
    private val verificationEngine: MyraPostActionVerificationEngine,
    private val snapshotProvider: MyraScreenSnapshotProvider
) {
    suspend fun verifyWithRetry(
        action: MyraAction, before: MyraPostActionVerificationSnapshot,
        maxAttempts: Int = 4, initialWaitMs: Long = 400L, maxWaitMs: Long = 2000L
    ): MyraPostActionVerificationResult {
        val attempts = maxAttempts.coerceIn(1, 5)
        val firstWait = initialWaitMs.coerceIn(100L, 2000L)
        val waitLimit = maxWaitMs.coerceIn(firstWait, 3000L)
        var waitMs = firstWait
        var last = MyraPostActionVerificationResult(MyraPostActionVerificationStatus.UNCERTAIN, 0f, "Verification नहीं हुई.")
        repeat(attempts) {
            delay(waitMs)
            val after = try { snapshotProvider.capture() } catch (_: Exception) { null }
            last = if (after != null) verificationEngine.verify(action, before, after)
            else MyraPostActionVerificationResult(MyraPostActionVerificationStatus.UNCERTAIN, 0f, "Snapshot उपलब्ध नहीं है.")
            if (last.status == MyraPostActionVerificationStatus.VERIFIED) return last
            waitMs = (waitMs * 2).coerceAtMost(waitLimit)
        }
        return last.copy(status = MyraPostActionVerificationStatus.UNCERTAIN, message = "Verification के बाद भी नतीजा confirm नहीं हुआ: ${last.message}")
    }
}
