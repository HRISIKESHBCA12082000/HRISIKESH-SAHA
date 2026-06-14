package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ExpenseViewModel
import com.example.ui.SyncStatus
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CloudSyncScreen(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    val expenses by viewModel.allExpenses.collectAsState()
    val syncUrl by viewModel.syncUrl.collectAsState()
    val syncApiKey by viewModel.syncApiKey.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val syncLogs by viewModel.syncLogs.collectAsState()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()

    val pendingSyncCount = remember(expenses) {
        expenses.count { !it.isSynced }
    }

    val logsListState = rememberLazyListState()

    // Automatically scroll console log to bottom when a new entry arrives
    LaunchedEffect(syncLogs.size) {
        if (syncLogs.isNotEmpty()) {
            logsListState.animateScrollToItem(syncLogs.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("cloud_sync_screen")
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- SCREEN TITLE ---
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Cloud Synchronization",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Backup and restore your ledger securely on a remote server",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontSize = 13.sp
            )
        }

        // --- OVERVIEW STATUS CARD ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Synchronization Status",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    val syncLabel = when (syncStatus) {
                        is SyncStatus.Idle -> "Connected & Ready"
                        is SyncStatus.Syncing -> "Synchronizing..."
                        is SyncStatus.Success -> "Backup Up-to-Date"
                        is SyncStatus.Error -> "Sync Failed"
                    }
                    val syncIcon = when (syncStatus) {
                        is SyncStatus.Idle -> Icons.Default.CloudQueue
                        is SyncStatus.Syncing -> Icons.Default.Sync
                        is SyncStatus.Success -> Icons.Default.CloudDone
                        is SyncStatus.Error -> Icons.Default.CloudOff
                    }
                    val syncColor = when (syncStatus) {
                        is SyncStatus.Idle -> Sage80
                        is SyncStatus.Syncing -> Emerald80
                        is SyncStatus.Success -> ColorSynced
                        is SyncStatus.Error -> ColorUnsynced
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(syncIcon, contentDescription = null, tint = syncColor, modifier = Modifier.size(18.dp))
                        Text(
                            text = syncLabel,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = syncColor,
                            modifier = Modifier.testTag("sync_status_label")
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (lastSyncTime != null) {
                            "Last Sync: " + SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault()).format(Date(lastSyncTime!!))
                        } else {
                            "Last Sync: Never"
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                // Cloud count badge
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (pendingSyncCount > 0) ColorUnsynced.copy(alpha = 0.15f) else ColorSynced.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$pendingSyncCount",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = if (pendingSyncCount > 0) ColorUnsynced else ColorSynced
                        )
                        Text(
                            text = "Unsynced",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (pendingSyncCount > 0) ColorUnsynced else ColorSynced
                        )
                    }
                }
            }
        }

        // --- BACKUP CONFIGURATION FIELDS ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Backup Node Settings",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Endpoint URL Input
                OutlinedTextField(
                    value = syncUrl,
                    onValueChange = { viewModel.setSyncUrl(it) },
                    label = { Text("Cloud Sync URL Endpoint", fontSize = 12.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sync_url_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Emerald80,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                    )
                )

                // Headers Secret key Token input
                OutlinedTextField(
                    value = syncApiKey,
                    onValueChange = { viewModel.setSyncApiKey(it) },
                    label = { Text("API Sync Security Key (Auth Token)", fontSize = 12.sp) },
                    placeholder = { Text("Optional authorization bearer key") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sync_apikey_input"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Emerald80,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Core Trigger Backup Action button
                Button(
                    onClick = { viewModel.performCloudSync() },
                    enabled = syncStatus !is SyncStatus.Syncing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("trigger_sync_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Emerald80,
                        contentColor = DeepCharcoalBg
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (syncStatus is SyncStatus.Syncing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = DeepCharcoalBg)
                    } else {
                        Icon(Icons.Default.CloudUpload, contentDescription = "Sync Now")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Force Remote Synchronisation", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- DEVELOPER LOGS CONSOLE DISPLAY ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sync Handshake Logs Console",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = "Clear Logs",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Emerald80,
                    modifier = Modifier
                        .clickable { viewModel.clearLogs() }
                        .padding(4.dp)
                )
            }

            // Developer Black and Green Monospace Console Screen overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF030704))
                    .testTag("sync_log_console")
                    .padding(12.dp)
            ) {
                if (syncLogs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Console stands ready. Logs print chronologically upon backup trigger.",
                            color = Color(0x665CDA9E),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        state = logsListState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(syncLogs) { logLine ->
                            Text(
                                text = logLine,
                                color = if (logLine.contains("error", ignoreCase = true) || logLine.contains("fail", ignoreCase = true)) {
                                    Color(0xFFFF5252)
                                } else if (logLine.contains("success", ignoreCase = true) || logLine.contains("OK", ignoreCase = true)) {
                                    Color(0xFF69F0AE)
                                } else {
                                    Color(0xFFB9F6CA)
                                },
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
