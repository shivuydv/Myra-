package com.example

import android.content.Context

class MyraReliableCheckpointEngine(context: Context) {
    private val delegate = MyraTaskCheckpointEngine(context.applicationContext)
    fun saveProgress(taskId: String, nextStepIndex: Int, totalSteps: Int, status: String, message: String): Boolean {
        if (taskId.isBlank() || totalSteps < 0 || nextStepIndex !in 0..totalSteps) return false
        return delegate.save(MyraTaskCheckpoint(
            taskId, nextStepIndex, totalSteps, status, message, System.currentTimeMillis()
        ))
    }
    fun load(taskId: String) = delegate.load(taskId)
    fun canResume(checkpoint: MyraTaskCheckpoint, actualPlanSize: Int) =
        checkpoint.taskId.isNotBlank() && checkpoint.totalSteps == actualPlanSize &&
        checkpoint.currentStep in 0..actualPlanSize && checkpoint.status in setOf("RUNNING", "PAUSED", "RETRY_REQUIRED")
}
