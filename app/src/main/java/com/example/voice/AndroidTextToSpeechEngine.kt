package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class AndroidTextToSpeechEngine(context: Context) : SpeechEngine, TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false

    private val _isSpeakingFlow = MutableStateFlow(false)
    override val isSpeakingFlow: StateFlow<Boolean> = _isSpeakingFlow.asStateFlow()

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.language = Locale.US
            tts?.setPitch(1.05f) // Slight sci-fi clean pitch
            tts?.setSpeechRate(1.02f)
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeakingFlow.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeakingFlow.value = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeakingFlow.value = false
                }
            })
        }
    }

    override fun speak(text: String) {
        if (!isInitialized || tts == null) return
        stop()
        // Strip markdown codeblocks or symbols for clean vocal delivery
        val cleanText = text
            .replace(Regex("```[\\s\\S]*?```"), "code block omitted.")
            .replace(Regex("[*#_`~]"), "")
            .trim()

        if (cleanText.isEmpty()) return
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "hermes_voice_${System.currentTimeMillis()}")
    }

    override fun stop() {
        tts?.stop()
        _isSpeakingFlow.value = false
    }

    override fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
    }
}
