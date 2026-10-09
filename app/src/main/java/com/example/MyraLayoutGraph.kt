package com.example

import kotlin.math.abs
import kotlin.math.sqrt

data class MyraLayoutNode(val id: Int, val element: MyraUnifiedElement)
data class MyraLayoutEdge(val from: Int, val to: Int, val relation: MyraSpatialRelation, val distance: Float)
data class MyraLayoutGraph(val nodes: List<MyraLayoutNode>, val edges: List<MyraLayoutEdge>)

class MyraLayoutGraphBuilder {
    fun build(elements: List<MyraUnifiedElement>): MyraLayoutGraph {
        val nodes = elements.mapIndexed { i, e -> MyraLayoutNode(i, e) }
        val edges = mutableListOf<MyraLayoutEdge>()
        for (i in nodes.indices) for (j in i + 1 until nodes.size) {
            val a = nodes[i].element; val b = nodes[j].element
            val dx = b.x-a.x; val dy = b.y-a.y
            val rel = when {
                abs(dx) < 30 && abs(dy) < 30 -> MyraSpatialRelation.NEAR
                abs(dx) > abs(dy) && dx < 0 -> MyraSpatialRelation.LEFT
                abs(dx) > abs(dy) && dx > 0 -> MyraSpatialRelation.RIGHT
                dy < 0 -> MyraSpatialRelation.ABOVE
                dy > 0 -> MyraSpatialRelation.BELOW
                else -> MyraSpatialRelation.UNKNOWN
            }
            edges += MyraLayoutEdge(i, j, rel, sqrt(dx*dx + dy*dy))
        }
        return MyraLayoutGraph(nodes, edges)
    }
}
