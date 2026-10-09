package com.example

enum class MyraPostActionVerificationStatus { VERIFIED, NOT_VERIFIED, UNCERTAIN }
data class MyraPostActionVerificationSnapshot(
    val packageName: String, val visibleTexts: Set<String>,
    val capturedAt: Long = System.currentTimeMillis()
)
data class MyraPostActionVerificationResult(
    val status: MyraPostActionVerificationStatus, val confidence: Float, val message: String
)

class MyraPostActionVerificationEngine {
    fun verify(action: MyraAction, before: MyraPostActionVerificationSnapshot, after: MyraPostActionVerificationSnapshot): MyraPostActionVerificationResult {
        if (after.capturedAt < before.capturedAt || after.packageName.isBlank()) return uncertain("नई screen state valid नहीं है.")
        if (action.expectedPackage.isNotBlank() && !after.packageName.equals(action.expectedPackage, true)) {
            return MyraPostActionVerificationResult(MyraPostActionVerificationStatus.NOT_VERIFIED, 0.90f, "Expected app foreground में नहीं दिख रहा.")
        }
        val oldTexts = before.visibleTexts.map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
        val newTexts = after.visibleTexts.map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
        val expected = action.expectedElement.trim().lowercase()
        if (expected.isNotBlank() && newTexts.any { it.contains(expected) }) {
            return MyraPostActionVerificationResult(MyraPostActionVerificationStatus.VERIFIED, 0.90f, "Expected element नई screen पर मिला.")
        }
        if (expected.isNotBlank() && expected in oldTexts) return uncertain("Expected element गायब है, लेकिन failure पक्का नहीं है.", 0.55f)
        return if (oldTexts != newTexts) uncertain("Screen बदली है; सफलता अलग से confirm करनी होगी.", 0.50f)
        else uncertain("Screen में स्पष्ट बदलाव नहीं मिला.", 0.30f)
    }
    private fun uncertain(message: String, confidence: Float = 0f) = MyraPostActionVerificationResult(MyraPostActionVerificationStatus.UNCERTAIN, confidence, message)
}
