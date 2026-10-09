package com.example

data class MyraGestureContextKey(
    val packageName: String, val direction: MyraGestureDirection,
    val section: MyraScreenSection
)

data class MyraContextGestureExperience(
    val packageName: String, val direction: MyraGestureDirection,
    val section: MyraScreenSection, val distanceRatio: Float,
    val duration: Long, val success: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class MyraContextAwareGestureLearning {
    private val records = mutableListOf<MyraContextGestureExperience>()

    fun record(item: MyraContextGestureExperience) {
        records += item
        while (records.size > 100) records.removeAt(0)
    }

    fun bestStrategyForContext(
        packageName: String, direction: MyraGestureDirection, section: MyraScreenSection
    ): MyraGestureRetryConfig? {
        val exact = records.filter {
            it.packageName == packageName && it.direction == direction && it.section == section
        }
        val candidates = if (exact.isNotEmpty()) exact else records.filter {
            it.packageName == packageName && it.direction == direction
        }
        val best = candidates.groupBy { it.distanceRatio to it.duration }
            .map { (key, group) ->
                val rate = group.count { it.success }.toFloat()/group.size.coerceAtLeast(1)
                MyraRankedGesture(key.first,key.second,rate,rate)
            }.maxByOrNull { it.score }?.takeIf { it.successRate >= .5f } ?: return null
        return MyraGestureRetryConfig(best.distanceRatio,best.duration)
    }
}
