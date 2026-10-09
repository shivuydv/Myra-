package com.example

import java.util.UUID

data class MyraSupervisedTaskResult(
    val taskId: String,
    val result: MyraActionResult,
    val report: MyraTaskExecutionReport?,
    val diagnosis: MyraRecoveryDiagnosis?
)

class MyraEndToEndTaskSupervisor(
    private val planner: MyraAdvancedPlanner,
    private val executor: MyraMultiStepTaskExecutor,
    private val planStore: MyraActionPlanStore,
    private val checkpointEngine: MyraTaskCheckpointEngine,
    private val auditLog: MyraTaskAuditLog,
    private val diagnostics: MyraTaskRecoveryDiagnostics
) {
    suspend fun run(command: String): MyraSupervisedTaskResult {
        val taskId = UUID.randomUUID().toString()
        val plan = planner.createPlan(command)
        if (plan.actions.isEmpty()) return MyraSupervisedTaskResult(
            taskId,
            MyraActionResult(MyraActionStatus.FAILED, "Executable actions नहीं मिले."),
            null, null
        )
        if (!planStore.save(taskId, plan)) return MyraSupervisedTaskResult(
            taskId,
            MyraActionResult(MyraActionStatus.FAILED, "Plan save नहीं हुआ."),
            null, null
        )
        checkpointEngine.save(MyraTaskCheckpoint(
            taskId, 0, plan.actions.size, "RUNNING", "Task शुरू हुआ.",
            System.currentTimeMillis()
        ))
        auditLog.record(MyraAuditEntry(
            taskId, 0, "TASK", "STARTED", "Task शुरू हुआ.", System.currentTimeMillis()
        ))
        val report = executor.execute(plan.actions.mapIndexed { i, action ->
            MyraTaskStep(i, action, "Step ${i + 1}")
        })
        val status = if (!report.stopped && report.completedSteps == report.totalSteps) {
            "COMPLETED"
        } else "FAILED"
        checkpointEngine.save(MyraTaskCheckpoint(
            taskId, report.completedSteps, plan.actions.size, status,
            "Completed ${report.completedSteps}/${report.totalSteps}", System.currentTimeMillis()
        ))
        auditLog.record(MyraAuditEntry(
            taskId, report.completedSteps, "TASK", status,
            "Completed ${report.completedSteps}/${report.totalSteps}", System.currentTimeMillis()
        ))
        val result = if (status == "COMPLETED") MyraActionResult(
            MyraActionStatus.SUCCESS, "Task पूरा हुआ."
        ) else MyraActionResult(MyraActionStatus.FAILED, "Task रुक गया.")
        val diagnosis = if (status == "FAILED") diagnostics.diagnose(
            checkpointEngine.load(taskId), auditLog.getTaskEntries(taskId)
        ) else null
        return MyraSupervisedTaskResult(taskId, result, report, diagnosis)
    }
}
