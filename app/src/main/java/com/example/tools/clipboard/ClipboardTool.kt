package com.example.tools.clipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.example.tools.AndroidTool
import com.example.tools.RiskLevel
import com.example.tools.ToolDefinition
import com.example.tools.ToolParameter
import com.example.tools.ToolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ClipboardTool : AndroidTool {
    override val definition: ToolDefinition = ToolDefinition(
        name = "manage_clipboard",
        description = "Read from or write text to the Android system clipboard",
        riskLevel = RiskLevel.LOW,
        parameters = listOf(
            ToolParameter(
                name = "action",
                type = "string",
                description = "Action: 'read' or 'write'",
                required = true,
                enumValues = listOf("read", "write")
            ),
            ToolParameter(
                name = "text",
                type = "string",
                description = "Text to copy to clipboard (required if action is 'write')",
                required = false
            )
        )
    )

    override suspend fun execute(context: Context, arguments: Map<String, Any?>): ToolResult =
        withContext(Dispatchers.Main) {
            val action = arguments["action"]?.toString()?.lowercase() ?: "read"
            val textToCopy = arguments["text"]?.toString() ?: ""

            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                ?: return@withContext ToolResult.failure("Clipboard service unavailable.")

            try {
                if (action == "write") {
                    if (textToCopy.isEmpty()) {
                        return@withContext ToolResult.failure("Parameter 'text' is required to write to clipboard.")
                    }
                    val clip = ClipData.newPlainText("Hermes Agent", textToCopy)
                    clipboard.setPrimaryClip(clip)
                    ToolResult.success(
                        data = mapOf("status" to "copied", "length" to textToCopy.length),
                        userNotice = "Copied to clipboard"
                    )
                } else {
                    val clip = clipboard.primaryClip
                    if (clip != null && clip.itemCount > 0) {
                        val text = clip.getItemAt(0).text?.toString() ?: ""
                        ToolResult.success(
                            data = mapOf("content" to text, "empty" to text.isEmpty()),
                            userNotice = if (text.isNotEmpty()) "Read clipboard text" else "Clipboard is empty"
                        )
                    } else {
                        ToolResult.success(
                            data = mapOf("content" to "", "empty" to true),
                            userNotice = "Clipboard is empty"
                        )
                    }
                }
            } catch (e: Exception) {
                ToolResult.failure("Clipboard error: ${e.localizedMessage}")
            }
        }
}
