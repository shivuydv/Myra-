package com.example

class MyraTaskLifecycleStateMachine {
    private val allowed = mapOf(
        MyraTaskLifecycle.CREATED to setOf(MyraTaskLifecycle.PLANNING, MyraTaskLifecycle.CANCELLED, MyraTaskLifecycle.FAILED),
        MyraTaskLifecycle.PLANNING to setOf(MyraTaskLifecycle.READY, MyraTaskLifecycle.FAILED, MyraTaskLifecycle.CANCELLED),
        MyraTaskLifecycle.READY to setOf(MyraTaskLifecycle.RUNNING, MyraTaskLifecycle.CANCELLED, MyraTaskLifecycle.FAILED),
        MyraTaskLifecycle.RUNNING to setOf(MyraTaskLifecycle.WAITING_CONFIRMATION, MyraTaskLifecycle.WAITING_INPUT, MyraTaskLifecycle.RETRY_REQUIRED, MyraTaskLifecycle.PAUSED, MyraTaskLifecycle.COMPLETED, MyraTaskLifecycle.FAILED, MyraTaskLifecycle.CANCELLED),
        MyraTaskLifecycle.WAITING_CONFIRMATION to setOf(MyraTaskLifecycle.RUNNING, MyraTaskLifecycle.PAUSED, MyraTaskLifecycle.CANCELLED, MyraTaskLifecycle.FAILED),
        MyraTaskLifecycle.WAITING_INPUT to setOf(MyraTaskLifecycle.RUNNING, MyraTaskLifecycle.PAUSED, MyraTaskLifecycle.CANCELLED, MyraTaskLifecycle.FAILED),
        MyraTaskLifecycle.RETRY_REQUIRED to setOf(MyraTaskLifecycle.RUNNING, MyraTaskLifecycle.PAUSED, MyraTaskLifecycle.FAILED, MyraTaskLifecycle.CANCELLED),
        MyraTaskLifecycle.PAUSED to setOf(MyraTaskLifecycle.RUNNING, MyraTaskLifecycle.CANCELLED)
    )
    @Synchronized fun transition(task: MyraUnifiedTaskContext, next: MyraTaskLifecycle, message: String = ""): Boolean {
        if (task.lifecycle == next) {
            task.lastMessage = message
            task.updatedAt = System.currentTimeMillis()
            return true
        }
        if (next !in (allowed[task.lifecycle] ?: emptySet())) return false
        task.updateLifecycle(next, message)
        return true
    }
    fun canTransition(current: MyraTaskLifecycle, next: MyraTaskLifecycle) =
        current == next || next in (allowed[current] ?: emptySet())
    fun isTerminal(status: MyraTaskLifecycle) = status in setOf(
        MyraTaskLifecycle.COMPLETED, MyraTaskLifecycle.FAILED, MyraTaskLifecycle.CANCELLED
    )
}
