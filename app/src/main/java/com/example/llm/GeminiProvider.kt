package com.example.llm

import android.util.Log
import com.example.BuildConfig
import com.example.tools.ToolDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiProvider(
    private val customApiKey: String? = null,
    private val model: String = "gemini-3.5-flash"
) : LLMProvider {

    override val name: String = "Gemini ($model)"
    override val isLocal: Boolean = false

    private val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    private fun getApiKey(): String {
        val custom = customApiKey?.trim()
        if (!custom.isNullOrEmpty()) return custom
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }
    }

    override suspend fun generate(
        systemPrompt: String,
        messages: List<ChatMessage>,
        tools: List<ToolDefinition>
    ): AgentResponse = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext AgentResponse(
                text = "Gemini API key is missing or not configured. Please set your API key in Settings or AI Studio Secrets panel."
            )
        }

        try {
            val root = JSONObject()

            // System instruction
            val sysInstObj = JSONObject()
            val sysParts = JSONArray().put(JSONObject().put("text", systemPrompt))
            sysInstObj.put("parts", sysParts)
            root.put("systemInstruction", sysInstObj)

            // Contents
            val contentsArray = JSONArray()
            for (msg in messages) {
                val contentObj = JSONObject()
                contentObj.put("role", if (msg.role == MessageRole.USER) "user" else "model")
                val partsArray = JSONArray()

                if (msg.toolCall != null) {
                    val fnCall = JSONObject()
                    fnCall.put("name", msg.toolCall.name)
                    val argsObj = JSONObject()
                    msg.toolCall.arguments.forEach { (k, v) -> argsObj.put(k, v) }
                    fnCall.put("args", argsObj)
                    partsArray.put(JSONObject().put("functionCall", fnCall))
                } else if (msg.role == MessageRole.TOOL) {
                    val fnResp = JSONObject()
                    fnResp.put("name", msg.toolCallId ?: "tool")
                    fnResp.put("response", JSONObject().put("output", msg.content))
                    partsArray.put(JSONObject().put("functionResponse", fnResp))
                    contentObj.put("role", "user")
                } else {
                    if (msg.content.isNotEmpty()) {
                        partsArray.put(JSONObject().put("text", msg.content))
                    }
                    if (msg.imageBase64 != null) {
                        val inlineData = JSONObject()
                        inlineData.put("mimeType", "image/jpeg")
                        inlineData.put("data", msg.imageBase64)
                        partsArray.put(JSONObject().put("inlineData", inlineData))
                    }
                }

                contentObj.put("parts", partsArray)
                contentsArray.put(contentObj)
            }
            root.put("contents", contentsArray)

            // Tools / Function declarations
            if (tools.isNotEmpty()) {
                val toolsArray = JSONArray()
                val fnDecls = JSONArray()

                for (tool in tools) {
                    val decl = JSONObject()
                    decl.put("name", tool.name)
                    decl.put("description", tool.description)

                    val paramsObj = JSONObject()
                    paramsObj.put("type", "OBJECT")
                    val propsObj = JSONObject()
                    val reqArray = JSONArray()

                    for (param in tool.parameters) {
                        val p = JSONObject()
                        p.put("type", when (param.type) {
                            "number" -> "NUMBER"
                            "boolean" -> "BOOLEAN"
                            "array" -> "ARRAY"
                            else -> "STRING"
                        })
                        p.put("description", param.description)
                        propsObj.put(param.name, p)
                        if (param.required) reqArray.put(param.name)
                    }

                    paramsObj.put("properties", propsObj)
                    if (reqArray.length() > 0) paramsObj.put("required", reqArray)
                    decl.put("parameters", paramsObj)

                    fnDecls.put(decl)
                }

                toolsArray.put(JSONObject().put("functionDeclarations", fnDecls))
                root.put("tools", toolsArray)
            }

            val requestBody = root.toString().toRequestBody("application/json".toMediaType())
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val respString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e("GeminiProvider", "API error code: ${response.code}, body: $respString")
                return@withContext AgentResponse(
                    text = "Gemini API error (${response.code}): ${parseErrorMessage(respString)}"
                )
            }

            parseGeminiResponse(respString)
        } catch (e: Exception) {
            Log.e("GeminiProvider", "Call failed", e)
            AgentResponse(text = "Error connecting to Gemini: ${e.localizedMessage}")
        }
    }

    override suspend fun analyzeImage(prompt: String, imageBase64: String): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Gemini API key is required to analyze images."
        }

        try {
            val root = JSONObject()
            val contents = JSONArray()
            val contentObj = JSONObject().put("role", "user")
            val parts = JSONArray()

            parts.put(JSONObject().put("text", prompt))
            val inlineData = JSONObject().put("mimeType", "image/jpeg").put("data", imageBase64)
            parts.put(JSONObject().put("inlineData", inlineData))

            contentObj.put("parts", parts)
            contents.put(contentObj)
            root.put("contents", contents)

            val requestBody = root.toString().toRequestBody("application/json".toMediaType())
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

            val request = Request.Builder().url(url).post(requestBody).build()
            val response = client.newCall(request).execute()
            val respString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext "Vision analysis failed (${response.code}): ${parseErrorMessage(respString)}"
            }

            val respJson = JSONObject(respString)
            val candidates = respJson.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val partsArr = firstCandidate?.optJSONObject("content")?.optJSONArray("parts")

            val textBuilder = StringBuilder()
            if (partsArr != null) {
                for (i in 0 until partsArr.length()) {
                    val p = partsArr.getJSONObject(i)
                    if (p.has("text")) textBuilder.append(p.getString("text"))
                }
            }

            textBuilder.toString().ifEmpty { "No image description returned." }
        } catch (e: Exception) {
            "Vision error: ${e.localizedMessage}"
        }
    }

    private fun parseGeminiResponse(jsonString: String): AgentResponse {
        val root = JSONObject(jsonString)
        val candidates = root.optJSONArray("candidates") ?: return AgentResponse(text = "Empty response from Gemini.")
        val candidate = candidates.optJSONObject(0) ?: return AgentResponse(text = "No candidate in response.")
        val content = candidate.optJSONObject("content") ?: return AgentResponse(text = "No content in candidate.")
        val parts = content.optJSONArray("parts") ?: return AgentResponse(text = "No parts in content.")

        val textBuilder = StringBuilder()
        val toolCalls = mutableListOf<ToolCall>()

        for (i in 0 until parts.length()) {
            val part = parts.getJSONObject(i)
            if (part.has("text")) {
                textBuilder.append(part.getString("text"))
            }
            if (part.has("functionCall")) {
                val fnCall = part.getJSONObject("functionCall")
                val fnName = fnCall.getString("name")
                val argsObj = fnCall.optJSONObject("args")
                val argsMap = mutableMapOf<String, Any?>()

                if (argsObj != null) {
                    val keys = argsObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        argsMap[key] = argsObj.get(key)
                    }
                }

                toolCalls.add(ToolCall(name = fnName, arguments = argsMap))
            }
        }

        return AgentResponse(
            text = textBuilder.toString().ifEmpty { null },
            toolCalls = toolCalls,
            rawJson = jsonString
        )
    }

    private fun parseErrorMessage(json: String): String {
        return try {
            val obj = JSONObject(json)
            obj.optJSONObject("error")?.optString("message") ?: json
        } catch (e: Exception) {
            json
        }
    }
}
