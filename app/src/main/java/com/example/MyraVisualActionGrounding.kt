package com.example

enum class MyraGroundedAction { TAP, LONG_PRESS }
data class MyraGroundedTarget(
    val target: MyraUnifiedElement, val action: MyraGroundedAction,
    val x: Float, val y: Float, val confidence: Float
)

class MyraVisualActionGrounding {
    fun ground(e: MyraUnifiedElement, action: MyraGroundedAction): MyraGroundedTarget? {
        if (!e.enabled || e.finalScore < 0.65f) return null
        return MyraGroundedTarget(e, action, e.x, e.y, e.finalScore)
    }
}
