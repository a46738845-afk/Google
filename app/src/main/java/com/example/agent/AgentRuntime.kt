package com.example.agent

import android.content.Context
import android.util.Log
import com.example.llm.ChatMessage
import com.example.llm.LLMProvider
import com.example.llm.MessageRole
import com.example.llm.ModelProviderFactory
import com.example.llm.ToolCall
import com.example.memory.MemoryRepository
import com.example.permissions.PermissionManager
import com.example.security.SecurePreferences
import com.example.tools.ToolResult
import com.example.voice.SpeechEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class AgentRuntime(
    private val context: Context,
    private val memoryRepository: MemoryRepository,
    private val securePreferences: SecurePreferences,
    private val speechEngine: SpeechEngine,
    val toolRegistry: ToolRegistry,
    private val scope: CoroutineScope
) {
    private val permissionManager = PermissionManager()
    private val toolExecutor = ToolExecutor(permissionManager, memoryRepository)
    private val planner = Planner()

    private val _agentState = MutableStateFlow<AgentState>(AgentState.Idle)
    val agentState: StateFlow<AgentState> = _agentState.asStateFlow()

    private val _activeToolSteps = MutableStateFlow<List<String>>(emptyList())
    val activeToolSteps: StateFlow<List<String>> = _activeToolSteps.asStateFlow()

    fun handleUserMessage(prompt: String, attachedImageBase64: String? = null) {
        if (prompt.isBlank() && attachedImageBase64 == null) return

        scope.launch(Dispatchers.IO) {
            try {
                // Save user message in DB
                memoryRepository.saveMessage(
                    role = "user",
                    content = prompt,
                    toolResultJson = if (attachedImageBase64 != null) "IMAGE_ATTACHED" else null
                )

                _agentState.value = AgentState.Thinking("Analyzing request...")
                _activeToolSteps.value = emptyList()

                runAgentLoop(prompt, attachedImageBase64)
            } catch (e: Exception) {
                Log.e("AgentRuntime", "Execution error", e)
                _agentState.value = AgentState.Error(e.localizedMessage ?: "Unknown execution failure")
            }
        }
    }

    private suspend fun runAgentLoop(initialPrompt: String, attachedImageBase64: String? = null) {
        val provider: LLMProvider = ModelProviderFactory.getProvider(securePreferences)
        val memories = memoryRepository.getTopFacts(20)
        val systemPrompt = planner.buildSystemPrompt(securePreferences.autonomyMode, memories)

        // Retrieve recent chat history for context
        val recentDbMessages = memoryRepository.getRecentMessages(limit = 12).reversed()
        val chatMessages = mutableListOf<ChatMessage>()

        for (m in recentDbMessages) {
            val role = when (m.role) {
                "user" -> MessageRole.USER
                "assistant" -> MessageRole.ASSISTANT
                "tool" -> MessageRole.TOOL
                else -> MessageRole.SYSTEM
            }
            val tc = if (m.toolCallName != null) {
                val argsMap = mutableMapOf<String, Any?>()
                try {
                    val obj = JSONObject(m.toolCallArgs ?: "{}")
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        argsMap[k] = obj.get(k)
                    }
                } catch (e: Exception) { }
                ToolCall(name = m.toolCallName, arguments = argsMap)
            } else null

            chatMessages.add(
                ChatMessage(
                    role = role,
                    content = m.content,
                    toolCall = tc,
                    toolCallId = m.toolCallName,
                    imageBase64 = if (m.toolResultJson == "IMAGE_ATTACHED") attachedImageBase64 else null
                )
            )
        }

        // Loop max 5 steps to avoid infinite cycles
        var stepCount = 0
        val maxSteps = 5

        while (stepCount < maxSteps) {
            stepCount++
            val activeTools = toolRegistry.getActiveToolDefinitions()

            _agentState.value = AgentState.Thinking("Evaluating available tools...")
            val response = provider.generate(systemPrompt, chatMessages, activeTools)

            // If model wants to call tools:
            if (response.toolCalls.isNotEmpty()) {
                val toolCall = response.toolCalls.first()
                val tool = toolRegistry.getTool(toolCall.name)

                if (tool == null) {
                    val errorNotice = "Tool '${toolCall.name}' is not recognized."
                    chatMessages.add(
                        ChatMessage(role = MessageRole.TOOL, content = errorNotice, toolCallId = toolCall.name)
                    )
                    continue
                }

                // Check permissions
                if (tool.requiredPermissions.isNotEmpty()) {
                    val missing = permissionManager.getMissingPermissions(context, tool.requiredPermissions)
                    if (missing.isNotEmpty()) {
                        // Pause loop and prompt for permission in UI
                        _agentState.value = AgentState.WaitingPermission(
                            toolName = tool.name,
                            missingPermissions = missing,
                            onGranted = {
                                scope.launch(Dispatchers.IO) {
                                    executeAndContinue(tool, toolCall, chatMessages, provider, systemPrompt, stepCount)
                                }
                            },
                            onDenied = {
                                scope.launch(Dispatchers.IO) {
                                    val deniedMsg = "User denied required Android permission(s) for ${tool.name}."
                                    chatMessages.add(ChatMessage(role = MessageRole.TOOL, content = deniedMsg, toolCallId = tool.name))
                                    resumeAfterToolResult(chatMessages, provider, systemPrompt, stepCount)
                                }
                            }
                        )
                        return
                    }
                }

                // Check safety confirmation requirement
                val needsConfirm = SafetyPolicy.requiresUserConfirmation(tool, securePreferences.autonomyMode)
                if (needsConfirm) {
                    val summary = SafetyPolicy.formatConfirmationSummary(tool.name, toolCall.arguments)
                    _agentState.value = AgentState.WaitingConfirmation(
                        tool = tool,
                        toolCall = toolCall,
                        riskLevel = tool.riskLevel,
                        summary = summary,
                        onConfirm = {
                            scope.launch(Dispatchers.IO) {
                                executeAndContinue(tool, toolCall, chatMessages, provider, systemPrompt, stepCount)
                            }
                        },
                        onReject = {
                            scope.launch(Dispatchers.IO) {
                                val rejectNotice = "Action canceled by user."
                                chatMessages.add(ChatMessage(role = MessageRole.TOOL, content = rejectNotice, toolCallId = tool.name))
                                resumeAfterToolResult(chatMessages, provider, systemPrompt, stepCount)
                            }
                        }
                    )
                    return
                } else {
                    // Execute automatically
                    executeAndContinue(tool, toolCall, chatMessages, provider, systemPrompt, stepCount)
                    return
                }
            } else {
                // Final textual response from model
                val finalAnswer = response.text ?: "Task completed."
                finishWithResponse(finalAnswer)
                return
            }
        }

        finishWithResponse("Process reached maximum multi-step limit.")
    }

    private suspend fun executeAndContinue(
        tool: com.example.tools.AndroidTool,
        toolCall: ToolCall,
        chatMessages: MutableList<ChatMessage>,
        provider: LLMProvider,
        systemPrompt: String,
        currentStep: Int
    ) {
        val stepLabel = "Executing ${tool.name}..."
        _agentState.value = AgentState.ExecutingTool(tool.name, stepLabel)
        _activeToolSteps.value = _activeToolSteps.value + stepLabel

        val result: ToolResult = toolExecutor.execute(context, tool, toolCall, userConfirmed = true)

        val resultSummary = if (result.success) {
            "Success. ${result.userNotice ?: ""}: ${JSONObject(result.data)}"
        } else {
            "Error: ${result.error}"
        }

        // Save tool invocation message in history
        memoryRepository.saveMessage(
            role = "assistant",
            content = "Executed ${tool.name}",
            toolCallName = tool.name,
            toolCallArgs = JSONObject(toolCall.arguments).toString(),
            toolResultJson = resultSummary
        )

        chatMessages.add(
            ChatMessage(
                role = MessageRole.TOOL,
                content = resultSummary,
                toolCallId = tool.name
            )
        )

        resumeAfterToolResult(chatMessages, provider, systemPrompt, currentStep)
    }

    private suspend fun resumeAfterToolResult(
        chatMessages: MutableList<ChatMessage>,
        provider: LLMProvider,
        systemPrompt: String,
        currentStep: Int
    ) {
        _agentState.value = AgentState.Thinking("Observing results and continuing...")
        val activeTools = toolRegistry.getActiveToolDefinitions()
        val followUp = provider.generate(systemPrompt, chatMessages, activeTools)

        if (followUp.toolCalls.isNotEmpty() && currentStep < 5) {
            val nextToolCall = followUp.toolCalls.first()
            val nextTool = toolRegistry.getTool(nextToolCall.name)
            if (nextTool != null) {
                val needsConfirm = SafetyPolicy.requiresUserConfirmation(nextTool, securePreferences.autonomyMode)
                if (needsConfirm) {
                    val summary = SafetyPolicy.formatConfirmationSummary(nextTool.name, nextToolCall.arguments)
                    _agentState.value = AgentState.WaitingConfirmation(
                        tool = nextTool,
                        toolCall = nextToolCall,
                        riskLevel = nextTool.riskLevel,
                        summary = summary,
                        onConfirm = {
                            scope.launch(Dispatchers.IO) {
                                executeAndContinue(nextTool, nextToolCall, chatMessages, provider, systemPrompt, currentStep + 1)
                            }
                        },
                        onReject = {
                            scope.launch(Dispatchers.IO) {
                                finishWithResponse("Secondary action canceled by user.")
                            }
                        }
                    )
                    return
                } else {
                    executeAndContinue(nextTool, nextToolCall, chatMessages, provider, systemPrompt, currentStep + 1)
                    return
                }
            }
        }

        val textAnswer = followUp.text ?: "Completed."
        finishWithResponse(textAnswer)
    }

    private suspend fun finishWithResponse(answerText: String) {
        memoryRepository.saveMessage(role = "assistant", content = answerText)

        if (securePreferences.ttsEnabled) {
            _agentState.value = AgentState.Speaking(answerText)
            speechEngine.speak(answerText)
        }

        _agentState.value = AgentState.Idle
    }

    fun stopSpeaking() {
        speechEngine.stop()
        if (_agentState.value is AgentState.Speaking) {
            _agentState.value = AgentState.Idle
        }
    }
}
