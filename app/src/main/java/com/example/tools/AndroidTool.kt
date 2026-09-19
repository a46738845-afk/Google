package com.example.tools

import android.content.Context

enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH
}

data class ToolParameter(
    val name: String,
    val type: String, // "string", "number", "boolean", "array", "object"
    val description: String,
    val required: Boolean = true,
    val enumValues: List<String>? = null
)

data class ToolDefinition(
    val name: String,
    val description: String,
    val riskLevel: RiskLevel,
    val requiredPermissions: List<String> = emptyList(),
    val parameters: List<ToolParameter> = emptyList()
)

data class ToolResult(
    val success: Boolean,
    val data: Map<String, Any?> = emptyMap(),
    val error: String? = null,
    val userNotice: String? = null
) {
    companion object {
        fun success(data: Map<String, Any?> = emptyMap(), userNotice: String? = null): ToolResult =
            ToolResult(success = true, data = data, error = null, userNotice = userNotice)

        fun failure(error: String, userNotice: String? = null): ToolResult =
            ToolResult(success = false, data = emptyMap(), error = error, userNotice = userNotice)
    }
}

interface AndroidTool {
    val definition: ToolDefinition
    val name: String get() = definition.name
    val description: String get() = definition.description
    val riskLevel: RiskLevel get() = definition.riskLevel
    val requiredPermissions: List<String> get() = definition.requiredPermissions

    suspend fun execute(
        context: Context,
        arguments: Map<String, Any?>
    ): ToolResult
}
