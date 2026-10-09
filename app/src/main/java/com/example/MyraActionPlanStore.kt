package com.example

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class MyraActionPlanStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "myra_action_plans", Context.MODE_PRIVATE
    )

    fun save(taskId: String, plan: MyraActionPlan): Boolean = try {
        val actions = JSONArray()
        plan.actions.forEach { a -> actions.put(JSONObject().apply {
            put("type", a.type); put("target", a.target); put("value", a.value)
            put("requiresConfirmation", a.requiresConfirmation)
            put("expectedElement", a.expectedElement)
            put("expectedPackage", a.expectedPackage)
        }) }
        val json = JSONObject().put("goal", plan.goal).put("actions", actions)
        prefs.edit().putString(taskId, json.toString()).commit()
    } catch (_: Exception) { false }

    fun load(taskId: String): MyraActionPlan? = try {
        val raw = prefs.getString(taskId, null) ?: return null
        val json = JSONObject(raw)
        val array = json.getJSONArray("actions")
        val actions = (0 until array.length()).map { i ->
            val a = array.getJSONObject(i)
            MyraAction(
                type = a.getString("type"), target = a.optString("target", ""),
                value = a.optString("value", ""),
                requiresConfirmation = a.optBoolean("requiresConfirmation", false),
                expectedElement = a.optString("expectedElement", ""),
                expectedPackage = a.optString("expectedPackage", "")
            )
        }
        MyraActionPlan(json.getString("goal"), actions)
    } catch (_: Exception) { null }

    fun delete(taskId: String) { prefs.edit().remove(taskId).apply() }
    fun exists(taskId: String): Boolean = prefs.contains(taskId)
}
