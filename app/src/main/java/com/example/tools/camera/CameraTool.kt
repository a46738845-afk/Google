package com.example.tools.camera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import com.example.tools.AndroidTool
import com.example.tools.RiskLevel
import com.example.tools.ToolDefinition
import com.example.tools.ToolParameter
import com.example.tools.ToolResult

class CameraTool(
    private val onTriggerViewfinder: (() -> Unit)? = null
) : AndroidTool {

    companion object {
        var lastCapturedImageBase64: String? = null
    }

    override val definition: ToolDefinition = ToolDefinition(
        name = "take_photo",
        description = "Open the visible on-screen camera viewfinder or launch camera app to capture a photo for vision analysis",
        riskLevel = RiskLevel.LOW,
        requiredPermissions = listOf(Manifest.permission.CAMERA),
        parameters = listOf(
            ToolParameter(
                name = "mode",
                type = "string",
                description = "Camera mode: 'open_camera' to show live viewfinder or 'quick_capture'",
                required = false
            )
        )
    )

    override suspend fun execute(context: Context, arguments: Map<String, Any?>): ToolResult {
        // Trigger live viewfinder UI callback if attached, or launch system camera intent
        if (onTriggerViewfinder != null) {
            onTriggerViewfinder.invoke()
            return ToolResult.success(
                data = mapOf(
                    "status" to "viewfinder_opened",
                    "instruction" to "Visible camera viewfinder activated. Ready for user capture."
                ),
                userNotice = "Camera activated"
            )
        } else {
            try {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return ToolResult.success(
                    data = mapOf("status" to "system_camera_opened"),
                    userNotice = "Opened system camera"
                )
            } catch (e: Exception) {
                return ToolResult.failure("Unable to open camera: ${e.localizedMessage}")
            }
        }
    }
}
