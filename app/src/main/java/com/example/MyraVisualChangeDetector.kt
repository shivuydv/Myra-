package com.example

data class MyraVisualChangeResult(val changed: Boolean, val score: Float, val message: String)

class MyraVisualChangeDetector {
    fun compare(beforeText: String, afterText: String): MyraVisualChangeResult {
        val a = beforeText.lowercase().split(Regex("\\s+")).filter(String::isNotBlank).toSet()
        val b = afterText.lowercase().split(Regex("\\s+")).filter(String::isNotBlank).toSet()
        if (a.isEmpty() && b.isEmpty()) return MyraVisualChangeResult(false,0f,"OCR text नहीं है.")
        val union = (a union b).size.coerceAtLeast(1)
        val score = 1f - (a intersect b).size.toFloat()/union
        val changed = score >= 0.15f
        return MyraVisualChangeResult(changed, score, if (changed) "OCR बदला." else "OCR में पर्याप्त बदलाव नहीं.")
    }
}
