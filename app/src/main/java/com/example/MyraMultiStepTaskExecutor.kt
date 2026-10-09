package com.example

import kotlinx.coroutines.delay

data class MyraTaskStep(
    val id: Int,
    val action: MyraAction,
    val description: String = "",
    val stopOnFailure: Boolean = true,
    val waitAfterMs: Long = 300L
)

data class MyraTaskExecutionReport(
    val totalSteps: Int,
    val completedSteps: Int,
    val results: List<MyraActionResult>,
    val stopped: Boolean
)

class MyraMultiStepTaskExecutor(
    private val executor: MyraUnifiedActionExecutor
) {
    suspend fun execute(steps: List<MyraTaskStep>): MyraTaskExecutionReport {
        val results = mutableListOf<MyraActionResult>()
        var completed = 0
        var stopped = false
        for (step in steps) {
            val result = executor.execute(step.action)
            results += result
            if (result.status == MyraActionStatus.SUCCESS) {
                completed++
                if (step.waitAfterMs > 0) delay(step.waitAfterMs)
            } else {
                stopped = true
                if (step.stopOnFailure) break
            }
        }
        return MyraTaskExecutionReport(steps.size, completed, results, stopped)
    }
}
