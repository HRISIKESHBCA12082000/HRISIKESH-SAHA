package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Expense
import com.example.ui.ExpenseViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun AnalyticsScreen(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    val expenses by viewModel.allExpenses.collectAsState()
    val aiInsights by viewModel.aiInsights.collectAsState()
    val isGeneratingInsights by viewModel.isGeneratingInsights.collectAsState()

    // Calculate Category Aggregations
    val categoryTotals = remember(expenses) {
        val map = mutableMapOf(
            "Food" to 0.0,
            "Shopping" to 0.0,
            "Bills" to 0.0,
            "Entertainment" to 0.0,
            "Travel" to 0.0,
            "Others" to 0.0
        )
        expenses.forEach { exp ->
            val cat = when (exp.category) {
                "Food", "Shopping", "Bills", "Entertainment", "Travel" -> exp.category
                else -> "Others"
            }
            map[cat] = (map[cat] ?: 0.0) + exp.amount
        }
        map.filterValues { it > 0.0 }
    }

    val totalExpensesSum = remember(categoryTotals) {
        categoryTotals.values.sum()
    }

    // Weekly spending aggregation for 7 days
    val weeklySpending = remember(expenses) {
        val calendar = Calendar.getInstance()
        val daysList = mutableListOf<Pair<String, Double>>() // Day Label to Amount
        
        // Loop for the past 7 days starting from today backwards
        for (i in 6 downTo 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val dayStart = cal.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            val dayEnd = dayStart + (24 * 60 * 60 * 1000L)
            
            val dayLabel = SimpleDateFormat("EEE", Locale.getDefault()).format(cal.time)
            val daySpend = expenses.filter { it.timestamp in dayStart until dayEnd }.sumOf { it.amount }
            
            daysList.add(dayLabel to daySpend)
        }
        daysList
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("analytics_screen")
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- SCREEN TITLE ---
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Spending Analytics",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Visual dashboard of allocation and trends",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )
            }
        }

        // --- VISUALIZATION: DONUT PIE CHART ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pie_chart_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Category Allocation",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (totalExpensesSum == 0.0) {
                        EmptyStateChart()
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            // Donut Ring Canvas
                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .testTag("alloc_pie_chart"),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    var currentStartAngle = -90f
                                    categoryTotals.forEach { (cat, amount) ->
                                        val sweepAngle = ((amount / totalExpensesSum) * 360f).toFloat()
                                        val color = getCategoryColor(cat)
                                        
                                        drawArc(
                                            color = color,
                                            startAngle = currentStartAngle,
                                            sweepAngle = sweepAngle,
                                            useCenter = false,
                                            style = Stroke(width = 32f),
                                            size = Size(size.width - 32f, size.height - 32f),
                                            topLeft = Offset(16f, 16f)
                                        )
                                        currentStartAngle += sweepAngle
                                    }
                                }

                                // Center Label
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$${String.format("%.0f", totalExpensesSum)}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Total",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }

                            // Legends column
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                categoryTotals.forEach { (cat, amount) ->
                                    val percentage = (amount / totalExpensesSum) * 100
                                    LegendRowItem(category = cat, percentage = percentage, amount = amount)
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- VISUALIZATION: BAR CHART FOR THE WEEK ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bar_chart_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Last 7 Days Spending",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    val maxAmount = weeklySpending.maxOf { it.second }
                    val scaleMax = if (maxAmount == 0.0) 10.0 else maxAmount

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .testTag("weekly_spending_bar_chart"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        weeklySpending.forEach { (day, amount) ->
                            val scaleFraction = (amount / scaleMax).toFloat()

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                if (amount > 0.0) {
                                    Text(
                                        text = "$${amount.toInt()}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Emerald80
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }

                                // Interactive-looking Pillar bar
                                Box(
                                    modifier = Modifier
                                        .width(18.dp)
                                        .fillMaxHeight(scaleFraction.coerceIn(0.04f, 1f))
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                colors = if (amount > 0.0) {
                                                    listOf(Emerald80, GreenSecondary)
                                                } else {
                                                    listOf(CardCharcoalSf, CardCharcoalSf)
                                                }
                                            )
                                        )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = day,
                                    fontSize = 11.sp,
                                    color = if (amount > 0.0) Emerald80 else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    fontWeight = if (amount > 0.0) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- GEMINI COGNITIVE FINANCIAL COACH INSIGHTS PANEL ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_insights_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Coaching Insights",
                                tint = Emerald80
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AI Financial Guide",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Button(
                            onClick = { viewModel.generateAiInsights() },
                            enabled = !isGeneratingInsights,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Emerald80.copy(alpha = 0.15f),
                                contentColor = Emerald80
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("generate_insights_button")
                        ) {
                            Text("Analyze", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    AnimatedVisibility(
                        visible = isGeneratingInsights,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Emerald80)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "AI advisor is analyzing your statements...",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }

                    if (aiInsights != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .padding(14.dp)
                        ) {
                            Text(
                                text = aiInsights!!,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 19.sp,
                                modifier = Modifier.testTag("ai_insights_box")
                            )
                        }
                    } else if (!isGeneratingInsights) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                .padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Insights,
                                    contentDescription = "Insights awaiting",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Ready to audit. Click 'Analyze' to generate advice.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
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
fun EmptyStateChart() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.BubbleChart,
                contentDescription = "Chart Empty",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "No records to display allocations.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun LegendRowItem(category: String, percentage: Double, amount: Double) {
    val color = getCategoryColor(category)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = category,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = "${percentage.toInt()}% ($${amount.toInt()})",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

fun getCategoryColor(category: String): Color {
    return when (category.lowercase(Locale.getDefault())) {
        "food" -> ColorFood
        "shopping" -> ColorShopping
        "bills" -> ColorBills
        "entertainment" -> ColorEntertainment
        "travel" -> ColorTravel
        else -> ColorOthers
    }
}
