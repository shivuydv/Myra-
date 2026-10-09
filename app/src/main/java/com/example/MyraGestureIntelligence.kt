package com.example

enum class MyraGestureDirection { UP, DOWN, LEFT, RIGHT }
data class MyraGesturePlan(
    val direction: MyraGestureDirection, val startX: Float, val startY: Float,
    val endX: Float, val endY: Float, val duration: Long
)

class MyraGestureIntelligence {
    fun createSwipe(
        direction: MyraGestureDirection, screenWidth: Float, screenHeight: Float,
        distanceRatio: Float = 0.45f, duration: Long = 500L
    ): MyraGesturePlan {
        val sx = screenWidth/2f; val sy = screenHeight/2f
        val d = if (direction == MyraGestureDirection.LEFT || direction == MyraGestureDirection.RIGHT)
            screenWidth*distanceRatio else screenHeight*distanceRatio
        val end = when (direction) {
            MyraGestureDirection.UP -> sx to sy-d
            MyraGestureDirection.DOWN -> sx to sy+d
            MyraGestureDirection.LEFT -> sx-d to sy
            MyraGestureDirection.RIGHT -> sx+d to sy
        }
        return MyraGesturePlan(direction, sx, sy, end.first, end.second, duration)
    }

    fun parseDirection(command: String): MyraGestureDirection? {
        val t = command.lowercase()
        return when {
            listOf("ऊपर", "up").any { it in t } -> MyraGestureDirection.UP
            listOf("नीचे", "down").any { it in t } -> MyraGestureDirection.DOWN
            listOf("बाएं", "बायीं", "left").any { it in t } -> MyraGestureDirection.LEFT
            listOf("दाएं", "दायीं", "right").any { it in t } -> MyraGestureDirection.RIGHT
            else -> null
        }
    }
}
