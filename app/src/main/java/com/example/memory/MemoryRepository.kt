package com.example.memory

import kotlinx.coroutines.flow.Flow

class MemoryRepository(
    private val conversationDao: ConversationDao,
    private val memoryFactDao: MemoryFactDao,
    private val scheduledTaskDao: ScheduledTaskDao,
    private val toolSettingDao: ToolSettingDao,
    private val activityLogDao: ActivityLogDao
) {
    // Conversations
    fun getMessages(sessionId: String = "default"): Flow<List<ConversationMessageEntity>> =
        conversationDao.getMessages(sessionId)

    suspend fun getRecentMessages(sessionId: String = "default", limit: Int = 30): List<ConversationMessageEntity> =
        conversationDao.getRecentMessages(sessionId, limit)

    suspend fun saveMessage(
        role: String,
        content: String,
        toolCallName: String? = null,
        toolCallArgs: String? = null,
        toolResultJson: String? = null,
        sessionId: String = "default"
    ): Long {
        return conversationDao.insertMessage(
            ConversationMessageEntity(
                sessionId = sessionId,
                role = role,
                content = content,
                toolCallName = toolCallName,
                toolCallArgs = toolCallArgs,
                toolResultJson = toolResultJson
            )
        )
    }

    suspend fun clearSession(sessionId: String = "default") =
        conversationDao.clearSession(sessionId)

    // Facts
    fun getAllFacts(): Flow<List<MemoryFactEntity>> = memoryFactDao.getAllFacts()

    suspend fun getTopFacts(limit: Int = 20): List<MemoryFactEntity> =
        memoryFactDao.getTopFacts(limit)

    suspend fun saveFact(category: String, key: String, value: String, importance: Int = 1): Long {
        return memoryFactDao.insertFact(
            MemoryFactEntity(category = category, key = key, value = value, importance = importance)
        )
    }

    suspend fun deleteFact(id: Long) = memoryFactDao.deleteFact(id)
    suspend fun clearAllFacts() = memoryFactDao.clearAll()

    // Tasks
    fun getAllTasks(): Flow<List<ScheduledTaskEntity>> = scheduledTaskDao.getAllTasks()

    suspend fun saveTask(title: String, promptAction: String, triggerTimeEpoch: Long, recurrence: String? = null): Long {
        return scheduledTaskDao.insertTask(
            ScheduledTaskEntity(
                title = title,
                promptAction = promptAction,
                triggerTimeEpoch = triggerTimeEpoch,
                recurrence = recurrence
            )
        )
    }

    suspend fun markTaskCompleted(id: Long) = scheduledTaskDao.markCompleted(id)
    suspend fun deleteTask(id: Long) = scheduledTaskDao.deleteTask(id)

    // Tool Settings
    fun getAllToolSettings(): Flow<List<ToolSettingEntity>> = toolSettingDao.getAllSettings()

    suspend fun isToolEnabled(toolName: String): Boolean {
        val setting = toolSettingDao.getSetting(toolName)
        return setting?.isEnabled ?: true
    }

    suspend fun setToolEnabled(toolName: String, enabled: Boolean) {
        val current = toolSettingDao.getSetting(toolName)
        if (current == null) {
            toolSettingDao.saveSetting(ToolSettingEntity(toolName = toolName, isEnabled = enabled))
        } else {
            toolSettingDao.setEnabled(toolName, enabled)
        }
    }

    // Activity Logs
    fun getRecentLogs(limit: Int = 100): Flow<List<ActivityLogEntity>> =
        activityLogDao.getRecentLogs(limit)

    suspend fun logActivity(log: ActivityLogEntity): Long =
        activityLogDao.insertLog(log)

    suspend fun clearLogs() = activityLogDao.clearLogs()
}
