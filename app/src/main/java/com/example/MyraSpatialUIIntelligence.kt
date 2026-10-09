package com.example

import kotlin.math.abs
import kotlin.math.sqrt

enum class MyraSpatialRelation {
    ABOVE, BELOW, LEFT, RIGHT, NEAR, FAR, SAME_ROW, SAME_COLUMN, UNKNOWN
}

class MyraSpatialUIIntelligence {
    fun relation(a: MyraUIElementInfo, b: MyraUIElementInfo): MyraSpatialRelation {
        val dx = b.x - a.x
        val dy = b.y - a.y
        if (abs(dx) < 40f && abs(dy) < 40f) return MyraSpatialRelation.NEAR
        if (abs(dy) < 30f) return MyraSpatialRelation.SAME_ROW
        if (abs(dx) < 30f) return MyraSpatialRelation.SAME_COLUMN
        return when {
            abs(dx) > abs(dy) && dx < 0 -> MyraSpatialRelation.LEFT
            abs(dx) > abs(dy) && dx > 0 -> MyraSpatialRelation.RIGHT
            dy < 0 -> MyraSpatialRelation.ABOVE
            dy > 0 -> MyraSpatialRelation.BELOW
            else -> MyraSpatialRelation.UNKNOWN
        }
    }
    fun nearest(origin: MyraUIElementInfo, elements: List<MyraUIElementInfo>) =
        elements.filter { it != origin }.minByOrNull {
            sqrt((it.x-origin.x)*(it.x-origin.x) + (it.y-origin.y)*(it.y-origin.y))
        }
    fun sortByPosition(elements: List<MyraUIElementInfo>) =
        elements.sortedWith(compareBy<MyraUIElementInfo> { it.y }.thenBy { it.x })
}
