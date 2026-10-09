package com.example

class MyraSmartElementLocator {
    fun find(elements: List<MyraUIElementInfo>, query: String): MyraUIElementInfo? {
        val q = query.trim()
        if (q.isBlank()) return null
        return elements.firstOrNull {
            it.text.equals(q, true) || it.description.equals(q, true)
        } ?: elements.firstOrNull {
            it.text.contains(q, true) || it.description.contains(q, true)
        }
    }
}
