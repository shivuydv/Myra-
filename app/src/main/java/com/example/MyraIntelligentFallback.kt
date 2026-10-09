package com.example

import android.content.Context

class MyraIntelligentFallback(private val context: Context) {
    suspend fun click(target: String): MyraActionResult {
        val service = MyraAccessibility.instance
            ?: return MyraActionResult(
                MyraActionStatus.FAILED, "Accessibility चालू नहीं है."
            )
        if (service.clickText(target)) return MyraActionResult(
            MyraActionStatus.SUCCESS, "$target पर click किया."
        )
        return MyraFallbackEngine().clickWithFallback(target)
    }
}
