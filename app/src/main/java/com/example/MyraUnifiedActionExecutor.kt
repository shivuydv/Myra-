package com.example

import android.content.Context
import kotlinx.coroutines.delay

class MyraActionPolicyEngine {
    fun evaluate(action: MyraAction): MyraPolicy {
        return MyraActionPolicy.check(action)
    }
}

class MyraUnifiedActionExecutor(
    private val capabilityManager: MyraCapabilityManager,
    private val policy: MyraActionPolicyEngine,
    private val verifier: MyraSmartVerifier = MyraSmartVerifier()
) {
    suspend fun execute(action: MyraAction): MyraActionResult {
        return when (policy.evaluate(action)) {
            MyraPolicy.BLOCKED -> MyraActionResult(
                MyraActionStatus.FAILED, "Safety policy ने action block किया."
            )
            MyraPolicy.CONFIRM -> MyraActionResult(
                MyraActionStatus.NEED_CONFIRMATION, "Confirmation चाहिये."
            )
            MyraPolicy.ALLOWED -> executeAllowed(action)
        }
    }

    private suspend fun executeAllowed(action: MyraAction): MyraActionResult {
        var last = MyraActionResult(MyraActionStatus.FAILED, "Action fail हुआ.")
        repeat(3) { attempt ->
            last = capabilityManager.execute(action)
            if (last.status == MyraActionStatus.SUCCESS) {
                if (action.expectedElement.isBlank() &&
                    action.expectedPackage.isBlank()) return last
                val v = verifier.verify(
                    expectedElement = action.expectedElement,
                    expectedPackage = action.expectedPackage
                )
                if (v.success) return last
                last = MyraActionResult(
                    MyraActionStatus.RETRY, "Action verify नहीं हुआ.",
                    retryCount = attempt + 1
                )
            }
            if (last.status == MyraActionStatus.NEED_CONFIRMATION ||
                last.status == MyraActionStatus.WAITING) return last
            if (attempt < 2) delay(400L)
        }
        return last
    }

    suspend fun executePlan(plan: MyraActionPlan): List<MyraActionResult> {
        val results = mutableListOf<MyraActionResult>()
        for (action in plan.actions) {
            val result = execute(action)
            results += result
            if (result.status == MyraActionStatus.FAILED ||
                result.status == MyraActionStatus.RETRY ||
                result.status == MyraActionStatus.NEED_CONFIRMATION ||
                result.status == MyraActionStatus.WAITING) break
        }
        return results
    }

    fun getFinalResult(results: List<MyraActionResult>): MyraActionResult {
        if (results.isEmpty()) return MyraActionResult(
            MyraActionStatus.FAILED, "कोई action execute नहीं हुआ."
        )
        results.firstOrNull { it.status != MyraActionStatus.SUCCESS }?.let { return it }
        return MyraActionResult(
            MyraActionStatus.SUCCESS,
            "सारे ${results.size} actions complete हो गए."
        )
    }
}
