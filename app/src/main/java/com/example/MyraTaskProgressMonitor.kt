package com.example

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MyraTaskProgress(
    val taskId: String, val command: String, val currentStep: Int, val totalSteps: Int,
    val status: MyraTaskLifecycle, val message: String, val startedAt: Long, val updatedAt: Long
) {
    val progressPercent: Int get() = if (totalSteps <= 0) {
        if (status == MyraTaskLifecycle.COMPLETED) 100 else 0
    } else currentStep.coerceIn(0, totalSteps) * 100 / totalSteps
    val elapsedMs: Long get() = (updatedAt - startedAt).coerceAtLeast(0L)
}

class MyraTaskProgressMonitor {
    private val _tasks = MutableStateFlow<Map<String, MyraTaskProgress>>(emptyMap())
    val tasks: StateFlow<Map<String, MyraTaskProgress>> = _tasks.asStateFlow()

    @Synchronized fun start(taskId: String, command: String, totalSteps: Int): MyraTaskProgress {
        val now = System.currentTimeMillis()
        val p = MyraTaskProgress(taskId, command, 0, totalSteps.coerceAtLeast(0), MyraTaskLifecycle.RUNNING, "Task शुरू हो रहा है.", now, now)
        _tasks.value = _tasks.value + (taskId to p)
        return p
    }
    @Synchronized fun update(taskId: String, currentStep: Int, status: MyraTaskLifecycle, message: String): MyraTaskProgress? {
        val old = _tasks.value[taskId] ?: return null
        val p = old.copy(currentStep = currentStep.coerceIn(0, old.totalSteps), status = status, message = message, updatedAt = System.currentTimeMillis())
        _tasks.value = _tasks.value + (taskId to p)
        return p
    }
    fun get(taskId: String): MyraTaskProgress? = _tasks.value[taskId]
    fun remove(taskId: String) { _tasks.value = _tasks.value - taskId }
}
