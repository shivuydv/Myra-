package com.example

class MyraCoordinateActionEngine {
    private val service get() = MyraAccessibilityService.instance

    suspend fun tap(x: Float, y: Float): MyraActionResult {
        val s = service ?: return MyraActionResult(MyraActionStatus.FAILED, "Accessibility चालू नहीं है.")
        return if (s.tap(x, y)) MyraActionResult(MyraActionStatus.SUCCESS, "Tap complete.")
        else MyraActionResult(MyraActionStatus.FAILED, "Tap fail हुआ.")
    }

    suspend fun longPress(x: Float, y: Float, duration: Long = 700L): MyraActionResult {
        val s = service ?: return MyraActionResult(MyraActionStatus.FAILED, "Accessibility चालू नहीं है.")
        return if (s.longPress(x, y, duration)) MyraActionResult(MyraActionStatus.SUCCESS, "Long press complete.")
        else MyraActionResult(MyraActionStatus.FAILED, "Long press fail हुआ.")
    }

    suspend fun swipe(
        startX: Float, startY: Float, endX: Float, endY: Float,
        duration: Long = 500L
    ): MyraActionResult {
        val s = service ?: return MyraActionResult(MyraActionStatus.FAILED, "Accessibility चालू नहीं है.")
        return if (s.swipe(startX, startY, endX, endY, duration))
            MyraActionResult(MyraActionStatus.SUCCESS, "Swipe भेज दिया गया.")
        else MyraActionResult(MyraActionStatus.FAILED, "Swipe fail हुआ.")
    }
}
