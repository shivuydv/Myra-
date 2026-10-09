package com.example

import android.content.Context

data class MyraGestureExperience(
    val packageName: String, val direction: MyraGestureDirection,
    val distanceRatio: Float, val duration: Long, val success: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class MyraGestureLearning(private val context: Context) {
    private val store = MyraGestureLearningStore(context)
    private val experiences = store.load().toMutableList()

    fun record(item: MyraGestureExperience) {
        experiences += item
        while (experiences.size > 100) experiences.removeAt(0)
        store.save(experiences)
    }

    fun bestStrategy(packageName: String, direction: MyraGestureDirection): MyraGestureRetryConfig? {
        val best = MyraGestureRankingEngine().rank(experiences,packageName,direction)
            ?.takeIf { it.successRate >= .50f } ?: return null
        return MyraGestureRetryConfig(best.distanceRatio,best.duration)
    }

    fun all() = experiences.toList()
}
