package com.example.tools.browser

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.tools.AndroidTool
import com.example.tools.RiskLevel
import com.example.tools.ToolDefinition
import com.example.tools.ToolParameter
import com.example.tools.ToolResult
import java.net.URLEncoder

class BrowserTool : AndroidTool {
    override val definition: ToolDefinition = ToolDefinition(
        name = "search_web",
        description = "Open a website URL or perform a web search using the user's browser",
        riskLevel = RiskLevel.LOW,
        parameters = listOf(
            ToolParameter(
                name = "query",
                type = "string",
                description = "The search query or web URL to navigate to (e.g. 'valorant guide', 'https://wikipedia.org')",
                required = true
            )
        )
    )

    override suspend fun execute(context: Context, arguments: Map<String, Any?>): ToolResult {
        val query = arguments["query"]?.toString()?.trim() ?: ""
        if (query.isEmpty()) {
            return ToolResult.failure("Search query or URL is required.")
        }

        try {
            val intent = if (query.startsWith("http://") || query.startsWith("https://")) {
                Intent(Intent.ACTION_VIEW, Uri.parse(query))
            } else {
                val encoded = URLEncoder.encode(query, "UTF-8")
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$encoded"))
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)

            return ToolResult.success(
                data = mapOf("target" to query, "status" to "dispatched"),
                userNotice = "Searching web for: '$query'"
            )
        } catch (e: Exception) {
            return ToolResult.failure("Unable to open browser: ${e.localizedMessage}")
        }
    }
}
