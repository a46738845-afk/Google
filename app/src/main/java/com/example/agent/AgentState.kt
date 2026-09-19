package com.example.agent

import com.example.llm.ToolCall
import com.example.tools.AndroidTool
import com.example.tools.RiskLevel

sealed class AgentState {
    object Idle : AgentState()
    data class Thinking(val statusMessage: String = "Reasoning...") : AgentState()
    data class ExecutingTool(val toolName: String, val stepMessage: String) : AgentState()
    data class WaitingConfirmation(
        val tool: AndroidTool,
        val toolCall: ToolCall,
        val riskLevel: RiskLevel,
        val summary: String,
        val onConfirm: () -> Unit,
        val onReject: () -> Unit
    ) : AgentState()
    data class WaitingPermission(
        val toolName: String,
        val missingPermissions: List<String>,
        val onGranted: () -> Unit,
        val onDenied: () -> Unit
    ) : AgentState()
    data class Speaking(val text: String) : AgentState()
    data class Error(val message: String) : AgentState()
}
