package com.example

enum class MyraGesturePattern {
    VERTICAL_SCROLL, HORIZONTAL_NAVIGATION, PAGE_CHANGE,
    SHORT_ADJUSTMENT, LONG_SCROLL, UNKNOWN
}
data class MyraGesturePatternResult(val pattern: MyraGesturePattern, val confidence: Float)

class MyraGesturePatternRecognition {
    fun recognize(
        direction: MyraGestureDirection, distanceRatio: Float,
        duration: Long, section: MyraScreenSection
    ): MyraGesturePatternResult = when {
        direction == MyraGestureDirection.LEFT || direction == MyraGestureDirection.RIGHT ->
            MyraGesturePatternResult(MyraGesturePattern.HORIZONTAL_NAVIGATION,.9f)
        distanceRatio >= .60f -> MyraGesturePatternResult(MyraGesturePattern.LONG_SCROLL,.85f)
        distanceRatio <= .25f -> MyraGesturePatternResult(MyraGesturePattern.SHORT_ADJUSTMENT,.8f)
        section == MyraScreenSection.CONTENT || section == MyraScreenSection.SEARCH ->
            MyraGesturePatternResult(MyraGesturePattern.VERTICAL_SCROLL,.75f)
        duration <= 350L -> MyraGesturePatternResult(MyraGesturePattern.PAGE_CHANGE,.65f)
        else -> MyraGesturePatternResult(MyraGesturePattern.UNKNOWN,.4f)
    }
}
