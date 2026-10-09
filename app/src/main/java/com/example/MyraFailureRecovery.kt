package com.example

import kotlinx.coroutines.delay

class MyraFailureRecovery(
    private val capabilityManager: MyraCapabilityManager,
    private val maxRetries: Int = 2
) {
    suspend fun execute(action: MyraAction): MyraActionResult {
        var result = MyraActionResult(
            MyraActionStatus.FAILED, "Action शुरू नहीं हुआ."
        )
        repeat(maxRetries + 1) { attempt ->
            result = capabilityManager.execute(action)
            if (result.status == MyraActionStatus.SUCCESS ||
                result.status == MyraActionStatus.NEED_CONFIRMATION ||
                result.status == MyraActionStatus.WAITING) return result
            if (attempt < maxRetries) delay(400L)
        }
        return result.copy(retryCount = maxRetries)
    }
}
