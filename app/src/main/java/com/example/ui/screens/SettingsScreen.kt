package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.AutonomyMode
import com.example.security.ProviderType
import com.example.security.SecurePreferences
import com.example.tools.notifications.HermesNotificationListenerService
import com.example.ui.theme.BorderCyan
import com.example.ui.theme.CardBackground
import com.example.ui.theme.CyanCore
import com.example.ui.theme.DeepSpace
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    prefs: SecurePreferences,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var selectedProvider by remember { mutableStateOf(prefs.providerType) }
    var apiKey by remember { mutableStateOf(prefs.apiKey) }
    var baseUrl by remember { mutableStateOf(prefs.baseUrl) }
    var modelName by remember { mutableStateOf(prefs.modelName) }
    var selectedAutonomy by remember { mutableStateOf(prefs.autonomyMode) }
    var ttsEnabled by remember { mutableStateOf(prefs.ttsEnabled) }
    var devMode by remember { mutableStateOf(prefs.developerModeEnabled) }
    var savedNotice by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSpace)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "AGENT RUNTIME SETTINGS",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = CyanCore,
            letterSpacing = 1.2.sp
        )
        Text(
            text = "Configure neural models, security policies & voice telemetry",
            fontSize = 12.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(18.dp))

        // 1. LLM Provider Selector
        Text(
            text = "LLM PROVIDER ARCHITECTURE",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = CyanCore,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = BorderStroke(1.dp, SurfaceDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                ProviderRadioItem("Gemini (Default / Multimodal Vision)", ProviderType.GEMINI, selectedProvider) {
                    selectedProvider = it
                    if (modelName.isEmpty() || modelName.contains("gpt") || modelName.contains("llama")) {
                        modelName = "gemini-3.5-flash"
                    }
                }
                ProviderRadioItem("OpenRouter (Claude, Llama, Gemini)", ProviderType.OPENROUTER, selectedProvider) {
                    selectedProvider = it
                    baseUrl = "https://openrouter.ai/api/v1/"
                    modelName = "google/gemini-2.5-flash"
                }
                ProviderRadioItem("OpenAI Compatible (Direct API)", ProviderType.OPENAI_COMPATIBLE, selectedProvider) {
                    selectedProvider = it
                    baseUrl = "https://api.openai.com/v1/"
                    modelName = "gpt-4o-mini"
                }
                ProviderRadioItem("Local HTTP Endpoint (Ollama, LM Studio)", ProviderType.LOCAL_HTTP, selectedProvider) {
                    selectedProvider = it
                    baseUrl = "http://10.0.2.2:11434/v1/"
                    modelName = "qwen2.5-coder:7b"
                }
                ProviderRadioItem("Offline Hermes Core (Local Rule Engine)", ProviderType.MOCK_OFFLINE, selectedProvider) {
                    selectedProvider = it
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Provider Configuration Inputs
        if (selectedProvider != ProviderType.MOCK_OFFLINE) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, SurfaceDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    if (selectedProvider != ProviderType.LOCAL_HTTP) {
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            label = { Text("API Key (or configure via Secrets panel)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanCore,
                                unfocusedBorderColor = BorderCyan,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (selectedProvider == ProviderType.OPENROUTER ||
                        selectedProvider == ProviderType.OPENAI_COMPATIBLE ||
                        selectedProvider == ProviderType.LOCAL_HTTP
                    ) {
                        OutlinedTextField(
                            value = baseUrl,
                            onValueChange = { baseUrl = it },
                            label = { Text("Base URL Endpoint") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanCore,
                                unfocusedBorderColor = BorderCyan,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    OutlinedTextField(
                        value = modelName,
                        onValueChange = { modelName = it },
                        label = { Text("Model Identifier") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanCore,
                            unfocusedBorderColor = BorderCyan,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 2. Autonomy Mode
        Text(
            text = "AUTONOMY & SAFETY POLICY",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = CyanCore,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = BorderStroke(1.dp, SurfaceDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                AutonomyRadioItem(
                    title = "Assisted Mode (Recommended)",
                    description = "Low-risk tools execute automatically. Sensitive side effects (SMS, calls, calendar events) require authorization.",
                    mode = AutonomyMode.ASSISTED,
                    selected = selectedAutonomy
                ) { selectedAutonomy = it }

                AutonomyRadioItem(
                    title = "Manual Confirmation Mode",
                    description = "Every tool action, regardless of risk, prompts for confirmation before dispatch.",
                    mode = AutonomyMode.MANUAL,
                    selected = selectedAutonomy
                ) { selectedAutonomy = it }

                AutonomyRadioItem(
                    title = "Autonomous Mode",
                    description = "Low and medium risk execute automatically. Critical external side effects still require verification.",
                    mode = AutonomyMode.AUTONOMOUS,
                    selected = selectedAutonomy
                ) { selectedAutonomy = it }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 3. Voice & Telemetry Toggles
        Text(
            text = "VOICE & SYSTEM PERMISSIONS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = CyanCore,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            border = BorderStroke(1.dp, SurfaceDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Text-to-Speech Voice Responses", color = TextPrimary, fontSize = 14.sp)
                        Text("Hermes speaks responses aloud using Android speech synthesis", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = ttsEnabled,
                        onCheckedChange = { ttsEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = DeepSpace, checkedTrackColor = CyanCore)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Notification Listener Access", color = TextPrimary, fontSize = 14.sp)
                        Text(
                            text = if (HermesNotificationListenerService.isServiceConnected) "Status: Connected" else "Status: Not Granted",
                            color = if (HermesNotificationListenerService.isServiceConnected) CyanCore else TextMuted,
                            fontSize = 12.sp
                        )
                    }
                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Configure", color = CyanCore, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Save Settings Button
        Button(
            onClick = {
                prefs.providerType = selectedProvider
                prefs.apiKey = apiKey.trim()
                prefs.baseUrl = baseUrl.trim()
                prefs.modelName = modelName.trim()
                prefs.autonomyMode = selectedAutonomy
                prefs.ttsEnabled = ttsEnabled
                prefs.developerModeEnabled = devMode
                savedNotice = true
            },
            colors = ButtonDefaults.buttonColors(containerColor = CyanCore),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Apply Configuration", color = DeepSpace, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        if (savedNotice) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Configuration updated successfully.", color = CyanCore, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun ProviderRadioItem(
    label: String,
    provider: ProviderType,
    selected: ProviderType,
    onSelect: (ProviderType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(provider) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected == provider,
            onClick = { onSelect(provider) },
            colors = RadioButtonDefaults.colors(selectedColor = CyanCore, unselectedColor = TextMuted)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, color = TextPrimary, fontSize = 13.sp)
    }
}

@Composable
private fun AutonomyRadioItem(
    title: String,
    description: String,
    mode: AutonomyMode,
    selected: AutonomyMode,
    onSelect: (AutonomyMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(mode) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        RadioButton(
            selected = selected == mode,
            onClick = { onSelect(mode) },
            colors = RadioButtonDefaults.colors(selectedColor = CyanCore, unselectedColor = TextMuted)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(text = title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(text = description, color = TextSecondary, fontSize = 11.sp)
        }
    }
}
