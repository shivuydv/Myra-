package com.example

data class MyraGestureContext(
    val screenWidth: Float, val screenHeight: Float,
    val startX: Float, val startY: Float, val distance: Float, val duration: Long
)

class MyraGestureContextEngine {
    fun createContext(
        direction: MyraGestureDirection, screenWidth: Float,
        screenHeight: Float, command: String
    ): MyraGestureContext {
        val t = command.lowercase()
        val ratio = when {
            "थोड़ा" in t || "short" in t -> 0.25f
            "लंबा" in t || "long" in t -> 0.65f
            else -> 0.45f
        }
        val duration = when {
            "जल्दी" in t || "fast" in t -> 250L
            "धीरे" in t || "slow" in t -> 900L
            else -> 500L
        }
        val sx = screenWidth/2f; val sy = screenHeight/2f
        val d = if (direction == MyraGestureDirection.LEFT || direction == MyraGestureDirection.RIGHT)
            screenWidth*ratio else screenHeight*ratio
        return MyraGestureContext(screenWidth, screenHeight, sx, sy, d, duration)
    }
    fun endPoint(c: MyraGestureContext, d: MyraGestureDirection): Pair<Float,Float> =
        when (d) {
            MyraGestureDirection.UP -> c.startX to c.startY-c.distance
            MyraGestureDirection.DOWN -> c.startX to c.startY+c.distance
            MyraGestureDirection.LEFT -> c.startX-c.distance to c.startY
            MyraGestureDirection.RIGHT -> c.startX+c.distance to c.startY
        }
}
