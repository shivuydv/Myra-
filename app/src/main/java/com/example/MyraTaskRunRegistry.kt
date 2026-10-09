package com.example

import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MyraTaskRunRegistry {
    private val lock = Mutex()
    private val jobs = mutableMapOf<String, Job>()
    suspend fun register(taskId: String, job: Job) { lock.withLock { jobs[taskId] = job } }
    suspend fun cancel(taskId: String): Boolean {
        val job = lock.withLock { jobs[taskId] } ?: return false
        job.cancel()
        return true
    }
    suspend fun unregister(taskId: String) { lock.withLock { jobs.remove(taskId) } }
}
