package com.example

class MyraActionDependencyManager {
    fun orderActions(actions: List<MyraAction>): List<MyraAction> =
        actions.toList() // Keep planner order; don't guess dependencies.
}
