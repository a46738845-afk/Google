package com.example.tools.tasks

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.HermesApplication
import com.example.background.HermesReminderWorker
import com.example.tools.AndroidTool
import com.example.tools.RiskLevel
import com.example.tools.ToolDefinition
import com.example.tools.ToolParameter
import com.example.tools.ToolResult
import java.util.concurrent.TimeUnit

class TaskScheduleTool : AndroidTool {
    override val definition: ToolDefinition = ToolDefinition(
        name = "schedule_task",
        description = "Schedule a reminder or background notification in WorkManager for a future time",
        riskLevel = RiskLevel.MEDIUM,
        parameters = listOf(
            ToolParameter(
                name = "title",
                type = "string",
                description = "Reminder title (e.g. 'Meeting with Sarah', 'Drink water')",
                required = true
            ),
            ToolParameter(
                name = "minutes_from_now",
                type = "number",
                description = "Number of minutes to wait before triggering (e.g. 5, 30, 60)",
                required = true
            ),
            ToolParameter(
                name = "prompt",
                type = "string",
                description = "Detailed text to display in notification",
                required = false
            )
        )
    )

    override suspend fun execute(context: Context, arguments: Map<String, Any?>): ToolResult {
        val title = arguments["title"]?.toString() ?: "Reminder"
        val minutes = (arguments["minutes_from_now"] as? Number)?.toLong() ?: 10L
        val prompt = arguments["prompt"]?.toString() ?: title

        val triggerTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(minutes)

        try {
            // Save to Room database
            val taskId = try {
                HermesApplication.instance.memoryRepository.saveTask(
                    title = title,
                    promptAction = prompt,
                    triggerTimeEpoch = triggerTime
                )
            } catch (e: Exception) {
                -1L
            }

            // Schedule WorkManager worker
            val inputData = Data.Builder()
                .putString("title", title)
                .putString("prompt", prompt)
                .putLong("task_id", taskId)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<HermesReminderWorker>()
                .setInitialDelay(minutes, TimeUnit.MINUTES)
                .setInputData(inputData)
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)

            return ToolResult.success(
                data = mapOf(
                    "title" to title,
                    "delay_minutes" to minutes,
                    "task_id" to taskId,
                    "status" to "scheduled"
                ),
                userNotice = "Reminder scheduled for $minutes minute(s) from now"
            )
        } catch (e: Exception) {
            return ToolResult.failure("Failed to schedule task: ${e.localizedMessage}")
        }
    }
}
