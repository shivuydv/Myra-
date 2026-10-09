package com.example

enum class MyraFailureCause {
    TASK_NOT_FOUND, ACTION_FAILED, CONFIRMATION_PENDING, TARGET_NOT_FOUND,
    SCREEN_MISMATCH, RETRY_REQUIRED, CHECKPOINT_INCONSISTENT, UNKNOWN
}
enum class MyraRecoveryRecommendation {
    RELOAD_TASK, RECHECK_SCREEN, FIND_TARGET_AGAIN, REQUEST_CONFIRMATION,
    RETRY_AFTER_REVIEW, RESTART_FROM_CHECKPOINT, MANUAL_INTERVENTION
}
data class MyraRecoveryDiagnosis(
    val cause: MyraFailureCause,
    val recommendation: MyraRecoveryRecommendation,
    val explanation: String,
    val safeToRetryAutomatically: Boolean
)

class MyraTaskRecoveryDiagnostics {
    fun diagnose(
        checkpoint: MyraTaskCheckpoint?,
        auditEntries: List<MyraAuditEntry>
    ): MyraRecoveryDiagnosis {
        if (checkpoint == null) return MyraRecoveryDiagnosis(
            MyraFailureCause.TASK_NOT_FOUND, MyraRecoveryRecommendation.RELOAD_TASK,
            "Checkpoint नहीं मिला.", false
        )
        if (checkpoint.currentStep !in 0..checkpoint.totalSteps) return MyraRecoveryDiagnosis(
            MyraFailureCause.CHECKPOINT_INCONSISTENT,
            MyraRecoveryRecommendation.MANUAL_INTERVENTION,
            "Checkpoint step range से बाहर है.", false
        )
        if (checkpoint.status == "WAITING_CONFIRMATION") return MyraRecoveryDiagnosis(
            MyraFailureCause.CONFIRMATION_PENDING,
            MyraRecoveryRecommendation.REQUEST_CONFIRMATION,
            "आगे बढ़ने से पहले user confirmation जरूरी है.", false
        )
        val latest = auditEntries.maxByOrNull { it.timestamp }
        return when {
            latest?.status == "TARGET_NOT_FOUND" -> MyraRecoveryDiagnosis(
                MyraFailureCause.TARGET_NOT_FOUND,
                MyraRecoveryRecommendation.FIND_TARGET_AGAIN,
                "Target फिर से खोजें.", false
            )
            checkpoint.status == "RETRY_REQUIRED" -> MyraRecoveryDiagnosis(
                MyraFailureCause.RETRY_REQUIRED,
                MyraRecoveryRecommendation.RETRY_AFTER_REVIEW,
                "Retry से पहले screen और पिछले action का प्रभाव verify करें.", false
            )
            checkpoint.status == "FAILED" -> MyraRecoveryDiagnosis(
                MyraFailureCause.ACTION_FAILED,
                MyraRecoveryRecommendation.RECHECK_SCREEN,
                "Failure के बाद screen state जाँचें.", false
            )
            else -> MyraRecoveryDiagnosis(
                MyraFailureCause.UNKNOWN,
                MyraRecoveryRecommendation.MANUAL_INTERVENTION,
                "कारण स्पष्ट नहीं; manual review करें.", false
            )
        }
    }
}
