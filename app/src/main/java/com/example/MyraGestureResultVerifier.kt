package com.example

import kotlinx.coroutines.delay

data class MyraGestureVerification(val changed: Boolean, val message: String)

class MyraGestureResultVerifier {
    suspend fun verify(
        beforePackage: String, beforeClass: String, timeout: Long = 1500L
    ): MyraGestureVerification {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis()-start < timeout) {
            val p = MyraScreenState.currentPackage
            val c = MyraScreenState.currentClass
            if (p != beforePackage || c != beforeClass)
                return MyraGestureVerification(true, "Screen class/package बदला.")
            delay(150L)
        }
        return MyraGestureVerification(false, "Package/class नहीं बदला.")
    }
}
