package com.example

class MyraVisualTargetConfidenceEngine {
    fun reliable(e: MyraUnifiedElement) = e.enabled && e.finalScore >= 0.65f
    fun score(e: MyraUnifiedElement, query: String): Float {
        val exact = e.text.equals(query, true) || e.description.equals(query, true)
        val partial = e.text.contains(query, true) || e.description.contains(query, true)
        return (e.finalScore + (if (exact) 0.35f else if (partial) 0.15f else 0f) +
            (if (e.clickable) 0.05f else 0f)).coerceIn(0f, 1f)
    }
}
