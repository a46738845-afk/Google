package com.example.tools.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.example.tools.AndroidTool
import com.example.tools.RiskLevel
import com.example.tools.ToolDefinition
import com.example.tools.ToolParameter
import com.example.tools.ToolResult

class AppsTool : AndroidTool {
    override val definition: ToolDefinition = ToolDefinition(
        name = "open_app",
        description = "Find and launch an installed Android application by name (e.g., YouTube, Chrome, Spotify, Camera, Settings, Maps)",
        riskLevel = RiskLevel.LOW,
        parameters = listOf(
            ToolParameter(
                name = "app_name",
                type = "string",
                description = "The display name of the application to launch (e.g. YouTube, Maps, Settings)",
                required = true
            )
        )
    )

    override suspend fun execute(context: Context, arguments: Map<String, Any?>): ToolResult {
        val targetName = arguments["app_name"]?.toString()?.trim() ?: ""
        if (targetName.isEmpty()) {
            return ToolResult.failure("Parameter 'app_name' is required.")
        }

        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(intent, 0)

        // Try exact match, then case-insensitive match, then contains match
        val matched = resolveInfos.firstOrNull {
            it.loadLabel(pm).toString().equals(targetName, ignoreCase = true)
        } ?: resolveInfos.firstOrNull {
            it.loadLabel(pm).toString().contains(targetName, ignoreCase = true)
        }

        if (matched != null) {
            val packageName = matched.activityInfo.packageName
            val launchIntent = pm.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                val appLabel = matched.loadLabel(pm).toString()
                return ToolResult.success(
                    data = mapOf(
                        "app_name" to appLabel,
                        "package_name" to packageName,
                        "status" to "launched"
                    ),
                    userNotice = "Opened $appLabel"
                )
            }
        }

        // List some installed apps for helpful feedback
        val sampleApps = resolveInfos.take(8).map { it.loadLabel(pm).toString() }
        return ToolResult.failure(
            "I couldn't find an installed app matching '$targetName'. Installed apps include: ${sampleApps.joinToString(", ")}",
            userNotice = "App '$targetName' not found"
        )
    }
}
