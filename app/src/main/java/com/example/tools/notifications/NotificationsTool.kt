package com.example.tools.notifications

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.example.tools.AndroidTool
import com.example.tools.RiskLevel
import com.example.tools.ToolDefinition
import com.example.tools.ToolParameter
import com.example.tools.ToolResult

class NotificationsTool : AndroidTool {
    override val definition: ToolDefinition = ToolDefinition(
        name = "read_notifications",
        description = "Read and summarize recent notifications posted by apps (messages, alerts, updates). Requires explicit Notification Listener access.",
        riskLevel = RiskLevel.LOW,
        parameters = listOf(
            ToolParameter(
                name = "limit",
                type = "number",
                description = "Maximum number of recent notifications to retrieve (1-10)",
                required = false
            )
        )
    )

    override suspend fun execute(context: Context, arguments: Map<String, Any?>): ToolResult {
        if (!HermesNotificationListenerService.isServiceConnected) {
            // Prompt user to enable in settings
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return ToolResult.failure(
                "Notification Listener access is not enabled. I've opened Android Notification Access settings so you can enable Hermes if you wish.",
                userNotice = "Notification Access required"
            )
        }

        val limit = (arguments["limit"] as? Number)?.toInt() ?: 5
        val list = HermesNotificationListenerService.recentNotifications.take(limit)

        if (list.isEmpty()) {
            return ToolResult.success(
                data = mapOf("notifications" to emptyList<String>(), "count" to 0),
                userNotice = "No recent notifications"
            )
        }

        val items = list.map {
            mapOf(
                "app" to it.packageName.substringAfterLast('.'),
                "title" to it.title,
                "text" to it.text
            )
        }

        return ToolResult.success(
            data = mapOf(
                "count" to items.size,
                "notifications" to items
            ),
            userNotice = "Found ${items.size} notification(s)"
        )
    }
}
