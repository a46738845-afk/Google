package com.example.agent

import com.example.security.AutonomyMode
import com.example.tools.AndroidTool
import com.example.tools.RiskLevel

object SafetyPolicy {

    /**
     * Determines whether the given tool call requires explicit user confirmation.
     * Never allow the LLM to bypass this check.
     */
    fun requiresUserConfirmation(
        tool: AndroidTool,
        autonomyMode: AutonomyMode,
        isExplicitlyEnforced: Boolean = false
    ): Boolean {
        if (isExplicitlyEnforced) return true

        return when (autonomyMode) {
            AutonomyMode.MANUAL -> true // Everything requires confirmation
            AutonomyMode.ASSISTED -> {
                // Low risk runs automatically, Medium and High require confirmation
                tool.riskLevel == RiskLevel.MEDIUM || tool.riskLevel == RiskLevel.HIGH
            }
            AutonomyMode.AUTONOMOUS -> {
                // High risk (SMS, Phone calls, Deletions) always requires confirmation
                tool.riskLevel == RiskLevel.HIGH
            }
        }
    }

    fun formatConfirmationSummary(toolName: String, arguments: Map<String, Any?>): String {
        return when (toolName) {
            "send_sms" -> "Send SMS to '${arguments["recipient"]}' with message: \"${arguments["message"]}\""
            "make_call" -> "Place phone call to '${arguments["phone_number"]}'"
            "manage_calendar" -> "Create calendar event '${arguments["title"]}'"
            "manage_files" -> "${arguments["action"]?.toString()?.uppercase()} file '${arguments["file_name"]}'"
            "schedule_task" -> "Schedule reminder '${arguments["title"]}' in ${arguments["minutes_from_now"]} min"
            else -> "Execute $toolName with arguments: $arguments"
        }
    }
}
