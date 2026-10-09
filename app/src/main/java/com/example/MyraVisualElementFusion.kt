package com.example

data class MyraOCRMatch(val text: String, val x: Float, val y: Float, val confidence: Float = 1f)
data class MyraUnifiedElement(
    val text: String = "", val description: String = "", val role: String = "UNKNOWN",
    val x: Float = 0f, val y: Float = 0f, val clickable: Boolean = false,
    val enabled: Boolean = true, val accessibilityScore: Float = 0f,
    val ocrScore: Float = 0f, val finalScore: Float = 0f
)

class MyraVisualElementFusion {
    fun fuse(accessibility: List<MyraUIElementInfo>, ocr: List<MyraOCRMatch>): List<MyraUnifiedElement> {
        val result = accessibility.map { e ->
            val match = ocr.maxByOrNull {
                when {
                    it.text.equals(e.text, true) -> 1f
                    it.text.contains(e.text, true) || e.text.contains(it.text, true) -> 0.6f
                    else -> 0f
                }
            }
            val os = match?.confidence ?: 0f
            MyraUnifiedElement(
                text = e.text.ifBlank { match?.text.orEmpty() },
                description = e.description, role = e.role,
                x = if (e.x != 0f) e.x else match?.x ?: 0f,
                y = if (e.y != 0f) e.y else match?.y ?: 0f,
                clickable = e.clickable, enabled = e.enabled,
                accessibilityScore = e.confidence, ocrScore = os,
                finalScore = (e.confidence * 0.6f + os * 0.4f).coerceIn(0f, 1f)
            )
        }.toMutableList()
        ocr.filter { o -> result.none { it.text.equals(o.text, true) } }.forEach {
            result += MyraUnifiedElement(
                text = it.text, x = it.x, y = it.y,
                ocrScore = it.confidence, finalScore = it.confidence * 0.4f
            )
        }
        return result
    }
}
