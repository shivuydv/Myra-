package com.example

enum class MyraTaskLifecycle {
    CREATED, PLANNING, READY, RUNNING, WAITING_CONFIRMATION,
    WAITING_INPUT, RETRY_REQUIRED, PAUSED, COMPLETED, FAILED, CANCELLED
}

data class MyraUnifiedTaskContext(
    val taskId: String,
    val command: String,
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis(),
    var lifecycle: MyraTaskLifecycle = MyraTaskLifecycle.CREATED,
    var currentStep: Int = 0,
    var totalSteps: Int = 0,
    var currentPackage: String = "",
    var lastMessage: String = "",
    var plan: MyraActionPlan? = null
) {
    fun updateLifecycle(next: MyraTaskLifecycle, message: String = "") {
        lifecycle = next
        lastMessage = message
        updatedAt = System.currentTimeMillis()
    }
    fun updateStep(step: Int, total: Int) {
        currentStep = step.coerceAtLeast(0)
        totalSteps = total.coerceAtLeast(0)
        updatedAt = System.currentTimeMillis()
    }
}
