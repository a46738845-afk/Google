package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agent.ToolRegistry
import com.example.memory.MemoryRepository
import com.example.permissions.AppPermission
import com.example.tools.AndroidTool
import com.example.tools.RiskLevel
import com.example.ui.theme.CardBackground
import com.example.ui.theme.CyanCore
import com.example.ui.theme.DeepSpace
import com.example.ui.theme.HighRiskRed
import com.example.ui.theme.LowRiskGreen
import com.example.ui.theme.MediumRiskYellow
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ToolsScreen(
    toolRegistry: ToolRegistry,
    repository: MemoryRepository,
    modifier: Modifier = Modifier
) {
    val allTools = toolRegistry.getAllTools()
    val toolSettings by repository.getAllToolSettings().collectAsState(initial = emptyList())

    val enabledMap = toolSettings.associate { it.toolName to it.isEnabled }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSpace)
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = "TOOL REGISTRY & CAPABILITIES",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = CyanCore,
                letterSpacing = 1.2.sp
            )
            Text(
                text = "Enable or disable individual Android capabilities",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(allTools) { tool ->
                val isEnabled = enabledMap[tool.name] ?: true
                ToolItemCard(
                    tool = tool,
                    isEnabled = isEnabled,
                    onToggle = { newValue ->
                        CoroutineScope(Dispatchers.IO).launch {
                            repository.setToolEnabled(tool.name, newValue)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ToolItemCard(
    tool: AndroidTool,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val riskColor = when (tool.riskLevel) {
        RiskLevel.HIGH -> HighRiskRed
        RiskLevel.MEDIUM -> MediumRiskYellow
        RiskLevel.LOW -> LowRiskGreen
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) CardBackground else SurfaceDark.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, if (isEnabled) SurfaceDark else SurfaceDark.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tool.name,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isEnabled) TextPrimary else TextMuted
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = riskColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = tool.riskLevel.name,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = riskColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DeepSpace,
                        checkedTrackColor = CyanCore,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = SurfaceDark
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = tool.description,
                fontSize = 12.sp,
                color = if (isEnabled) TextSecondary else TextMuted
            )

            if (tool.requiredPermissions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                val permNames = tool.requiredPermissions.mapNotNull {
                    AppPermission.fromManifest(it)?.title ?: it.substringAfterLast('.')
                }.distinct()
                Text(
                    text = "Requires: ${permNames.joinToString(", ")}",
                    fontSize = 11.sp,
                    color = CyanCore.copy(alpha = 0.75f)
                )
            }

            if (tool.definition.parameters.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                val paramsStr = tool.definition.parameters.joinToString(", ") { p ->
                    "${p.name}${if (p.required) "*" else ""}: ${p.type}"
                }
                Text(
                    text = "params: ($paramsStr)",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted
                )
            }
        }
    }
}
