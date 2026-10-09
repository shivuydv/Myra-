package com.example

import kotlinx.coroutines.delay

class MyraAdaptiveVisualRetry {
    suspend fun click(
        target: String,
        attempts: Int = 4,
        action: suspend (String, Int) -> MyraActionResult
    ): MyraActionResult {
        var last = MyraActionResult(MyraActionStatus.FAILED, "Target नहीं मिला.")
        repeat(attempts.coerceIn(1, 4)) { i ->
            last = action(target, i)
            if (last.status == MyraActionStatus.SUCCESS ||
                last.status == MyraActionStatus.NEED_CONFIRMATION ||
                last.status == MyraActionStatus.WAITING) return last
            delay(250L)
        }
        return last
    }
}
