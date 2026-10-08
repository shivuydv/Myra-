package com.example

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import org.json.JSONObject

enum class MyraTaskField {
    NONE, TARGET, MESSAGE, CONFIRMATION
}

enum class MyraTaskStatus {
    IDLE,
    RUNNING,
    WAITING_FOR_INPUT,
    WAITING_FOR_CONFIRMATION,
    RETRYING,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class MyraPendingTask(
    val action: String,
    var target: String = "",
    var message: String = "",
    var waitingFor: MyraTaskField = MyraTaskField.NONE
)

data class MyraTaskState(
    val taskId: String,
    var action: String = "",
    var target: String = "",
    var value: String = "",
    var currentStep: Int = 0,
    var totalSteps: Int = 0,
    var status: MyraTaskStatus = MyraTaskStatus.IDLE,
    var lastMessage: String = ""
)

class MyraTaskStore(context: Context) {

    private val prefs =
        context.getSharedPreferences(
            "myra_tasks",
            Context.MODE_PRIVATE
        )

    fun save(state: MyraTaskState) {
        val json = JSONObject().apply {
            put("taskId", state.taskId)
            put("action", state.action)
            put("target", state.target)
            put("value", state.value)
            put("currentStep", state.currentStep)
            put("totalSteps", state.totalSteps)
            put("status", state.status.name)
            put("lastMessage", state.lastMessage)
        }

        prefs.edit()
            .putString("task", json.toString())
            .apply()
    }

    fun clear() {
        prefs.edit().remove("task").apply()
    }
}

class MyraSecureStore(context: Context) {

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val prefs = EncryptedSharedPreferences.create(
        "myra_secure",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun put(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun get(key: String): String? =
        prefs.getString(key, null)

    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }
}
