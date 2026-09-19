package com.example.agent

import android.content.Context
import com.example.llm.ToolCall
import com.example.memory.ActivityLogEntity
import com.example.memory.MemoryRepository
import com.example.permissions.PermissionManager
import com.example.tools.AndroidTool
import com.example.tools.ToolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class ToolExecutor(
    private val permissionManager: PermissionManager,
    private val memoryRepository: MemoryRepository
) {

    suspend fun execute(
        context: Context,
        tool: AndroidTool,
        toolCall: ToolCall,
        userConfirmed: Boolean = true
    ): ToolResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        // 1. Check required permissions
        if (tool.requiredPermissions.isNotEmpty()) {
            val missing = permissionManager.getMissingPermissions(context, tool.requiredPermissions)
            if (missing.isNotEmpty()) {
                val errorMsg = "Missing required Android permissions: ${missing.joinToString(", ")}"
                logActivity(tool, toolCall, null, errorMsg, userConfirmed, System.currentTimeMillis() - startTime)
                return@withContext ToolResult.failure(errorMsg, "Permissions required for ${tool.name}")
            }
        }

        // 2. Execute tool
        val result = try {
            tool.execute(context, toolCall.arguments)
        } catch (e: Exception) {
            ToolResult.failure("Execution threw an exception: ${e.localizedMessage}")
        }

        val duration = System.currentTimeMillis() - startTime

        // 3. Log to database
        logActivity(tool, toolCall, result, result.error, userConfirmed, duration)

        result
    }

    private suspend fun logActivity(
        tool: AndroidTool,
        toolCall: ToolCall,
        result: ToolResult?,
        error: String?,
        userConfirmed: Boolean,
        durationMs: Long
    ) {
        try {
            val argsJson = JSONObject(toolCall.arguments).toString()
            val resultJson = if (result != null) JSONObject(result.data).toString() else null
            val summary = SafetyPolicy.formatConfirmationSummary(tool.name, toolCall.arguments)

            memoryRepository.logActivity(
                ActivityLogEntity(
                    toolName = tool.name,
                    actionSummary = summary,
                    argumentsJson = argsJson,
                    resultJson = resultJson,
                    riskLevel = tool.riskLevel.name,
                    confirmationRequired = !userConfirmed,
                    userConfirmed = userConfirmed,
                    executionDurationMs = durationMs,
                    error = error
                )
            )
        } catch (e: Exception) {
            // Non-fatal logging failure
        }
    }
}
