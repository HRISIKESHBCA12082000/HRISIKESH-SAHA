package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.Expense
import com.example.ui.ExpenseViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    val expenses by viewModel.allExpenses.collectAsState()
    val isParsingNote by viewModel.isParsingNote.collectAsState()
    val parsedResult by viewModel.parsedExpenseResult.collectAsState()
    val naturalLanguageNote by viewModel.naturalLanguageNote.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterCategory by remember { mutableStateOf("All") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<Expense?>(null) }

    // Form states for manual adding/editing
    var formTitle by remember { mutableStateOf("") }
    var formAmount by remember { mutableStateOf("") }
    var formCategory by remember { mutableStateOf("Food") }
    var formDescription by remember { mutableStateOf("") }
    var formDate by remember { mutableStateOf(System.currentTimeMillis()) }

    val categories = listOf("All", "Food", "Shopping", "Bills", "Entertainment", "Travel", "Others")
    val formCategories = listOf("Food", "Shopping", "Bills", "Entertainment", "Travel", "Others")

    // Filtered lists
    val filteredExpenses = remember(expenses, searchQuery, selectedFilterCategory) {
        expenses.filter { expense ->
            val matchQuery = expense.title.contains(searchQuery, ignoreCase = true) || 
                             expense.description.contains(searchQuery, ignoreCase = true)
            val matchCategory = selectedFilterCategory == "All" || expense.category == selectedFilterCategory
            matchQuery && matchCategory
        }
    }

    val focusManager = LocalFocusManager.current

    // Detect if we got a parsed output from Gemini
    LaunchedEffect(parsedResult) {
        parsedResult?.let { result ->
            formTitle = result.title
            formAmount = if (result.amount > 0) String.format(Locale.US, "%.2f", result.amount) else ""
            formCategory = result.category
            viewModel.clearParsedResult()
            showAddDialog = true // open dialog with prepopulated values!
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("expenses_screen")
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- SCREEN TITLE & MANUAL ADD TRIGGERS ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Transaction Ledger",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${filteredExpenses.size} records found",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )
            }

            IconButton(
                onClick = {
                    formTitle = ""
                    formAmount = ""
                    formCategory = "Food"
                    formDescription = ""
                    showAddDialog = true
                },
                modifier = Modifier
                    .background(Emerald80, RoundedCornerShape(12.dp))
                    .testTag("fab_add_expense"),
                colors = IconButtonDefaults.iconButtonColors(contentColor = DeepCharcoalBg)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add manually")
            }
        }

        // --- GEMINI COGNITIVE NOTE PARSER PANEL ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Gemini AI Parser",
                        tint = Emerald80,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Conversational AI Logger",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Emerald80
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = naturalLanguageNote,
                        onValueChange = { viewModel.setNaturalLanguageNote(it) },
                        placeholder = { Text("E.g., 15 dollars on burgers...", fontSize = 13.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("ai_note_input"),
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.parseSmartNoteText()
                        },
                        enabled = !isParsingNote && naturalLanguageNote.isNotEmpty(),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("ai_parse_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Emerald80,
                            contentColor = DeepCharcoalBg
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        if (isParsingNote) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = DeepCharcoalBg, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Psychology, contentDescription = "AI Parse", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AI Log", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- FILTER AND SEARCH CONTROLS ---
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search title, notes...", fontSize = 14.sp) },
            prefix = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_bar"),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Emerald80,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
            )
        )

        // Category Horizontal Selection Filter Row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                val isSelected = selectedFilterCategory == category
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilterCategory = category },
                    label = { Text(category, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Emerald80,
                        selectedLabelColor = DeepCharcoalBg
                    )
                )
            }
        }

        // --- LEDGER ENTRIES LIST ---
        if (filteredExpenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = "Empty Search",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No matching records found.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("expenses_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredExpenses, key = { it.id }) { expense ->
                    ExpenseItemCard(
                        expense = expense,
                        onDeleteClick = { viewModel.deleteExpense(expense) },
                        onEditClick = {
                            showEditDialog = expense
                            formTitle = expense.title
                            formAmount = String.format(Locale.US, "%.2f", expense.amount)
                            formCategory = expense.category
                            formDescription = expense.description
                            formDate = expense.timestamp
                        }
                    )
                }
            }
        }
    }

    // --- DIALOG: ADD TRANSACTION ---
    if (showAddDialog) {
        Dialog(onDismissRequest = { showAddDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Add Transaction",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = formTitle,
                        onValueChange = { formTitle = it },
                        label = { Text("Title / Vendor") },
                        modifier = Modifier.fillMaxWidth().testTag("add_title_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = formAmount,
                        onValueChange = { formAmount = it },
                        label = { Text("Amount ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().testTag("add_amount_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    val formattedDateString = remember(formDate) {
                        try {
                            SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date(formDate))
                        } catch (e: Exception) {
                            "Select Date"
                        }
                    }

                    val addContext = androidx.compose.ui.platform.LocalContext.current
                    val addCalendar = java.util.Calendar.getInstance().apply { timeInMillis = formDate }
                    val addDatePicker = android.app.DatePickerDialog(
                        addContext,
                        { _, year, month, dayOfMonth ->
                            val selectedCalendar = java.util.Calendar.getInstance()
                            selectedCalendar.set(year, month, dayOfMonth)
                            formDate = selectedCalendar.timeInMillis
                        },
                        addCalendar.get(java.util.Calendar.YEAR),
                        addCalendar.get(java.util.Calendar.MONTH),
                        addCalendar.get(java.util.Calendar.DAY_OF_MONTH)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { addDatePicker.show() }
                    ) {
                        OutlinedTextField(
                            value = formattedDateString,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Transaction Date") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Select Date",
                                    tint = Emerald80
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledLeadingIconColor = Emerald80
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Category Selector Segment Chips
                    Column {
                        Text("Category", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            formCategories.take(3).forEach { cat ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (formCategory == cat) Emerald80 else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { formCategory = cat }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        cat, 
                                        color = if (formCategory == cat) DeepCharcoalBg else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 11.sp, 
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            formCategories.drop(3).forEach { cat ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (formCategory == cat) Emerald80 else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { formCategory = cat }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        cat, 
                                        color = if (formCategory == cat) DeepCharcoalBg else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 11.sp, 
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = formDescription,
                        onValueChange = { formDescription = it },
                        label = { Text("Optional Note") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showAddDialog = false }) {
                            Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val amountVal = formAmount.toDoubleOrNull() ?: 0.0
                                if (formTitle.isNotEmpty() && amountVal > 0) {
                                    viewModel.addExpense(
                                        title = formTitle,
                                        amount = amountVal,
                                        category = formCategory,
                                        timestamp = formDate,
                                        description = formDescription
                                    )
                                    showAddDialog = false
                                }
                             },
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald80, contentColor = DeepCharcoalBg),
                            modifier = Modifier.testTag("save_expense_button")
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }

    // --- DIALOG: EDIT TRANSACTION ---
    if (showEditDialog != null) {
        val targetExpense = showEditDialog!!
        Dialog(onDismissRequest = { showEditDialog = null }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Edit Transaction",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = formTitle,
                        onValueChange = { formTitle = it },
                        label = { Text("Title / Vendor") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = formAmount,
                        onValueChange = { formAmount = it },
                        label = { Text("Amount ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    val formattedDateString = remember(formDate) {
                        try {
                            SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date(formDate))
                        } catch (e: Exception) {
                            "Select Date"
                        }
                    }

                    val editContext = androidx.compose.ui.platform.LocalContext.current
                    val editCalendar = java.util.Calendar.getInstance().apply { timeInMillis = formDate }
                    val editDatePicker = android.app.DatePickerDialog(
                        editContext,
                        { _, year, month, dayOfMonth ->
                            val selectedCalendar = java.util.Calendar.getInstance()
                            selectedCalendar.set(year, month, dayOfMonth)
                            formDate = selectedCalendar.timeInMillis
                        },
                        editCalendar.get(java.util.Calendar.YEAR),
                        editCalendar.get(java.util.Calendar.MONTH),
                        editCalendar.get(java.util.Calendar.DAY_OF_MONTH)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editDatePicker.show() }
                    ) {
                        OutlinedTextField(
                            value = formattedDateString,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Transaction Date") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Select Date",
                                    tint = Emerald80
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledLeadingIconColor = Emerald80
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Category Selector
                    Column {
                        Text("Category", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            formCategories.take(3).forEach { cat ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (formCategory == cat) Emerald80 else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { formCategory = cat }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        cat, 
                                        color = if (formCategory == cat) DeepCharcoalBg else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 11.sp, 
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            formCategories.drop(3).forEach { cat ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (formCategory == cat) Emerald80 else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { formCategory = cat }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        cat, 
                                        color = if (formCategory == cat) DeepCharcoalBg else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 11.sp, 
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = formDescription,
                        onValueChange = { formDescription = it },
                        label = { Text("Note") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showEditDialog = null }) {
                            Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val amountVal = formAmount.toDoubleOrNull() ?: 0.0
                                if (formTitle.isNotEmpty() && amountVal > 0) {
                                    viewModel.updateExpense(
                                        targetExpense.copy(
                                            title = formTitle,
                                            amount = amountVal,
                                            category = formCategory,
                                            timestamp = formDate,
                                            description = formDescription
                                        )
                                    )
                                    showEditDialog = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald80, contentColor = DeepCharcoalBg)
                        ) {
                            Text("Save Changes")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseItemCard(
    expense: Expense,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val categoryColor = when (expense.category.lowercase(Locale.getDefault())) {
        "food" -> ColorFood
        "shopping" -> ColorShopping
        "bills" -> ColorBills
        "entertainment" -> ColorEntertainment
        "travel" -> ColorTravel
        else -> ColorOthers
    }

    val categoryIcon = when (expense.category.lowercase(Locale.getDefault())) {
        "food" -> Icons.Default.Restaurant
        "shopping" -> Icons.Default.ShoppingBag
        "bills" -> Icons.Default.ReceiptLong
        "entertainment" -> Icons.Default.TheaterComedy
        "travel" -> Icons.Default.DirectionsTransit
        else -> Icons.Default.Category
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Shape Icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(categoryColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = expense.category,
                        tint = categoryColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Detail Information
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = expense.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(expense.timestamp)),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                    if (expense.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = expense.description,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Price / Back up indicator & delete actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$${String.format("%.2f", expense.amount)}",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (expense.isSynced) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                            contentDescription = "Sync status",
                            tint = if (expense.isSynced) ColorSynced else ColorUnsynced,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = if (expense.isSynced) "Synced" else "Pending",
                            color = (if (expense.isSynced) ColorSynced else ColorUnsynced).copy(alpha = 0.8f),
                            fontSize = 10.sp
                        )
                    }
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        tint = ColorUnsynced.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
