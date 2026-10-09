package com.example

class MyraAdvancedPlanner(
    private val mapper: MyraIntentMapper = MyraIntentMapper()
) {
    fun createPlan(
        command: String,
        context: MyraContextSnapshot = MyraContextSnapshot()
    ): MyraActionPlan {
        val action = when (mapper.map(command)) {
            "GESTURE" -> MyraAction(type = "GESTURE", target = command, value = command)
            "OPEN_APP" -> MyraAction(type = "OPEN_APP", target = command)
            "CALL" -> MyraAction(
                type = "CALL", target = command, requiresConfirmation = true
            )
            "WEB_SEARCH" -> MyraAction(type = "WEB_SEARCH", value = command)
            else -> null
        }
        return MyraActionPlan(command, listOfNotNull(action))
    }
}
