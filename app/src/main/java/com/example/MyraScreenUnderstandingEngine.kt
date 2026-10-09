package com.example

data class MyraScreenElement(
    val text: String = "",
    val contentDescription: String = "",
    val className: String = "",
    val clickable: Boolean = false,
    val enabled: Boolean = true,
    val x: Float = 0f,
    val y: Float = 0f
)

data class MyraUnderstandingResult(
    val packageName: String,
    val className: String,
    val elements: List<MyraScreenElement>,
    val ocrText: String = ""
)

class MyraScreenUnderstandingEngine {
    fun analyze(): MyraUnderstandingResult {
        val state = MyraScreenState
        val elements = MyraAccessibility.instance?.getVisibleElements().orEmpty().map {
            MyraScreenElement(
                text = it.text?.toString().orEmpty(),
                contentDescription = it.contentDescription?.toString().orEmpty(),
                className = it.className?.toString().orEmpty(),
                clickable = it.isClickable,
                enabled = it.isEnabled
            )
        }
        return MyraUnderstandingResult(
            state.currentPackage,
            state.currentClass,
            elements
        )
    }
}
