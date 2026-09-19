package com.example.security

import android.content.Context
import android.content.SharedPreferences

enum class ProviderType {
    GEMINI,
    OPENROUTER,
    OPENAI_COMPATIBLE,
    LOCAL_HTTP,
    MOCK_OFFLINE
}

enum class AutonomyMode {
    MANUAL,      // Every tool action requires user confirmation
    ASSISTED,    // Low-risk actions automatic, medium/high risk require confirmation
    AUTONOMOUS   // Low & medium risk automatic, high-risk confirmation
}

class SecurePreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("hermes_secure_settings", Context.MODE_PRIVATE)

    var providerType: ProviderType
        get() {
            val name = prefs.getString(KEY_PROVIDER_TYPE, ProviderType.GEMINI.name)
            return try { ProviderType.valueOf(name ?: ProviderType.GEMINI.name) } catch (e: Exception) { ProviderType.GEMINI }
        }
        set(value) = prefs.edit().putString(KEY_PROVIDER_TYPE, value.name).apply()

    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_API_KEY, value).apply()

    var baseUrl: String
        get() = prefs.getString(KEY_BASE_URL, "https://openrouter.ai/api/v1/") ?: "https://openrouter.ai/api/v1/"
        set(value) = prefs.edit().putString(KEY_BASE_URL, value).apply()

    var localEndpointUrl: String
        get() = prefs.getString(KEY_LOCAL_ENDPOINT, "http://10.0.2.2:11434/v1/") ?: "http://10.0.2.2:11434/v1/"
        set(value) = prefs.edit().putString(KEY_LOCAL_ENDPOINT, value).apply()

    var modelName: String
        get() = prefs.getString(KEY_MODEL_NAME, "gemini-3.5-flash") ?: "gemini-3.5-flash"
        set(value) = prefs.edit().putString(KEY_MODEL_NAME, value).apply()

    var autonomyMode: AutonomyMode
        get() {
            val name = prefs.getString(KEY_AUTONOMY_MODE, AutonomyMode.ASSISTED.name)
            return try { AutonomyMode.valueOf(name ?: AutonomyMode.ASSISTED.name) } catch (e: Exception) { AutonomyMode.ASSISTED }
        }
        set(value) = prefs.edit().putString(KEY_AUTONOMY_MODE, value.name).apply()

    var ttsEnabled: Boolean
        get() = prefs.getBoolean(KEY_TTS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_TTS_ENABLED, value).apply()

    var developerModeEnabled: Boolean
        get() = prefs.getBoolean(KEY_DEV_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_DEV_MODE, value).apply()

    companion object {
        private const val KEY_PROVIDER_TYPE = "provider_type"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_LOCAL_ENDPOINT = "local_endpoint"
        private const val KEY_MODEL_NAME = "model_name"
        private const val KEY_AUTONOMY_MODE = "autonomy_mode"
        private const val KEY_TTS_ENABLED = "tts_enabled"
        private const val KEY_DEV_MODE = "developer_mode"
    }
}
