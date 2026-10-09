package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow

@Composable
fun MyraTaskDashboard(
    taskFlow: StateFlow<Map<String, MyraTaskProgress>>,
    onStop: (String) -> Unit
) {
    val tasks by taskFlow.collectAsState()
    var clock by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { delay(1000); clock = System.currentTimeMillis() }
    }
    val active = tasks.values.filter { it.status !in setOf(
        MyraTaskLifecycle.COMPLETED, MyraTaskLifecycle.FAILED, MyraTaskLifecycle.CANCELLED
    ) }.sortedByDescending { it.updatedAt }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("MYRA Task Dashboard", style = MaterialTheme.typography.headlineSmall) }
        if (active.isEmpty()) item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("अभी कोई active task नहीं है.")
                    Text("नया task शुरू करने पर progress यहाँ दिखेगी.")
                }
            }
        }
        items(active, key = { it.taskId }) { task ->
            val elapsed = ((clock - task.startedAt).coerceAtLeast(0L)) / 1000L
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(task.command, style = MaterialTheme.typography.titleMedium)
                    Text("Status: ${task.status}")
                    LinearProgressIndicator(progress = { task.progressPercent / 100f }, modifier = Modifier.fillMaxWidth())
                    Text("${task.progressPercent}% • Step ${task.currentStep}/${task.totalSteps}")
                    Text("समय: ${elapsed / 60}m ${elapsed % 60}s")
                    Text(task.message)
                    if (task.status in setOf(MyraTaskLifecycle.RUNNING, MyraTaskLifecycle.READY, MyraTaskLifecycle.PLANNING)) {
                        Button(onClick = { onStop(task.taskId) }, modifier = Modifier.fillMaxWidth()) { Text("STOP TASK") }
                    }
                }
            }
        }
    }
}

@Composable
fun MyraDashboardScreen(progressMonitor: MyraTaskProgressMonitor, onStopTask: (String) -> Unit) {
    MyraTaskDashboard(taskFlow = progressMonitor.tasks, onStop = onStopTask)
}
