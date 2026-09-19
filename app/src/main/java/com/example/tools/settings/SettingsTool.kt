package com.example.tools.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.example.tools.AndroidTool
import com.example.tools.RiskLevel
import com.example.tools.ToolDefinition
import com.example.tools.ToolParameter
import com.example.tools.ToolResult

class SettingsTool : AndroidTool {
    override val definition: ToolDefinition = ToolDefinition(
        name = "open_settings",
        description = "Open specific Android system settings panels (e.g. wifi, bluetooth, display, sound, app_info)",
        riskLevel = RiskLevel.LOW,
        parameters = listOf(
            ToolParameter(
                name = "panel",
                type = "string",
                description = "Settings panel to open: 'wifi', 'bluetooth', 'display', 'sound', 'app_info', 'general'",
                required = true,
                enumValues = listOf("wifi", "bluetooth", "display", "sound", "app_info", "general")
            )
        )
    )

    override suspend fun execute(context: Context, arguments: Map<String, Any?>): ToolResult {
        val panel = arguments["panel"]?.toString()?.lowercase() ?: "general"

        try {
            val intent = when (panel) {
                "wifi" -> Intent(Settings.ACTION_WIFI_SETTINGS)
                "bluetooth" -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                "display" -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
                "sound" -> Intent(Settings.ACTION_SOUND_SETTINGS)
                "app_info" -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                else -> Intent(Settings.ACTION_SETTINGS)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)

            return ToolResult.success(
                data = mapOf("panel" to panel, "opened" to true),
                userNotice = "Opened $panel settings"
            )
        } catch (e: Exception) {
            return ToolResult.failure("Could not open settings: ${e.localizedMessage}")
        }
    }
}
