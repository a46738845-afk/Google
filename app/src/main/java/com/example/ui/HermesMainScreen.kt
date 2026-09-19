package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.ActivityLogScreen
import com.example.ui.screens.CameraCaptureScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.MemoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.CyanCore
import com.example.ui.theme.DeepSpace
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted

@Composable
fun HermesMainScreen(
    viewModel: HermesViewModel = viewModel()
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val isCameraOpen by viewModel.isCameraOpen.collectAsState()

    if (isCameraOpen) {
        CameraCaptureScreen(
            onImageCaptured = { base64 ->
                viewModel.onImageCaptured(base64)
            },
            onClose = {
                viewModel.closeCamera()
            }
        )
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceDark,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == MainTab.CHAT,
                    onClick = { viewModel.setTab(MainTab.CHAT) },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Terminal") },
                    label = { Text("Terminal", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DeepSpace,
                        selectedTextColor = CyanCore,
                        indicatorColor = CyanCore,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    )
                )

                NavigationBarItem(
                    selected = currentTab == MainTab.LOGS,
                    onClick = { viewModel.setTab(MainTab.LOGS) },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Activity") },
                    label = { Text("Activity", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DeepSpace,
                        selectedTextColor = CyanCore,
                        indicatorColor = CyanCore,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    )
                )

                NavigationBarItem(
                    selected = currentTab == MainTab.MEMORY,
                    onClick = { viewModel.setTab(MainTab.MEMORY) },
                    icon = { Icon(Icons.Default.Info, contentDescription = "Memory") },
                    label = { Text("Memory", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DeepSpace,
                        selectedTextColor = CyanCore,
                        indicatorColor = CyanCore,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    )
                )

                NavigationBarItem(
                    selected = currentTab == MainTab.TOOLS,
                    onClick = { viewModel.setTab(MainTab.TOOLS) },
                    icon = { Icon(Icons.Default.Build, contentDescription = "Tools") },
                    label = { Text("Tools", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DeepSpace,
                        selectedTextColor = CyanCore,
                        indicatorColor = CyanCore,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    )
                )

                NavigationBarItem(
                    selected = currentTab == MainTab.SETTINGS,
                    onClick = { viewModel.setTab(MainTab.SETTINGS) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DeepSpace,
                        selectedTextColor = CyanCore,
                        indicatorColor = CyanCore,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepSpace)
                .padding(innerPadding)
        ) {
            when (currentTab) {
                MainTab.CHAT -> ChatScreen(viewModel = viewModel)
                MainTab.LOGS -> ActivityLogScreen(repository = viewModel.memoryRepository)
                MainTab.MEMORY -> MemoryScreen(repository = viewModel.memoryRepository)
                MainTab.TOOLS -> ToolsScreen(toolRegistry = viewModel.toolRegistry, repository = viewModel.memoryRepository)
                MainTab.SETTINGS -> SettingsScreen(prefs = viewModel.securePreferences)
            }
        }
    }
}
