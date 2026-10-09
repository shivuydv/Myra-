package com.example

data class MyraGoalStep(val description: String, val action: MyraAction)

class MyraGoalStepPlanner {
    fun plan(goal: String, actions: List<MyraAction>) =
        actions.mapIndexed { i, a ->
            MyraGoalStep("Step ${i + 1}: ${a.type}", a)
        }
}
