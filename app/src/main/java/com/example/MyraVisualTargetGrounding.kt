package com.example

data class MyraGroundingResult(
    val target: MyraUnifiedElement?,
    val confidence: Float,
    val source: String,
    val message: String
) {
    val found: Boolean get() = target != null && confidence >= 0.65f
}

class MyraVisualTargetGrounding {
    fun findTarget(
        query: String,
        elements: List<MyraUnifiedElement>
    ): MyraGroundingResult {
        val q = query.trim().lowercase()
        if (q.isBlank()) return MyraGroundingResult(
            null, 0f, "none", "Target query खाली है."
        )
        val scored = elements.filter { it.enabled }.map { element ->
            val text = element.text.lowercase()
            val description = element.description.lowercase()
            val score = when {
                text == q || description == q -> 0.95f
                text.contains(q) || description.contains(q) -> 0.78f
                else -> 0f
            }
            element to score
        }.filter { it.second > 0f }.sortedByDescending { it.second }
        if (scored.isEmpty()) return MyraGroundingResult(
            null, 0f, "none", "Visible elements में target नहीं मिला."
        )
        if (scored.size > 1 && scored[0].second == scored[1].second) {
            return MyraGroundingResult(
                null, scored[0].second, "ambiguous", "एक से ज्यादा matching targets मिले."
            )
        }
        val (element, score) = scored.first()
        val source = if (element.accessibilityScore >= element.ocrScore) "accessibility" else "ocr"
        return MyraGroundingResult(element, score, source, "Target grounded.")
    }
}
