package com.example

enum class MyraResumeStatus {
    READY, TASK_NOT_FOUND, PLAN_MISMATCH, INVALID_CHECKPOINT,
    SCREEN_MISMATCH, CONFIRMATION_REQUIRED, ALREADY_COMPLETED, NOT_RESUMABLE
}

data class MyraResumeDecision(
    val status: MyraResumeStatus,
    val taskId: String,
    val nextStepIndex: Int,
    val message: String
)

class MyraSafeTaskResumeEngine(
    private val checkpoints: MyraTaskCheckpointEngine
) {
    fun prepareResume(
        taskId: String,
        savedPlan: MyraActionPlan?,
        currentPackage: String
    ): MyraResumeDecision {
        val cp = checkpoints.load(taskId) ?: return MyraResumeDecision(
            MyraResumeStatus.TASK_NOT_FOUND, taskId, 0, "Checkpoint नहीं मिला."
        )
        if (savedPlan == null) return MyraResumeDecision(
            MyraResumeStatus.PLAN_MISMATCH, taskId, cp.currentStep, "Saved plan नहीं मिला."
        )
        if (cp.totalSteps != savedPlan.actions.size ||
            cp.currentStep !in 0..cp.totalSteps) return MyraResumeDecision(
            MyraResumeStatus.INVALID_CHECKPOINT, taskId, cp.currentStep, "Checkpoint invalid है."
        )
        if (cp.status == "COMPLETED") return MyraResumeDecision(
            MyraResumeStatus.ALREADY_COMPLETED, taskId, cp.currentStep, "Task पहले ही पूरा है."
        )
        if (cp.status !in setOf("RUNNING", "PAUSED", "RETRY_REQUIRED")) {
            return MyraResumeDecision(
                MyraResumeStatus.NOT_RESUMABLE, taskId, cp.currentStep,
                "इस status से automatic resume allowed नहीं है."
            )
        }
        val expectedPackage = savedPlan.actions.getOrNull(cp.currentStep)?.expectedPackage.orEmpty()
        if (expectedPackage.isNotBlank() && currentPackage != expectedPackage) {
            return MyraResumeDecision(
                MyraResumeStatus.SCREEN_MISMATCH, taskId, cp.currentStep,
                "Foreground app expected package से match नहीं करता."
            )
        }
        if (savedPlan.actions.getOrNull(cp.currentStep)?.requiresConfirmation == true) {
            return MyraResumeDecision(
                MyraResumeStatus.CONFIRMATION_REQUIRED, taskId, cp.currentStep,
                "Resume से पहले confirmation जरूरी है."
            )
        }
        return MyraResumeDecision(
            MyraResumeStatus.READY, taskId, cp.currentStep, "Resume की तैयारी पूरी है."
        )
    }
}
