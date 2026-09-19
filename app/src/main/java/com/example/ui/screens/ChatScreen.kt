package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agent.AgentState
import com.example.memory.ConversationMessageEntity
import com.example.ui.HermesViewModel
import com.example.ui.components.ConfirmationCard
import com.example.ui.components.JarvisOrb
import com.example.ui.components.PermissionCard
import com.example.ui.components.ToolExecutionIndicator
import com.example.ui.theme.BorderCyan
import com.example.ui.theme.CardBackground
import com.example.ui.theme.CyanCore
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.DeepSpace
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.HighRiskRed
import com.example.ui.theme.LowRiskGreen
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    viewModel: HermesViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsState()
    val agentState by viewModel.agentState.collectAsState()
    val activeSteps by viewModel.activeToolSteps.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val audioLevel by viewModel.audioLevel.collectAsState()
    val speechError by viewModel.speechError.collectAsState()

    var inputPrompt by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Record Audio Permission launcher for voice mic
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.toggleVoiceListening()
        }
    }

    // Auto-scroll on new message
    LaunchedEffect(messages.size, agentState) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSpace)
    ) {
        // 1. Status Bar Header
        AgentHeaderBar(viewModel = viewModel)

        // 2. Chat / Event Stream
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // Hero Orb Header
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    JarvisOrb(
                        state = agentState,
                        isListening = isListening,
                        audioLevel = audioLevel,
                        size = 110.dp,
                        onClick = {
                            if (agentState is AgentState.Speaking) {
                                viewModel.stopSpeaking()
                            } else {
                                recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = when {
                            isListening -> "HERMES IS LISTENING..."
                            agentState is AgentState.Thinking -> "REASONING & SELECTING TOOLS..."
                            agentState is AgentState.ExecutingTool -> "EXECUTING ON-DEVICE ACTION..."
                            agentState is AgentState.Speaking -> "HERMES IS TRANSMITTING AUDIO..."
                            agentState is AgentState.WaitingConfirmation -> "ACTION REQUIRES AUTHORIZATION"
                            else -> "HERMES RUNTIME ACTIVE"
                        },
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isListening -> CyanGlow
                            agentState is AgentState.WaitingConfirmation -> HighRiskRed
                            else -> CyanCore
                        },
                        letterSpacing = 1.2.sp
                    )

                    if (speechError != null) {
                        Text(
                            text = speechError ?: "",
                            fontSize = 10.sp,
                            color = HighRiskRed,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            // Messages history
            items(messages) { msg ->
                ChatMessageItem(message = msg)
            }

            // Current Tool Execution indicator
            item {
                val stepMessage = (agentState as? AgentState.ExecutingTool)?.stepMessage
                ToolExecutionIndicator(
                    activeSteps = activeSteps,
                    currentStepMessage = stepMessage
                )
            }

            // Pending Confirmation Card
            if (agentState is AgentState.WaitingConfirmation) {
                item {
                    ConfirmationCard(confirmation = agentState as AgentState.WaitingConfirmation)
                }
            }

            // Pending Permission Card
            if (agentState is AgentState.WaitingPermission) {
                item {
                    PermissionCard(request = agentState as AgentState.WaitingPermission)
                }
            }
        }

        // 3. Quick Action Suggestion Chips
        QuickActionRow(
            onSelectPrompt = { prompt ->
                viewModel.submitMessage(prompt)
            }
        )

        // 4. Message Input & Voice Controls Bar
        BottomInputBar(
            input = inputPrompt,
            onInputChange = { inputPrompt = it },
            onSend = {
                if (inputPrompt.isNotBlank()) {
                    viewModel.submitMessage(inputPrompt)
                    inputPrompt = ""
                }
            },
            isListening = isListening,
            onMicTap = {
                recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            },
            onCameraTap = {
                viewModel.openCamera()
            }
        )
    }
}

@Composable
private fun AgentHeaderBar(viewModel: HermesViewModel) {
    val prefs = viewModel.securePreferences

    Surface(
        color = SurfaceDark,
        border = BorderStroke(1.dp, BorderCyan),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = LowRiskGreen,
                    shape = CircleShape,
                    modifier = Modifier.size(8.dp)
                ) {}
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "HERMES",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = CardBackground,
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(0.5.dp, BorderCyan)
                ) {
                    Text(
                        text = prefs.autonomyMode.name,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanCore,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = prefs.providerType.name.replace("_", " "),
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { viewModel.clearHistory() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear History", tint = TextMuted, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ChatMessageItem(message: ConversationMessageEntity) {
    val isUser = message.role == "user"
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val timeStr = timeFormat.format(Date(message.timestamp))

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isUser) 14.dp else 2.dp,
                bottomEnd = if (isUser) 2.dp else 14.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) ElectricBlue.copy(alpha = 0.25f) else CardBackground
            ),
            border = BorderStroke(
                1.dp,
                if (isUser) ElectricBlue.copy(alpha = 0.6f) else BorderCyan
            ),
            modifier = Modifier.fillMaxWidth(0.88f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Header (Role & Time)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUser) "COMMANDER" else "HERMES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isUser) CyanGlow else CyanCore,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = timeStr,
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Content
                Text(
                    text = message.content,
                    fontSize = 14.sp,
                    color = TextPrimary,
                    lineHeight = 20.sp
                )

                // Tool execution result footer if present
                if (message.toolCallName != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = DeepSpace,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            Text(
                                text = "ACTION: ${message.toolCallName}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyanCore
                            )
                            if (message.toolResultJson != null) {
                                Text(
                                    text = message.toolResultJson,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionRow(onSelectPrompt: (String) -> Unit) {
    val prompts = listOf(
        "Open YouTube",
        "Take a photo",
        "Search contacts for John",
        "What's on my calendar?",
        "Read clipboard",
        "Where am I?",
        "Send SMS to Mom",
        "Open Wi-Fi Settings"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        prompts.forEach { prompt ->
            Surface(
                color = CardBackground,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderCyan),
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                Text(
                    text = prompt,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .background(Color.Transparent)
                        .padding(0.dp),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun BottomInputBar(
    input: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    isListening: Boolean,
    onMicTap: () -> Unit,
    onCameraTap: () -> Unit
) {
    Surface(
        color = SurfaceDark,
        border = BorderStroke(1.dp, BorderCyan),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Camera / Optics Button
            IconButton(
                onClick = onCameraTap,
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Visual Optics",
                    tint = CyanCore
                )
            }

            // Input Text Field
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                placeholder = { Text("Directive for Hermes...", color = TextMuted, fontSize = 13.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanCore,
                    unfocusedBorderColor = BorderCyan,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = CyanCore
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Push-to-Talk Mic Button
            IconButton(
                onClick = onMicTap,
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (isListening) CyanGlow else CardBackground,
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Input",
                    tint = if (isListening) DeepSpace else CyanCore,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Send Button
            IconButton(
                onClick = onSend,
                modifier = Modifier
                    .size(44.dp)
                    .background(CyanCore, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send Command",
                    tint = DeepSpace,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
