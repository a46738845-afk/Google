package com.example.agent

import com.example.memory.MemoryRepository
import com.example.tools.AndroidTool
import com.example.tools.ToolDefinition
import com.example.tools.apps.AppsTool
import com.example.tools.browser.BrowserTool
import com.example.tools.calendar.CalendarTool
import com.example.tools.camera.CameraTool
import com.example.tools.clipboard.ClipboardTool
import com.example.tools.contacts.ContactsTool
import com.example.tools.files.FilesTool
import com.example.tools.intents.IntentTool
import com.example.tools.location.LocationTool
import com.example.tools.notifications.NotificationsTool
import com.example.tools.phone.PhoneTool
import com.example.tools.settings.SettingsTool
import com.example.tools.sms.SmsTool
import com.example.tools.tasks.TaskScheduleTool

class ToolRegistry(
    private val memoryRepository: MemoryRepository,
    cameraViewfinderTrigger: (() -> Unit)? = null
) {
    private val registeredTools = mutableMapOf<String, AndroidTool>()

    init {
        register(AppsTool())
        register(IntentTool())
        register(CameraTool(cameraViewfinderTrigger))
        register(ContactsTool())
        register(SmsTool())
        register(PhoneTool())
        register(NotificationsTool())
        register(FilesTool())
        register(ClipboardTool())
        register(LocationTool())
        register(CalendarTool())
        register(BrowserTool())
        register(SettingsTool())
        register(TaskScheduleTool())
    }

    fun register(tool: AndroidTool) {
        registeredTools[tool.name] = tool
    }

    fun getTool(name: String): AndroidTool? = registeredTools[name]

    fun getAllTools(): List<AndroidTool> = registeredTools.values.toList()

    suspend fun getActiveToolDefinitions(): List<ToolDefinition> {
        val result = mutableListOf<ToolDefinition>()
        for ((name, tool) in registeredTools) {
            if (memoryRepository.isToolEnabled(name)) {
                result.add(tool.definition)
            }
        }
        return result
    }
}
