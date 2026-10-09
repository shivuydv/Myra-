package com.example

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class MyraAuditEntry(
    val taskId: String, val step: Int, val actionType: String,
    val status: String, val message: String, val timestamp: Long
)

class MyraTaskAuditLog(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "myra_task_audit", Context.MODE_PRIVATE
    )
    private val key = "entries"

    @Synchronized
    fun record(entry: MyraAuditEntry): Boolean = try {
        val array = JSONArray(prefs.getString(key, "[]"))
        array.put(JSONObject().apply {
            put("taskId", entry.taskId); put("step", entry.step)
            put("actionType", entry.actionType); put("status", entry.status)
            put("message", entry.message); put("timestamp", entry.timestamp)
        })
        val trimmed = JSONArray()
        val start = (array.length() - 500).coerceAtLeast(0)
        for (i in start until array.length()) trimmed.put(array.get(i))
        prefs.edit().putString(key, trimmed.toString()).commit()
    } catch (_: Exception) { false }

    @Synchronized
    fun getTaskEntries(taskId: String): List<MyraAuditEntry> = try {
        val a = JSONArray(prefs.getString(key, "[]"))
        (0 until a.length()).mapNotNull { i ->
            val j = a.getJSONObject(i)
            if (j.getString("taskId") != taskId) null else MyraAuditEntry(
                j.getString("taskId"), j.getInt("step"), j.getString("actionType"),
                j.getString("status"), j.optString("message", ""), j.getLong("timestamp")
            )
        }
    } catch (_: Exception) { emptyList() }

    fun clearTask(taskId: String) {
        val keep = JSONArray()
        val a = JSONArray(prefs.getString(key, "[]"))
        for (i in 0 until a.length()) {
            val j = a.getJSONObject(i)
            if (j.optString("taskId") != taskId) keep.put(j)
        }
        prefs.edit().putString(key, keep.toString()).apply()
    }
}
