package com.example

import kotlinx.coroutines.delay

class MyraSmartWait {
    suspend fun waitFor(
        timeoutMs: Long = 4000L,
        intervalMs: Long = 200L,
        condition: suspend () -> Boolean
    ): Boolean {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (condition()) return true
            delay(intervalMs.coerceAtLeast(50L))
        }
        return condition()
    }
}
