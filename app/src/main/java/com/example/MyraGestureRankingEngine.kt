package com.example

data class MyraRankedGesture(
    val distanceRatio: Float, val duration: Long,
    val successRate: Float, val score: Float
)

class MyraGestureRankingEngine {
    fun rank(
        experiences: List<MyraGestureExperience>,
        packageName: String, direction: MyraGestureDirection
    ): MyraRankedGesture? {
        val list = experiences.filter { it.packageName == packageName && it.direction == direction }
        if (list.isEmpty()) return null
        return list.groupBy { it.distanceRatio to it.duration }.map { (key, group) ->
            val rate = group.count { it.success }.toFloat()/group.size.coerceAtLeast(1)
            MyraRankedGesture(key.first,key.second,rate,rate)
        }.maxByOrNull { it.score }
    }
}
