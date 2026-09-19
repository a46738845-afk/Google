package com.example.llm

enum class MessageRole {
    USER,
    ASSISTANT,
    TOOL,
    SYSTEM
}

data class ToolCall(
    val id: String = System.currentTimeMillis().toString(),
    val name: String,
    val arguments: Map<String, Any?>
)

data class ChatMessage(
    val role: MessageRole,
    val content: String,
    val toolCall: ToolCall? = null,
    val toolCallId: String? = null,
    val imageBase64: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class AgentResponse(
    val text: String? = null,
    val toolCalls: List<ToolCall> = emptyList(),
    val rawJson: String? = null
)
