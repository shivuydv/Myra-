package com.example

class MyraResumeAwareTaskExecutor(
    private val planStore: MyraActionPlanStore,
    private val checkpointEngine: MyraTaskCheckpointEngine,
    private val resumeEngine: MyraSafeTaskResumeEngine,
    private val executor: MyraUnifiedActionExecutor
) {
    suspend fun resume(taskId: String, currentPackage: String): MyraActionResult {
        val plan = planStore.load(taskId) ?: return MyraActionResult(
            MyraActionStatus.FAILED, "Saved plan नहीं मिला."
        )
        val decision = resumeEngine.prepareResume(taskId, plan, currentPackage)
        if (decision.status != MyraResumeStatus.READY) return MyraActionResult(
            MyraActionStatus.WAITING, decision.message
        )
        var next = decision.nextStepIndex
        for (index in next until plan.actions.size) {
            val action = plan.actions[index]
            val result = executor.execute(action)
            if (result.status != MyraActionStatus.SUCCESS) {
                val status = when (result.status) {
                    MyraActionStatus.NEED_CONFIRMATION -> "WAITING_CONFIRMATION"
                    MyraActionStatus.WAITING -> "PAUSED"
                    MyraActionStatus.RETRY -> "RETRY_REQUIRED"
                    else -> "FAILED"
                }
                checkpointEngine.save(MyraTaskCheckpoint(
                    taskId, index, plan.actions.size, status,
                    result.message, System.currentTimeMillis()
                ))
                return result
            }
            next = index + 1
            checkpointEngine.save(MyraTaskCheckpoint(
                taskId, next, plan.actions.size,
                if (next == plan.actions.size) "COMPLETED" else "RUNNING",
                result.message, System.currentTimeMillis()
            ))
        }
        return MyraActionResult(MyraActionStatus.SUCCESS, "Task resume पूरा हुआ.")
    }
}
