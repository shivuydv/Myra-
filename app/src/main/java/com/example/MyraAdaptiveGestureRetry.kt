package com.example

data class MyraGestureRetryConfig(
    val distanceRatio: Float, val duration: Long,
    val centerOffsetX: Float = 0f, val centerOffsetY: Float = 0f
)

class MyraAdaptiveGestureRetry(private val maxAttempts: Int = 3) {
    fun configs() = listOf(
        MyraGestureRetryConfig(.45f,500L),
        MyraGestureRetryConfig(.60f,400L),
        MyraGestureRetryConfig(.30f,700L,centerOffsetY=120f)
    ).take(maxAttempts.coerceIn(1,3))
}
