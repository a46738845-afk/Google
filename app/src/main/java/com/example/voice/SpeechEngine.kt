package com.example.voice

import kotlinx.coroutines.flow.StateFlow

interface SpeechEngine {
    val isSpeakingFlow: StateFlow<Boolean>
    fun speak(text: String)
    fun stop()
    fun shutdown()
}
