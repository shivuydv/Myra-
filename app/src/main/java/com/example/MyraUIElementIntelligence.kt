package com.example

data class MyraUIElementInfo(
    val text: String = "", val description: String = "",
    val role: String = "", val className: String = "",
    val clickable: Boolean = false, val editable: Boolean = false,
    val enabled: Boolean = true, val selected: Boolean = false,
    val focused: Boolean = false, val x: Float = 0f, val y: Float = 0f,
    val confidence: Float = 0f
)

class MyraUIElementIntelligence {
    fun score(e: MyraUIElementInfo, query: String): Float {
        val q = query.trim().lowercase()
        if (q.isBlank()) return 0f
        return when {
            e.text.equals(q, true) || e.description.equals(q, true) -> 1f
            e.text.contains(q, true) || e.description.contains(q, true) -> 0.8f
            else -> e.confidence.coerceIn(0f, 1f) * 0.5f
        }
    }
    fun bestMatch(elements: List<MyraUIElementInfo>, query: String) =
        elements.maxByOrNull { score(it, query) }?.takeIf { score(it, query) >= 0.5f }
}
