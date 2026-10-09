package com.example

enum class MyraGestureStrategy { NORMAL, FAST, SLOW, LONG_DISTANCE, SHORT_DISTANCE }

class MyraGestureStrategySelector {
    fun select(
        command: String, direction: MyraGestureDirection,
        screenHeight: Float, screenWidth: Float
    ): MyraGestureRetryConfig {
        val t = command.lowercase()
        val ratio = when {
            "थोड़ा" in t || "short" in t -> .25f
            "लंबा" in t || "long" in t -> .65f
            direction == MyraGestureDirection.LEFT || direction == MyraGestureDirection.RIGHT -> .40f
            else -> .45f
        }
        val duration = when {
            "जल्दी" in t || "fast" in t -> 250L
            "धीरे" in t || "slow" in t -> 900L
            else -> 500L
        }
        return MyraGestureRetryConfig(ratio,duration)
    }
}
