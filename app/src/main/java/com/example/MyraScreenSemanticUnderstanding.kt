package com.example

enum class MyraScreenSection { HEADER, SEARCH, CONTENT, MENU, DIALOG, BOTTOM_NAVIGATION, UNKNOWN }
data class MyraSemanticSection(val section: MyraScreenSection, val confidence: Float)

class MyraScreenSemanticUnderstanding {
    fun classify(text: String, yRatio: Float): MyraSemanticSection {
        val t = text.lowercase()
        val section = when {
            "search" in t || "खोज" in t -> MyraScreenSection.SEARCH
            listOf("cancel", "confirm", "okay", "dialog").any { it in t } -> MyraScreenSection.DIALOG
            yRatio < 0.12f -> MyraScreenSection.HEADER
            yRatio > 0.86f -> MyraScreenSection.BOTTOM_NAVIGATION
            else -> MyraScreenSection.CONTENT
        }
        return MyraSemanticSection(section, 0.65f)
    }
}
