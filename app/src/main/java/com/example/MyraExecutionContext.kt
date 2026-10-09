package com.example

data class MyraExecutionContext(
    var currentPackage: String = "",
    var currentClass: String = "",
    var lastActionType: String = "",
    var lastActionTarget: String = "",
    var lastResult: MyraActionResult? = null
)
