package com.example

import android.content.Context
import org.json.JSONObject

enum class MyraLedgerStatus { STARTED, SUCCEEDED, FAILED, NEEDS_VERIFICATION }

data class MyraLedgerEntry(
    val taskId: String, val actionId: String, val actionType: String,
    val status: MyraLedgerStatus, val message: String, val updatedAt: Long
)

class MyraActionExecutionLedger(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("myra_action_execution_ledger", Context.MODE_PRIVATE)
    private fun key(taskId: String, actionId: String) = "$taskId::$actionId"
    @Synchronized fun get(taskId: String, actionId: String): MyraLedgerEntry? = try {
        val raw = prefs.getString(key(taskId, actionId), null) ?: return null
        val j = JSONObject(raw)
        MyraLedgerEntry(
            j.getString("taskId"), j.getString("actionId"), j.getString("actionType"),
            MyraLedgerStatus.valueOf(j.getString("status")), j.optString("message", ""), j.getLong("updatedAt")
        )
    } catch (_: Exception) { null }
    @Synchronized fun record(entry: MyraLedgerEntry): Boolean = try {
        val j = JSONObject().apply {
            put("taskId", entry.taskId); put("actionId", entry.actionId); put("actionType", entry.actionType)
            put("status", entry.status.name); put("message", entry.message); put("updatedAt", entry.updatedAt)
        }
        prefs.edit().putString(key(entry.taskId, entry.actionId), j.toString()).commit()
    } catch (_: Exception) { false }
}
