package com.example

class MyraConditionEngine {
    fun shouldContinue(r: MyraActionResult) = r.status == MyraActionStatus.SUCCESS
    fun shouldAskConfirmation(r: MyraActionResult) =
        r.status == MyraActionStatus.NEED_CONFIRMATION
    fun shouldWaitForUser(r: MyraActionResult) =
        r.status == MyraActionStatus.WAITING
}
