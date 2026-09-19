package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.memory.ActivityLogEntity
import com.example.memory.MemoryRepository
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ActivityLogScreen(
    repository: MemoryRepository,
    modifier: Modifier = Modifier
) {
    val logs by repository.getRecentLogs().collectAsState(initial = emptyList())
    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSpace)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "AGENT ACTIVITY LOG",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanCore,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "Inspectable timeline of tool executions & telemetry",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            if (logs.isNotEmpty()) {
                IconButton(onClick = {
                    CoroutineScope(Dispatchers.IO).launch {
                        repository.clearLogs()
                    }
                }) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear Logs", tint = TextMuted)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (logs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No tool activity recorded yet.", color = TextMuted, fontSize = 14.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(logs) { log ->
                    LogCard(log = log, timeStr = dateFormat.format(Date(log.timestamp)))
                }
            }
        }
    }
}

@Composable
private fun LogCard(log: ActivityLogEntity, timeStr: String) {
    val riskColor = when (log.riskLevel) {
        "HIGH" -> HighRiskRed
        "MEDIUM" -> MediumRiskYellow
        else -> LowRiskGreen
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, SurfaceDark)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: Tool name + Time + Latency + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (log.error == null) Icons.Default.CheckCircle else Icons.Default.Clear,
                        contentDescription = null,
                        tint = if (log.error == null) LowRiskGreen else HighRiskRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = log.toolName,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = riskColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = log.riskLevel,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = riskColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${log.executionDurationMs}ms",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = timeStr,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = log.actionSummary,
                fontSize = 12.sp,
                color = TextSecondary
            )

            // Arguments block
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                color = DeepSpace,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "args: ${log.argumentsJson}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )
                    if (log.resultJson != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "result: ${log.resultJson}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CyanCore.copy(alpha = 0.8f)
                        )
                    }
                    if (log.error != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "error: ${log.error}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = HighRiskRed
                        )
                    }
                }
            }
        }
    }
}
