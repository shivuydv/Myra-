package com.example

enum class MyraAppState { FOREGROUND, BACKGROUND, UNKNOWN }

class MyraAppStateDetector {
    fun detect(expectedPackage: String, currentPackage: String) =
        when {
            expectedPackage.isBlank() || currentPackage.isBlank() ->
                MyraAppState.UNKNOWN
            expectedPackage == currentPackage -> MyraAppState.FOREGROUND
            else -> MyraAppState.BACKGROUND
        }
}
