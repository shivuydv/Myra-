package com.example

import java.security.MessageDigest

object MyraActionIdentity {
    fun create(taskId: String, stepIndex: Int, action: MyraAction): String {
        val raw = listOf(taskId, stepIndex.toString(), action.type, action.target, action.value, action.expectedPackage).joinToString("|")
        val bytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
