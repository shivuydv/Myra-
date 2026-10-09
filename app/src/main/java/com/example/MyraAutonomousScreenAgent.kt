package com.example

class MyraAutonomousScreenAgent(
    private val screenEngine: MyraScreenUnderstandingEngine,
    private val elementIntelligence: MyraUIElementIntelligence,
    private val targetResolver: MyraSmartElementLocator,
    private val actionExecutor: MyraUnifiedActionExecutor
) {
    suspend fun execute(command: String): MyraActionResult {
        if (command.isBlank()) return MyraActionResult(
            MyraActionStatus.FAILED, "Command खाली है."
        )
        val screen = screenEngine.analyze()
        val targetText = command.substringAfterLast(" ").trim()
        if (targetText.isBlank()) return MyraActionResult(
            MyraActionStatus.FAILED, "Target नहीं मिला."
        )
        val rawElements = MyraAccessibility.instance?.getVisibleElements().orEmpty()
        val elements = rawElements.map { node ->
            MyraUIElementInfo(
                text = node.text?.toString().orEmpty(),
                description = node.contentDescription?.toString().orEmpty(),
                className = node.className?.toString().orEmpty(),
                clickable = node.isClickable,
                enabled = node.isEnabled,
                editable = node.isEditable,
                selected = node.isSelected,
                focused = node.isFocused
            )
        }
        val target = targetResolver.find(elements, targetText)
            ?: return MyraActionResult(
                MyraActionStatus.FAILED, "Screen पर target नहीं मिला."
            )
        val action = MyraAction(
            type = "CLICK",
            target = targetText,
            expectedElement = targetText
        )
        return actionExecutor.execute(action)
    }
}
