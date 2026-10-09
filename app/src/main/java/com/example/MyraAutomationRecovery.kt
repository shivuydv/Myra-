package com.example

class MyraAutomationRecovery(private val recovery: MyraFailureRecovery) {
    suspend fun recover(action: MyraAction) = recovery.execute(action)
}
