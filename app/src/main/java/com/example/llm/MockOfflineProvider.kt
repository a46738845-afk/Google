package com.example.llm

import com.example.tools.ToolDefinition

class MockOfflineProvider : LLMProvider {
    override val name: String = "Offline Hermes Core (Rule-Based)"
    override val isLocal: Boolean = true

    override suspend fun generate(
        systemPrompt: String,
        messages: List<ChatMessage>,
        tools: List<ToolDefinition>
    ): AgentResponse {
        val lastUserMessage = messages.lastOrNull { it.role == MessageRole.USER }?.content?.trim() ?: ""
        val lastToolMessage = messages.lastOrNull { it.role == MessageRole.TOOL }

        // If observing a tool result:
        if (lastToolMessage != null) {
            return AgentResponse(
                text = "Hermes: Completed action. Result: ${lastToolMessage.content}"
            )
        }

        val lower = lastUserMessage.lowercase()

        // 1. Open app
        if (lower.startsWith("open ") || lower.startsWith("launch ") || lower.contains("open app")) {
            val appTarget = lastUserMessage
                .replace(Regex("(?i)^(hermes,?\\s*)?(please\\s+)?(open|launch)(\\s+app)?\\s+"), "")
                .replace(Regex("[.?!]+$"), "")
                .trim()
            if (tools.any { it.name == "open_app" }) {
                return AgentResponse(
                    text = "Opening $appTarget for you, sir.",
                    toolCalls = listOf(ToolCall(name = "open_app", arguments = mapOf("app_name" to appTarget)))
                )
            }
        }

        // 2. Camera / photo
        if (lower.contains("camera") || lower.contains("picture") || lower.contains("photo")) {
            if (tools.any { it.name == "take_photo" }) {
                return AgentResponse(
                    text = "Activating optics and camera sensors.",
                    toolCalls = listOf(ToolCall(name = "take_photo", arguments = emptyMap()))
                )
            }
        }

        // 3. Contacts / Phone
        if (lower.contains("contact") || lower.contains("number") || lower.contains("find john") || lower.contains("call")) {
            if (lower.startsWith("call ") && tools.any { it.name == "make_call" }) {
                val name = lastUserMessage.replace(Regex("(?i)^(hermes,?\\s*)?(please\\s+)?call\\s+"), "").trim()
                return AgentResponse(
                    text = "Initiating call protocol to $name.",
                    toolCalls = listOf(ToolCall(name = "make_call", arguments = mapOf("contact_name" to name)))
                )
            }
            if (tools.any { it.name == "search_contacts" }) {
                val query = lastUserMessage.replace(Regex("(?i).*?(contact|number|find|for)\\s+"), "").trim()
                return AgentResponse(
                    text = "Searching local address book for '$query'.",
                    toolCalls = listOf(ToolCall(name = "search_contacts", arguments = mapOf("query" to query)))
                )
            }
        }

        // 4. SMS
        if (lower.contains("sms") || lower.contains("text") || lower.contains("send message")) {
            if (tools.any { it.name == "send_sms" }) {
                return AgentResponse(
                    text = "Drafting transmission. Confirmation is required before dispatch.",
                    toolCalls = listOf(
                        ToolCall(
                            name = "send_sms",
                            arguments = mapOf(
                                "recipient" to "Mom",
                                "message" to "I'll be home soon."
                            )
                        )
                    )
                )
            }
        }

        // 5. Calendar
        if (lower.contains("calendar") || lower.contains("schedule") || lower.contains("meeting")) {
            if (tools.any { it.name == "read_calendar" }) {
                return AgentResponse(
                    text = "Accessing calendar telemetry.",
                    toolCalls = listOf(ToolCall(name = "read_calendar", arguments = mapOf("days_ahead" to 3)))
                )
            }
        }

        // 6. Clipboard
        if (lower.contains("clipboard") || lower.contains("copied")) {
            if (tools.any { it.name == "read_clipboard" }) {
                return AgentResponse(
                    text = "Querying system clipboard buffer.",
                    toolCalls = listOf(ToolCall(name = "read_clipboard", arguments = emptyMap()))
                )
            }
        }

        // 7. Location
        if (lower.contains("location") || lower.contains("where am i") || lower.contains("gps")) {
            if (tools.any { it.name == "get_location" }) {
                return AgentResponse(
                    text = "Acquiring GPS fix and geocoordinates.",
                    toolCalls = listOf(ToolCall(name = "get_location", arguments = emptyMap()))
                )
            }
        }

        // 8. Remind / scheduled task
        if (lower.contains("remind") || lower.contains("timer") || lower.contains("alarm")) {
            if (tools.any { it.name == "schedule_task" }) {
                return AgentResponse(
                    text = "Setting scheduled reminder in task manager.",
                    toolCalls = listOf(
                        ToolCall(
                            name = "schedule_task",
                            arguments = mapOf(
                                "title" to "Reminder",
                                "minutes_from_now" to 60,
                                "prompt" to lastUserMessage
                            )
                        )
                    )
                )
            }
        }

        // 9. Web search / Browser
        if (lower.contains("search") || lower.contains("google") || lower.contains("browse")) {
            val query = lastUserMessage.replace(Regex("(?i).*?search(\\s+for)?\\s+"), "").trim()
            if (tools.any { it.name == "search_web" }) {
                return AgentResponse(
                    text = "Dispatching browser search query for '$query'.",
                    toolCalls = listOf(ToolCall(name = "search_web", arguments = mapOf("query" to query)))
                )
            }
        }

        // Default conversational response
        return AgentResponse(
            text = "Hermes online. I'm ready to assist with device controls, camera capture, contacts, calls, messages, calendar, file management, and web navigation. What would you like to execute, sir?"
        )
    }

    override suspend fun analyzeImage(prompt: String, imageBase64: String): String {
        return "Image received. Optical sensors detect standard environment. Connect cloud LLM (Gemini or OpenRouter) in Settings for deep neural visual reasoning."
    }
}
