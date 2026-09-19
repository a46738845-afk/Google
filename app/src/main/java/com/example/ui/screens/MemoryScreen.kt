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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.memory.MemoryFactEntity
import com.example.memory.MemoryRepository
import com.example.memory.ScheduledTaskEntity
import com.example.ui.theme.BorderCyan
import com.example.ui.theme.CardBackground
import com.example.ui.theme.CyanCore
import com.example.ui.theme.DeepSpace
import com.example.ui.theme.HighRiskRed
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
fun MemoryScreen(
    repository: MemoryRepository,
    modifier: Modifier = Modifier
) {
    val facts by repository.getAllFacts().collectAsState(initial = emptyList())
    val tasks by repository.getAllTasks().collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    var newKey by remember { mutableStateOf("") }
    var newValue by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf("preference") }

    val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSpace)
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "LONG-TERM MEMORY",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanCore,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "Stored user facts, rules, and background tasks",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Row {
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Memory", tint = CyanCore)
                }
                if (facts.isNotEmpty()) {
                    IconButton(onClick = {
                        CoroutineScope(Dispatchers.IO).launch {
                            repository.clearAllFacts()
                        }
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear All Memories", tint = HighRiskRed)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (tasks.isNotEmpty()) {
                item {
                    Text(
                        text = "SCHEDULED TASKS (${tasks.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanCore,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
                    )
                }

                items(tasks) { task ->
                    TaskCard(task = task, timeStr = dateFormat.format(Date(task.triggerTimeEpoch)), onDelete = {
                        CoroutineScope(Dispatchers.IO).launch { repository.deleteTask(task.id) }
                    })
                }
            }

            item {
                Text(
                    text = "FACTS & PREFERENCES (${facts.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanCore,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                )
            }

            if (facts.isEmpty()) {
                item {
                    Surface(
                        color = CardBackground,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = TextMuted, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No facts memorized yet.", color = TextMuted, fontSize = 13.sp)
                            Text("Hermes learns facts automatically, or tap '+' above to add one.", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                items(facts) { fact ->
                    FactCard(fact = fact, onDelete = {
                        CoroutineScope(Dispatchers.IO).launch { repository.deleteFact(fact.id) }
                    })
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = CardBackground,
            title = { Text("Add Memory Fact", color = CyanCore, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newKey,
                        onValueChange = { newKey = it },
                        label = { Text("Key / Subject (e.g. Mom, Favorite Game)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanCore,
                            unfocusedBorderColor = BorderCyan,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newValue,
                        onValueChange = { newValue = it },
                        label = { Text("Value / Detail (e.g. Jane Doe, Valorant)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanCore,
                            unfocusedBorderColor = BorderCyan,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newKey.isNotBlank() && newValue.isNotBlank()) {
                            CoroutineScope(Dispatchers.IO).launch {
                                repository.saveFact(newCategory, newKey.trim(), newValue.trim())
                            }
                            newKey = ""
                            newValue = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanCore)
                ) {
                    Text("Save Fact", color = DeepSpace, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }
}

@Composable
private fun FactCard(fact: MemoryFactEntity, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, SurfaceDark)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = CyanCore.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = fact.category.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanCore,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = fact.key,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = fact.value,
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun TaskCard(task: ScheduledTaskEntity, timeStr: String, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, SurfaceDark)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = task.promptAction,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Trigger: $timeStr",
                    fontSize = 11.sp,
                    color = CyanCore
                )
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(18.dp))
            }
        }
    }
}
