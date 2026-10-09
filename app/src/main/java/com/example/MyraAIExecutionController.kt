package com.example

class MyraAIExecutionController(
    private val planner: MyraAdvancedPlanner,
    private val executor: MyraUnifiedActionExecutor,
    private val memory: MyraContextMemoryManager
) {
    suspend fun execute(command: String): MyraActionResult {
        if (command.isBlank()) return MyraActionResult(
            MyraActionStatus.FAILED, "Command खाली है."
        )
        memory.updateCommand(command)
        val plan = planner.createPlan(command, memory.get())
        if (plan.actions.isEmpty()) return MyraActionResult(
            MyraActionStatus.FAILED, "Executable action नहीं मिला."
        )
        val final = executor.getFinalResult(executor.executePlan(plan))
        memory.updateResult(final)
        return final
    }
}
