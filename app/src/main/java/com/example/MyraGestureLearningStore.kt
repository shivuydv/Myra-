package com.example

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class MyraGestureLearningStore(context: Context) {
    private val prefs = context.getSharedPreferences("myra_gesture_learning", Context.MODE_PRIVATE)

    fun save(records: List<MyraGestureExperience>) {
        val a = JSONArray()
        records.takeLast(100).forEach { r ->
            a.put(JSONObject().apply {
                put("packageName",r.packageName); put("direction",r.direction.name)
                put("distanceRatio",r.distanceRatio.toDouble()); put("duration",r.duration)
                put("success",r.success); put("timestamp",r.timestamp)
            })
        }
        prefs.edit().putString("records",a.toString()).apply()
    }

    fun load(): List<MyraGestureExperience> {
        val raw = prefs.getString("records",null) ?: return emptyList()
        return try {
            val a = JSONArray(raw)
            (0 until a.length()).mapNotNull { i ->
                val o = a.optJSONObject(i) ?: return@mapNotNull null
                val d = runCatching {
                    MyraGestureDirection.valueOf(o.optString("direction","UP"))
                }.getOrDefault(MyraGestureDirection.UP)
                MyraGestureExperience(
                    o.optString("packageName",""), d,
                    o.optDouble("distanceRatio",.45).toFloat(),
                    o.optLong("duration",500L), o.optBoolean("success",false),
                    o.optLong("timestamp",0L)
                )
            }
        } catch (_: Exception) { emptyList() }
    }
}
