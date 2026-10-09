package com.example

import android.content.Context
import org.json.JSONObject

data class MyraTaskCheckpoint(
    val taskId: String,
    val currentStep: Int,
    val totalSteps: Int,
    val status: String,
    val lastMessage: String,
    val updatedAt: Long
)

class MyraTaskCheckpointEngine(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "myra_task_checkpoints", Context.MODE_PRIVATE
    )

    fun save(checkpoint: MyraTaskCheckpoint): Boolean = try {
        val json = JSONObject().apply {
            put("taskId", checkpoint.taskId)
            put("currentStep", checkpoint.currentStep)
            put("totalSteps", checkpoint.totalSteps)
            put("status", checkpoint.status)
            put("lastMessage", checkpoint.lastMessage)
            put("updatedAt", checkpoint.updatedAt)
        }
        prefs.edit().putString(checkpoint.taskId, json.toString())
            .putString("latest_task_id", checkpoint.taskId).commit()
    } catch (_: Exception) { false }

    fun load(taskId: String): MyraTaskCheckpoint? = try {
        val raw = prefs.getString(taskId, null) ?: return null
        val j = JSONObject(raw)
        MyraTaskCheckpoint(
            j.getString("taskId"), j.getInt("currentStep"),
            j.getInt("totalSteps"), j.getString("status"),
            j.optString("lastMessage", ""), j.getLong("updatedAt")
        )
    } catch (_: Exception) { null }

    fun loadLatest(): MyraTaskCheckpoint? =
        prefs.getString("latest_task_id", null)?.let(::load)

    fun delete(taskId: String) {
        prefs.edit().remove(taskId).apply()
        if (prefs.getString("latest_task_id", null) == taskId) {
            prefs.edit().remove("latest_task_id").apply()
        }
    }
}
