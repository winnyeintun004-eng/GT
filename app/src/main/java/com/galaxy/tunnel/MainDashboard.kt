package com.galaxy.tunnel

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle

data class AppConfig(
    val isDarkMode: Boolean = true,
    val language: String = "EN",
    val fontScale: Float = 1.0f
)

val LocalAppConfig = compositionLocalOf { AppConfig() }
val LocalAppConfigUpdater = compositionLocalOf<(AppConfig) -> Unit> { {} }

@Composable
fun MainDashboard(viewModel: MainViewModel = viewModel()) {
    var isConnected by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importUrl by remember { mutableStateOf("") }
    
    val appConfig = LocalAppConfig.current
    val servers by viewModel.servers.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    if (showSettings) {
        SettingsDialog(onDismiss = { showSettings = false })
    }
    
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text(if (appConfig.language == "EN") "Import Config" else "ကွန်ဖစ်ထည့်ရန်") },
            text = {
                OutlinedTextField(
                    value = importUrl,
                    onValueChange = { importUrl = it },
                    label = { Text("Subscription Link / URL") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (importUrl.isNotBlank()) {
                            viewModel.fetchServers(importUrl)
                            showImportDialog = false
                            importUrl = ""
                        }
                    }
                ) {
                    Text(if (appConfig.language == "EN") "Import" else "ထည့်သွင်းမည်")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text(if (appConfig.language == "EN") "Cancel" else "ပယ်ဖျက်မည်")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "GALAXY TUNNEL",
                style = TextStyle(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.White, Color(0xFF5BC0EB))
                    )
                ),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { showImportDialog = true }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Import Config",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (appConfig.language == "EN") "Import Config" else "ကွန်ဖစ်ထည့်ရန်",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                IconButton(onClick = { showSettings = true }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(64.dp))

        // Center Control
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PremiumToggleSwitch(
                isConnected = isConnected,
                onToggle = { isConnected = it }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = if (isConnected) {
                    if (appConfig.language == "EN") "Status: Protected" else "အခြေအနေ - ကာကွယ်ထားသည်"
                } else {
                    if (appConfig.language == "EN") "Status: Unprotected" else "အခြေအနေ - မကာကွယ်ထားပါ"
                },
                color = if (isConnected) Color(0xFF5BC0EB) else Color.Gray,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(64.dp))

        // Bottom Section: Available Servers
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (appConfig.language == "EN") "Available Servers" else "ရနိုင်သော ဆာဗာများ",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            
            TextButton(
                onClick = { viewModel.fetchServers("https://raw.githubusercontent.com/Galaxy-Tunnel/ONE-AGENT/refs/heads/main/servers.txt") },
                enabled = !isRefreshing
            ) {
                Text(
                    text = if (isRefreshing) (if (appConfig.language == "EN") "Updating..." else "အဆင့်မြှင့်နေသည်...") 
                           else (if (appConfig.language == "EN") "Update Servers" else "ဆာဗာများကို အဆင့်မြှင့်မည်"),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (servers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (isRefreshing) {
                    CircularProgressIndicator()
                } else {
                    Text(
                        text = if (appConfig.language == "EN") "No servers found" else "ဆာဗာများ မတွေ့ပါ",
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(servers) { server ->
                    ServerItem(server = server)
                }
            }
        }
    }
}

@Composable
fun PremiumToggleSwitch(
    isConnected: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val thumbOffset by animateDpAsState(if (isConnected) 60.dp else 0.dp, label = "thumbOffset")
    val bgColor by animateColorAsState(if (isConnected) Color(0xFF5BC0EB).copy(alpha = 0.3f) else Color.DarkGray, label = "bgColor")
    val iconTint by animateColorAsState(if (isConnected) Color(0xFF5BC0EB) else Color.Gray, label = "iconTint")
    
    Box(
        modifier = Modifier
            .width(120.dp)
            .height(60.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(bgColor)
            .clickable { onToggle(!isConnected) }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .offset(x = thumbOffset)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun SettingsDialog(onDismiss: () -> Unit) {
    val appConfig = LocalAppConfig.current
    val updateConfig = LocalAppConfigUpdater.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                if (appConfig.language == "EN") "More Settings" else "ဆက်တင်များ",
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Theme Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(16.dp)
                ) {
                    Text(if (appConfig.language == "EN") "Theme Mode" else "အသွင်အပြင်", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (appConfig.language == "EN") "Dark Mode" else "အမည်းရောင်မုဒ်", color = MaterialTheme.colorScheme.onSurface)
                        Switch(
                            checked = appConfig.isDarkMode,
                            onCheckedChange = { updateConfig(appConfig.copy(isDarkMode = it)) }
                        )
                    }
                }
                
                // Language Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(16.dp)
                ) {
                    Text(if (appConfig.language == "EN") "Language" else "ဘာသာစကား", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { updateConfig(appConfig.copy(language = "EN")) },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("English", color = MaterialTheme.colorScheme.onSurface)
                        RadioButton(
                            selected = appConfig.language == "EN",
                            onClick = { updateConfig(appConfig.copy(language = "EN")) }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { updateConfig(appConfig.copy(language = "MM")) },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("မြန်မာ", color = MaterialTheme.colorScheme.onSurface)
                        RadioButton(
                            selected = appConfig.language == "MM",
                            onClick = { updateConfig(appConfig.copy(language = "MM")) }
                        )
                    }
                }

                // Font Size Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(16.dp)
                ) {
                    Text(if (appConfig.language == "EN") "Font Size" else "စာလုံးအရွယ်အစား", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Slider(
                        value = appConfig.fontScale,
                        onValueChange = { updateConfig(appConfig.copy(fontScale = it)) },
                        valueRange = 0.8f..1.5f,
                        steps = 5,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(if (appConfig.language == "EN") "Close" else "ပိတ်မည်", color = MaterialTheme.colorScheme.primary)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun ServerItem(server: VpnServer) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .clickable { /* Select Server */ }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = server.flag,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = server.name,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${server.ping} ms",
                color = Color.Gray,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (server.ping < 100) Color.Green else if (server.ping < 200) Color.Yellow else Color.Red)
            )
        }
    }
}
