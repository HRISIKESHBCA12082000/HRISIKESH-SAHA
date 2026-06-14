package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppTab
import com.example.ui.ExpenseViewModel
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.CloudSyncScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExpensesScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: ExpenseViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val currentTab by viewModel.currentTab.collectAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Text(
                                    text = "SPENDSMART",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 2.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background
                            ),
                            modifier = Modifier.testTag("app_top_bar")
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp,
                            modifier = Modifier.testTag("bottom_nav_bar")
                        ) {
                            // Tab 1: Dashboard
                            NavigationBarItem(
                                selected = currentTab == AppTab.Dashboard,
                                onClick = { viewModel.setTab(AppTab.Dashboard) },
                                icon = {
                                    Icon(
                                        imageVector = if (currentTab == AppTab.Dashboard) Icons.Filled.Dashboard else Icons.Outlined.Dashboard,
                                        contentDescription = "Dashboard"
                                    )
                                },
                                label = { Text("Dashboard", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                modifier = Modifier.testTag("nav_dashboard_tab")
                            )

                            // Tab 2: Ledger/Expenses
                            NavigationBarItem(
                                selected = currentTab == AppTab.Expenses,
                                onClick = { viewModel.setTab(AppTab.Expenses) },
                                icon = {
                                    Icon(
                                        imageVector = if (currentTab == AppTab.Expenses) Icons.Filled.ReceiptLong else Icons.Outlined.ReceiptLong,
                                        contentDescription = "Ledger"
                                    )
                                },
                                label = { Text("Ledger", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                modifier = Modifier.testTag("nav_expenses_tab")
                            )

                            // Tab 3: Analytics
                            NavigationBarItem(
                                selected = currentTab == AppTab.Analytics,
                                onClick = { viewModel.setTab(AppTab.Analytics) },
                                icon = {
                                    Icon(
                                        imageVector = if (currentTab == AppTab.Analytics) Icons.Filled.Analytics else Icons.Outlined.Analytics,
                                        contentDescription = "Analytics"
                                    )
                                },
                                label = { Text("Analytics", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                modifier = Modifier.testTag("nav_analytics_tab")
                            )

                            // Tab 4: Cloud Sync
                            NavigationBarItem(
                                selected = currentTab == AppTab.CloudSync,
                                onClick = { viewModel.setTab(AppTab.CloudSync) },
                                icon = {
                                    Icon(
                                        imageVector = if (currentTab == AppTab.CloudSync) Icons.Filled.CloudQueue else Icons.Outlined.CloudQueue,
                                        contentDescription = "CloudSync"
                                    )
                                },
                                label = { Text("Cloud Sync", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                modifier = Modifier.testTag("nav_sync_tab")
                            )
                        }
                    },
                    contentWindowInsets = WindowInsets.safeDrawing // support edge-to-edge system bars seamlessly!
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentTab) {
                            AppTab.Dashboard -> DashboardScreen(viewModel = viewModel)
                            AppTab.Expenses -> ExpensesScreen(viewModel = viewModel)
                            AppTab.Analytics -> AnalyticsScreen(viewModel = viewModel)
                            AppTab.CloudSync -> CloudSyncScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
