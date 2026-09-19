package com.example.memory

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversation_messages")
data class ConversationMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String = "default",
    val role: String, // "user", "assistant", "tool", "system"
    val content: String,
    val toolCallName: String? = null,
    val toolCallArgs: String? = null,
    val toolResultJson: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "memory_facts")
data class MemoryFactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String, // "preference", "fact", "rule", "contact_note"
    val key: String,
    val value: String,
    val timestamp: Long = System.currentTimeMillis(),
    val importance: Int = 1
)

@Entity(tableName = "scheduled_tasks")
data class ScheduledTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val promptAction: String,
    val triggerTimeEpoch: Long,
    val recurrence: String? = null, // "none", "daily", "weekly"
    val isCompleted: Boolean = false,
    val workRequestId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tool_settings")
data class ToolSettingEntity(
    @PrimaryKey val toolName: String,
    val isEnabled: Boolean = true,
    val requiresExplicitConfirmation: Boolean = false,
    val allowedInAutonomous: Boolean = true
)

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val toolName: String,
    val actionSummary: String,
    val argumentsJson: String,
    val resultJson: String? = null,
    val riskLevel: String, // "LOW", "MEDIUM", "HIGH"
    val confirmationRequired: Boolean = false,
    val userConfirmed: Boolean = true,
    val executionDurationMs: Long = 0L,
    val error: String? = null
)
