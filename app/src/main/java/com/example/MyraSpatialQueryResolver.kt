package com.example

import kotlin.math.abs

data class MyraSpatialQuery(val target: String, val relation: MyraSpatialRelation)

class MyraSpatialQueryResolver {
    fun parse(command: String): MyraSpatialQuery {
        val t = command.lowercase()
        val relation = when {
            "ऊपर" in t || "above" in t -> MyraSpatialRelation.ABOVE
            "नीचे" in t || "below" in t -> MyraSpatialRelation.BELOW
            "बाएं" in t || "left" in t -> MyraSpatialRelation.LEFT
            "दाएं" in t || "right" in t -> MyraSpatialRelation.RIGHT
            "पास" in t || "near" in t -> MyraSpatialRelation.NEAR
            else -> MyraSpatialRelation.UNKNOWN
        }
        val target = command.replace(
            Regex("(?i)ऊपर|नीचे|बाएं|दाएं|above|below|left|right|near|पास"), ""
        ).trim()
        return MyraSpatialQuery(target, relation)
    }
}
