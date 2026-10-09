package com.example

class MyraDuplicateActionGuard(private val ledger: MyraActionExecutionLedger) {
    fun shouldExecute(taskId: String, actionId: String): Boolean = when (ledger.get(taskId, actionId)?.status) {
        null, MyraLedgerStatus.FAILED -> true
        MyraLedgerStatus.STARTED, MyraLedgerStatus.SUCCEEDED, MyraLedgerStatus.NEEDS_VERIFICATION -> false
    }
    fun mark(taskId: String, actionId: String, action: MyraAction, status: MyraLedgerStatus, message: String): Boolean =
        ledger.record(MyraLedgerEntry(taskId, actionId, action.type, status, message, System.currentTimeMillis()))
}
