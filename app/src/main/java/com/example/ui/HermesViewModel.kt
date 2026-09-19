package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.HermesApplication
import com.example.agent.AgentRuntime
import com.example.agent.AgentState
import com.example.agent.ToolRegistry
import com.example.memory.ConversationMessageEntity
import com.example.memory.MemoryRepository
import com.example.security.SecurePreferences
import com.example.voice.HermesSpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MainTab {
    CHAT,
    LOGS,
    MEMORY,
    TOOLS,
    SETTINGS
}

class HermesViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as HermesApplication
    val memoryRepository: MemoryRepository = app.memoryRepository
    val securePreferences: SecurePreferences = app.securePreferences

    private val _currentTab = MutableStateFlow(MainTab.CHAT)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    private val _isCameraOpen = MutableStateFlow(false)
    val isCameraOpen: StateFlow<Boolean> = _isCameraOpen.asStateFlow()

    private val _attachedImageBase64 = MutableStateFlow<String?>(null)
    val attachedImageBase64: StateFlow<String?> = _attachedImageBase64.asStateFlow()

    private val _speechError = MutableStateFlow<String?>(null)
    val speechError: StateFlow<String?> = _speechError.asStateFlow()

    val toolRegistry: ToolRegistry = ToolRegistry(
        memoryRepository = memoryRepository,
        cameraViewfinderTrigger = {
            _isCameraOpen.value = true
        }
    )

    val agentRuntime: AgentRuntime = AgentRuntime(
        context = application.applicationContext,
        memoryRepository = memoryRepository,
        securePreferences = securePreferences,
        speechEngine = app.speechEngine,
        toolRegistry = toolRegistry,
        scope = viewModelScope
    )

    val agentState: StateFlow<AgentState> = agentRuntime.agentState
    val activeToolSteps: StateFlow<List<String>> = agentRuntime.activeToolSteps

    val messages: StateFlow<List<ConversationMessageEntity>> = memoryRepository.getMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Speech Recognizer
    private val speechRecognizer = HermesSpeechRecognizer(
        context = application.applicationContext,
        onResult = { recognizedText ->
            submitMessage(recognizedText)
        },
        onError = { error ->
            _speechError.value = error
        }
    )

    val isListening: StateFlow<Boolean> = speechRecognizer.isListening
    val audioLevel: StateFlow<Float> = speechRecognizer.soundLevel

    fun setTab(tab: MainTab) {
        _currentTab.value = tab
    }

    fun submitMessage(prompt: String) {
        val image = _attachedImageBase64.value
        _attachedImageBase64.value = null
        agentRuntime.handleUserMessage(prompt, image)
    }

    fun toggleVoiceListening() {
        if (isListening.value) {
            speechRecognizer.stopListening()
        } else {
            agentRuntime.stopSpeaking()
            speechRecognizer.startListening()
        }
    }

    fun openCamera() {
        _isCameraOpen.value = true
    }

    fun closeCamera() {
        _isCameraOpen.value = false
    }

    fun onImageCaptured(base64: String) {
        _attachedImageBase64.value = base64
        _isCameraOpen.value = false
        // Automatically ask Hermes to analyze the captured visual
        submitMessage("Here is an image from my camera. Please analyze what you see.")
    }

    fun clearHistory() {
        viewModelScope.launch {
            memoryRepository.clearSession()
        }
    }

    fun stopSpeaking() {
        agentRuntime.stopSpeaking()
    }

    override fun onCleared() {
        speechRecognizer.stopListening()
        super.onCleared()
    }
}
