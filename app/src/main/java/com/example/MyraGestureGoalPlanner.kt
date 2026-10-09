package com.example

data class MyraGestureGoal(
    val intent: MyraGestureIntent, val direction: MyraGestureDirection,
    val distanceRatio: Float, val duration: Long,
    val requiresVerification: Boolean = true
)

class MyraGestureGoalPlanner {
    fun createGoal(
        intent: MyraGestureIntent, direction: MyraGestureDirection,
        strategy: MyraGestureRetryConfig
    ): MyraGestureGoal {
        val ratio = when (intent) {
            MyraGestureIntent.LONG_SCROLL -> maxOf(strategy.distanceRatio,.60f)
            MyraGestureIntent.ADJUST_POSITION -> minOf(strategy.distanceRatio,.25f)
            MyraGestureIntent.CHANGE_PAGE -> maxOf(strategy.distanceRatio,.50f)
            MyraGestureIntent.NAVIGATE_HORIZONTAL -> maxOf(strategy.distanceRatio,.40f)
            else -> strategy.distanceRatio
        }
        val duration = when (intent) {
            MyraGestureIntent.ADJUST_POSITION -> maxOf(strategy.duration,500L)
            MyraGestureIntent.CHANGE_PAGE -> minOf(strategy.duration,500L)
            else -> strategy.duration
        }
        return MyraGestureGoal(intent,direction,ratio,duration,true)
    }
}
