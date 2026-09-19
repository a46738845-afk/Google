package com.example.tools.intents

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.tools.AndroidTool
import com.example.tools.RiskLevel
import com.example.tools.ToolDefinition
import com.example.tools.ToolParameter
import com.example.tools.ToolResult

class IntentTool : AndroidTool {
    override val definition: ToolDefinition = ToolDefinition(
        name = "launch_intent",
        description = "Safely launch supported Android system actions from a validated whitelist (VIEW, SEND, DIAL, WEB_SEARCH)",
        riskLevel = RiskLevel.LOW,
        parameters = listOf(
            ToolParameter(
                name = "action",
                type = "string",
                description = "The Android action: 'ACTION_VIEW', 'ACTION_SEND', 'ACTION_DIAL', 'ACTION_WEB_SEARCH'",
                required = true,
                enumValues = listOf("ACTION_VIEW", "ACTION_SEND", "ACTION_DIAL", "ACTION_WEB_SEARCH")
            ),
            ToolParameter(
                name = "uri",
                type = "string",
                description = "Optional URI string (e.g. 'https://...', 'tel:12345678', 'geo:0,0?q=restaurants')",
                required = false
            ),
            ToolParameter(
                name = "text",
                type = "string",
                description = "Optional text for SEND or query for WEB_SEARCH",
                required = false
            )
        )
    )

    private val allowedActions = setOf(
        "ACTION_VIEW",
        "ACTION_SEND",
        "ACTION_DIAL",
        "ACTION_WEB_SEARCH"
    )

    override suspend fun execute(context: Context, arguments: Map<String, Any?>): ToolResult {
        val actionName = arguments["action"]?.toString()?.trim() ?: ""
        val uriStr = arguments["uri"]?.toString()?.trim()
        val textExtra = arguments["text"]?.toString()?.trim()

        if (actionName !in allowedActions) {
            return ToolResult.failure("Action '$actionName' is rejected by security policy. Allowed actions: ${allowedActions.joinToString(", ")}")
        }

        try {
            val intent = when (actionName) {
                "ACTION_VIEW" -> {
                    if (uriStr.isNullOrEmpty()) {
                        return ToolResult.failure("ACTION_VIEW requires a valid 'uri' parameter.")
                    }
                    val parsedUri = Uri.parse(uriStr)
                    val scheme = parsedUri.scheme?.lowercase()
                    if (scheme != "http" && scheme != "https" && scheme != "geo" && scheme != "market") {
                        return ToolResult.failure("URI scheme '$scheme' is not permitted for ACTION_VIEW.")
                    }
                    Intent(Intent.ACTION_VIEW, parsedUri)
                }

                "ACTION_SEND" -> {
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, textExtra ?: "")
                    }
                }

                "ACTION_DIAL" -> {
                    val uri = if (!uriStr.isNullOrEmpty()) {
                        if (uriStr.startsWith("tel:")) Uri.parse(uriStr) else Uri.parse("tel:$uriStr")
                    } else {
                        Uri.parse("tel:")
                    }
                    Intent(Intent.ACTION_DIAL, uri)
                }

                "ACTION_WEB_SEARCH" -> {
                    Intent(Intent.ACTION_WEB_SEARCH).apply {
                        putExtra(SearchManager.QUERY, textExtra ?: "")
                    }
                }

                else -> return ToolResult.failure("Unsupported action")
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)

            return ToolResult.success(
                data = mapOf("action" to actionName, "dispatched" to true),
                userNotice = "Dispatched $actionName"
            )
        } catch (e: Exception) {
            return ToolResult.failure("Failed to launch intent: ${e.localizedMessage}")
        }
    }
}
