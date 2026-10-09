package com.example

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MyraTaskCancellationManager {
    private val lock = Mutex()
    private val cancelled = mutableSetOf<String>()
    suspend fun cancel(taskId: String) { lock.withLock { cancelled.add(taskId) } }
    suspend fun isCancelled(taskId: String) = lock.withLock { taskId in cancelled }
    suspend fun check(taskId: String) {
        currentCoroutineContext().ensureActive()
        if (isCancelled(taskId)) throw CancellationException("MYRA task cancelled: $taskId")
    }
    suspend fun clear(taskId: String) { lock.withLock { cancelled.remove(taskId) } }
}
