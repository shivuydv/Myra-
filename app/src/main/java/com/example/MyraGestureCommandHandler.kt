package com.example

import android.content.Context

class MyraGestureCommandHandler(context: Context) {
    private val orchestrator = MyraGestureExecutionOrchestrator(context)

    fun isGestureCommand(command: String): Boolean {
        val t = command.lowercase()
        return listOf(
            "scroll","swipe","ऊपर","नीचे","दाएं","दायें","बाएं","बायें",
            "right","left","up","down"
        ).any { it in t }
    }

    suspend fun execute(
        command: String, section: MyraScreenSection = MyraScreenSection.UNKNOWN
    ): MyraActionResult {
        if (!isGestureCommand(command)) return MyraActionResult(
            MyraActionStatus.FAILED,"यह gesture command नहीं है."
        )
        return orchestrator.execute(command,section)
    }
}
