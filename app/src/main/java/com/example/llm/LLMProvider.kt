package com.example.llm

import com.example.tools.ToolDefinition

interface LLMProvider {
    val name: String
    val isLocal: Boolean

    suspend fun generate(
        systemPrompt: String,
        messages: List<ChatMessage>,
        tools: List<ToolDefinition>
    ): AgentResponse

    suspend fun analyzeImage(
        prompt: String,
        imageBase64: String
    ): String
}
