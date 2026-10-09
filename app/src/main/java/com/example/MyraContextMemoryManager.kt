package com.example

data class MyraContextSnapshot(
    val lastCommand: String = "",
    val lastResult: MyraActionResult? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

class MyraContextMemoryManager {
    private var state = MyraContextSnapshot()
    fun updateCommand(command: String) {
        state = state.copy(lastCommand = command, updatedAt = System.currentTimeMillis())
    }
    fun updateResult(result: MyraActionResult) {
        state = state.copy(lastResult = result, updatedAt = System.currentTimeMillis())
    }
    fun get(): MyraContextSnapshot = state
}
