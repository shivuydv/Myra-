package com.example

enum class MyraGestureIntent {
    SCROLL_CONTENT, CHANGE_PAGE, NAVIGATE_HORIZONTAL,
    ADJUST_POSITION, LONG_SCROLL, UNKNOWN
}
data class MyraGestureIntentResult(val intent: MyraGestureIntent, val confidence: Float)

class MyraGestureIntentResolver {
    fun resolve(command: String, pattern: MyraGesturePattern, section: MyraScreenSection): MyraGestureIntentResult {
        val t = command.lowercase()
        return when {
            listOf("next page","अगला पेज","next").any { it in t } ->
                MyraGestureIntentResult(MyraGestureIntent.CHANGE_PAGE,.9f)
            pattern == MyraGesturePattern.HORIZONTAL_NAVIGATION ->
                MyraGestureIntentResult(MyraGestureIntent.NAVIGATE_HORIZONTAL,.85f)
            pattern == MyraGesturePattern.LONG_SCROLL ->
                MyraGestureIntentResult(MyraGestureIntent.LONG_SCROLL,.85f)
            pattern == MyraGesturePattern.SHORT_ADJUSTMENT ->
                MyraGestureIntentResult(MyraGestureIntent.ADJUST_POSITION,.8f)
            pattern == MyraGesturePattern.VERTICAL_SCROLL &&
                (section == MyraScreenSection.CONTENT || section == MyraScreenSection.SEARCH) ->
                MyraGestureIntentResult(MyraGestureIntent.SCROLL_CONTENT,.8f)
            else -> MyraGestureIntentResult(MyraGestureIntent.UNKNOWN,.4f)
        }
    }
}
