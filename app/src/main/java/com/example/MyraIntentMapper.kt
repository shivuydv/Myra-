package com.example

class MyraIntentMapper {
    fun map(command: String): String {
        val t = command.lowercase()
        return when {
            listOf("scroll", "swipe", "ऊपर", "नीचे", "दाएं", "बाएं").any(t::contains) ->
                "GESTURE"
            listOf("open", "खोल", "खोलो", "चलाओ").any(t::contains) -> "OPEN_APP"
            listOf("call", "फोन", "कॉल").any(t::contains) -> "CALL"
            listOf("search", "खोज", "ढूंढ").any(t::contains) -> "WEB_SEARCH"
            else -> "UNKNOWN"
        }
    }
}
