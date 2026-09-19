package com.example.llm

import com.example.security.ProviderType
import com.example.security.SecurePreferences

object ModelProviderFactory {
    fun getProvider(prefs: SecurePreferences): LLMProvider {
        return when (prefs.providerType) {
            ProviderType.GEMINI -> GeminiProvider(
                customApiKey = prefs.apiKey.ifEmpty { null },
                model = prefs.modelName.ifEmpty { "gemini-3.5-flash" }
            )
            ProviderType.OPENROUTER -> OpenAICompatibleProvider(
                apiKey = prefs.apiKey,
                baseUrl = prefs.baseUrl.ifEmpty { "https://openrouter.ai/api/v1/" },
                model = prefs.modelName.ifEmpty { "google/gemini-2.5-flash" },
                isLocal = false,
                name = "OpenRouter (${prefs.modelName})"
            )
            ProviderType.OPENAI_COMPATIBLE -> OpenAICompatibleProvider(
                apiKey = prefs.apiKey,
                baseUrl = prefs.baseUrl.ifEmpty { "https://api.openai.com/v1/" },
                model = prefs.modelName.ifEmpty { "gpt-4o-mini" },
                isLocal = false,
                name = "OpenAI (${prefs.modelName})"
            )
            ProviderType.LOCAL_HTTP -> OpenAICompatibleProvider(
                apiKey = "",
                baseUrl = prefs.localEndpointUrl.ifEmpty { "http://10.0.2.2:11434/v1/" },
                model = prefs.modelName.ifEmpty { "local-model" },
                isLocal = true,
                name = "Local Endpoint"
            )
            ProviderType.MOCK_OFFLINE -> MockOfflineProvider()
        }
    }
}
