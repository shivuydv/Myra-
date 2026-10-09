package com.example

import android.content.Context
import kotlinx.coroutines.withTimeoutOrNull

data class MyraTimeoutPolicy(val actionTimeoutMs: Long = 15_000L, val taskTimeoutMs: Long = 120_000L)
data class MyraTimeoutOutcome(val result: MyraActionResult, val timedOut: Boolean)

class MyraTaskTimeoutGuard(private val policy: MyraTimeoutPolicy = MyraTimeoutPolicy()) {
    suspend fun executeAction(action: MyraAction, executor: MyraUnifiedActionExecutor, timeoutMs: Long = policy.actionTimeoutMs): MyraTimeoutOutcome {
        val timeout = timeoutMs.coerceIn(1_000L, policy.taskTimeoutMs)
        val result = withTimeoutOrNull(timeout) { executor.execute(action) }
        return if (result != null) MyraTimeoutOutcome(result, false) else MyraTimeoutOutcome(
            MyraActionResult(MyraActionStatus.RETRY, "Action timeout हुआ. Retry से पहले screen verify करो."), true
        )
    }
}

class MyraTimeoutRecoveryHandler(context: Context) {
    private val checkpoints = MyraReliableCheckpointEngine(context)
    fun handle(taskId: String, stepIndex: Int, totalSteps: Int, message: String): Boolean =
        checkpoints.saveProgress(taskId, stepIndex, totalSteps, "RETRY_REQUIRED", message)
}
