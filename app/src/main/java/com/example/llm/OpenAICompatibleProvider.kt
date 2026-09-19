package com.example.llm

import android.util.Log
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

class OpenAICompatibleProvider(
    private val apiKey: String,
    private val baseUrl: String = "https://openrouter.ai/api/v1/",
    private val model: String = "google/gemini-2.5-flash",
    override val isLocal: Boolean = false,
    override val name: String = "OpenAI Compatible ($model)"
) : LLMProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    override suspend fun generate(
        systemPrompt: String,
        messages: List<ChatMessage>,
        tools: List<ToolDefinition>
    ): AgentResponse = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject()
            root.put("model", model)

            val messagesArr = JSONArray()
            messagesArr.put(JSONObject().put("role", "system").put("content", systemPrompt))

            for (msg in messages) {
                val m = JSONObject()
                m.put("role", when (msg.role) {
                    MessageRole.USER -> "user"
                    MessageRole.ASSISTANT -> "assistant"
                    MessageRole.TOOL -> "tool"
                    MessageRole.SYSTEM -> "system"
                })

                if (msg.role == MessageRole.TOOL) {
                    m.put("tool_call_id", msg.toolCallId ?: "call_1")
                    m.put("content", msg.content)
                } else if (msg.toolCall != null) {
                    m.put("content", JSONObject.NULL)
                    val tc = JSONObject()
                    tc.put("id", msg.toolCall.id)
                    tc.put("type", "function")
                    val fn = JSONObject()
                    fn.put("name", msg.toolCall.name)
                    val argsJson = JSONObject()
                    msg.toolCall.arguments.forEach { (k, v) -> argsJson.put(k, v) }
                    fn.put("arguments", argsJson.toString())
                    tc.put("function", fn)
                    m.put("tool_calls", JSONArray().put(tc))
                } else {
                    m.put("content", msg.content)
                }
                messagesArr.put(m)
            }
            root.put("messages", messagesArr)

            if (tools.isNotEmpty()) {
                val toolsArr = JSONArray()
                for (tool in tools) {
                    val t = JSONObject()
                    t.put("type", "function")
                    val fn = JSONObject()
                    fn.put("name", tool.name)
                    fn.put("description", tool.description)

                    val paramsObj = JSONObject()
                    paramsObj.put("type", "object")
                    val propsObj = JSONObject()
                    val reqArr = JSONArray()

                    for (p in tool.parameters) {
                        val prop = JSONObject()
                        prop.put("type", p.type)
                        prop.put("description", p.description)
                        propsObj.put(p.name, prop)
                        if (p.required) reqArr.put(p.name)
                    }
                    paramsObj.put("properties", propsObj)
                    if (reqArr.length() > 0) paramsObj.put("required", reqArr)
                    fn.put("parameters", paramsObj)

                    t.put("function", fn)
                    toolsArr.put(t)
                }
                root.put("tools", toolsArr)
            }

            val sanitizedBase = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val endpoint = if (sanitizedBase.endsWith("chat/completions/")) sanitizedBase else "${sanitizedBase}chat/completions"

            val body = root.toString().toRequestBody("application/json".toMediaType())
            val reqBuilder = Request.Builder()
                .url(endpoint)
                .post(body)

            if (apiKey.isNotEmpty()) {
                reqBuilder.addHeader("Authorization", "Bearer $apiKey")
            }

            val response = client.newCall(reqBuilder.build()).execute()
            val respString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext AgentResponse(text = "API error (${response.code}): $respString")
            }

            parseResponse(respString)
        } catch (e: Exception) {
            Log.e("OpenAICompatible", "Error", e)
            AgentResponse(text = "Connection failed: ${e.localizedMessage}")
        }
    }

    override suspend fun analyzeImage(prompt: String, imageBase64: String): String = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject()
            root.put("model", model)
            val messagesArr = JSONArray()
            val userMsg = JSONObject()
            userMsg.put("role", "user")

            val contentArr = JSONArray()
            contentArr.put(JSONObject().put("type", "text").put("text", prompt))
            val imgObj = JSONObject()
            imgObj.put("url", "data:image/jpeg;base64,$imageBase64")
            contentArr.put(JSONObject().put("type", "image_url").put("image_url", imgObj))

            userMsg.put("content", contentArr)
            messagesArr.put(userMsg)
            root.put("messages", messagesArr)

            val sanitizedBase = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val endpoint = if (sanitizedBase.endsWith("chat/completions/")) sanitizedBase else "${sanitizedBase}chat/completions"

            val body = root.toString().toRequestBody("application/json".toMediaType())
            val reqBuilder = Request.Builder().url(endpoint).post(body)
            if (apiKey.isNotEmpty()) reqBuilder.addHeader("Authorization", "Bearer $apiKey")

            val response = client.newCall(reqBuilder.build()).execute()
            val respString = response.body?.string() ?: ""

            val respJson = JSONObject(respString)
            respJson.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content")
                ?: "No analysis returned."
        } catch (e: Exception) {
            "Vision analysis error: ${e.localizedMessage}"
        }
    }

    private fun parseResponse(jsonString: String): AgentResponse {
        val root = JSONObject(jsonString)
        val choices = root.optJSONArray("choices") ?: return AgentResponse(text = "No choices in response.")
        val first = choices.optJSONObject(0) ?: return AgentResponse(text = "Empty choice.")
        val message = first.optJSONObject("message") ?: return AgentResponse(text = "No message.")

        val text = if (message.has("content") && !message.isNull("content")) message.getString("content") else null
        val toolCallsArr = message.optJSONArray("tool_calls")
        val toolCalls = mutableListOf<ToolCall>()

        if (toolCallsArr != null) {
            for (i in 0 until toolCallsArr.length()) {
                val tc = toolCallsArr.getJSONObject(i)
                val id = tc.optString("id", System.currentTimeMillis().toString())
                val fn = tc.getJSONObject("function")
                val fnName = fn.getString("name")
                val argsRaw = fn.optString("arguments", "{}")
                val argsObj = JSONObject(argsRaw)
                val argsMap = mutableMapOf<String, Any?>()
                val keys = argsObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    argsMap[k] = argsObj.get(k)
                }
                toolCalls.add(ToolCall(id = id, name = fnName, arguments = argsMap))
            }
        }

        return AgentResponse(
            text = text,
            toolCalls = toolCalls,
            rawJson = jsonString
        )
    }
}
