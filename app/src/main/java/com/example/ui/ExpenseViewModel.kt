package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Expense
import com.example.data.ExpenseRepository
import com.example.network.GeminiApiClient
import com.example.network.ParsedExpense
import com.example.network.RetrofitSyncClient
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppTab {
    Dashboard, Expenses, Analytics, CloudSync
}

sealed interface SyncStatus {
    object Idle : SyncStatus
    object Syncing : SyncStatus
    data class Success(val message: String) : SyncStatus
    data class Error(val message: String) : SyncStatus
}

@JsonClass(generateAdapter = true)
data class RemoteSyncRequest(
    val deviceId: String,
    val syncedAt: Long,
    val expenses: List<RemoteExpenseItem>
)

@JsonClass(generateAdapter = true)
data class RemoteExpenseItem(
    val title: String,
    val amount: Double,
    val category: String,
    val timestamp: Long,
    val description: String
)

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ExpenseRepository
    val allExpenses: StateFlow<List<Expense>>

    // Tabs
    private val _currentTab = MutableStateFlow(AppTab.Dashboard)
    val currentTab = _currentTab.asStateFlow()

    // Sync Settings
    private val _syncUrl = MutableStateFlow("https://httpbin.org/post")
    val syncUrl = _syncUrl.asStateFlow()

    private val _syncApiKey = MutableStateFlow("")
    val syncApiKey = _syncApiKey.asStateFlow()

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus = _syncStatus.asStateFlow()

    private val _syncLogs = MutableStateFlow<List<String>>(emptyList())
    val syncLogs = _syncLogs.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime = _lastSyncTime.asStateFlow()

    // AI Assist States
    private val _aiInsights = MutableStateFlow<String?>(null)
    val aiInsights = _aiInsights.asStateFlow()

    private val _isGeneratingInsights = MutableStateFlow(false)
    val isGeneratingInsights = _isGeneratingInsights.asStateFlow()

    private val _isParsingNote = MutableStateFlow(false)
    val isParsingNote = _isParsingNote.asStateFlow()

    private val _parsedExpenseResult = MutableStateFlow<ParsedExpense?>(null)
    val parsedExpenseResult = _parsedExpenseResult.asStateFlow()

    private val _naturalLanguageNote = MutableStateFlow("")
    val naturalLanguageNote = _naturalLanguageNote.asStateFlow()

    init {
        val expenseDao = AppDatabase.getDatabase(application).expenseDao()
        repository = ExpenseRepository(expenseDao)
        allExpenses = repository.allExpenses.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        addLog("Offline SQLite Database initialized successfully.")
    }

    fun setTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun setSyncUrl(url: String) {
        _syncUrl.value = url
    }

    fun setSyncApiKey(key: String) {
        _syncApiKey.value = key
    }

    fun setNaturalLanguageNote(note: String) {
        _naturalLanguageNote.value = note
    }

    fun clearParsedResult() {
        _parsedExpenseResult.value = null
    }

    private fun addLog(message: String) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val timestamp = sdf.format(Date())
        _syncLogs.value = _syncLogs.value + "[$timestamp] $message"
    }

    fun clearLogs() {
        _syncLogs.value = emptyList()
    }

    // --- CRUD DATABASE OPERATIONS ---

    fun addExpense(title: String, amount: Double, category: String, timestamp: Long, description: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val expense = Expense(
                title = title,
                amount = amount,
                category = category,
                timestamp = timestamp,
                description = description,
                isSynced = false
            )
            repository.insert(expense)
            addLog("Created transaction: \"${title}\" ($${String.format("%.2f", amount)})")
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch(Dispatchers.IO) {
            // Reset sync status on edit
            repository.update(expense.copy(isSynced = false))
            addLog("Updated transaction \"${expense.title}\"")
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(expense)
            addLog("Deleted transaction \"${expense.title}\"")
        }
    }

    fun seedSampleData() {
        viewModelScope.launch(Dispatchers.IO) {
            // Seed a few diverse items to make the charts beautiful right off the bat!
            val now = System.currentTimeMillis()
            val dayMs = 24 * 60 * 60 * 1000L
            val samples = listOf(
                Expense(title = "Weekly Groceries", amount = 85.50, category = "Food", timestamp = now - dayMs * 1),
                Expense(title = "Monthly Power Bill", amount = 120.00, category = "Bills", timestamp = now - dayMs * 3),
                Expense(title = "Movie Tickets & Popcorn", amount = 34.20, category = "Entertainment", timestamp = now - dayMs * 2),
                Expense(title = "Train Ride", amount = 12.50, category = "Travel", timestamp = now - dayMs * 4),
                Expense(title = "Running Shoes", amount = 95.00, category = "Shopping", timestamp = now - dayMs * 1),
                Expense(title = "Indie Coffee Shop", amount = 6.75, category = "Food", timestamp = now - dayMs * 0),
                Expense(title = "Netflix renewal", amount = 15.99, category = "Bills", timestamp = now - dayMs * 5)
            )
            samples.forEach { repository.insert(it) }
            addLog("Seeded standard placeholder expenses for test visualisations.")
        }
    }

    fun clearAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAll()
            addLog("Purged all local records.")
        }
    }

    // --- CLOUD SYNC OPERATION ---

    fun performCloudSync() {
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.Syncing
            clearLogs()
            addLog("Sync handshake initiated...")

            // Retrieve unsynced items
            val unsyncedList = withContext(Dispatchers.IO) {
                repository.getUnsyncedExpenses()
            }

            val targetList = if (unsyncedList.isEmpty()) {
                addLog("No new unsynced changes. Gathering backup of modern records to keep cloud up-to-date...")
                // Fallback to sending all expenses to guarantee they sync and showcase the network call!
                allExpenses.value
            } else {
                addLog("Found ${unsyncedList.size} new local changes to upload.")
                unsyncedList
            }

            if (targetList.isEmpty()) {
                addLog("Database is empty. Nothing to sync.")
                _syncStatus.value = SyncStatus.Success("Database is empty; sync completed with 0 items.")
                return@launch
            }

            // Create payload
            val remoteItems = targetList.map { 
                RemoteExpenseItem(
                    title = it.title,
                    amount = it.amount,
                    category = it.category,
                    timestamp = it.timestamp,
                    description = it.description
                )
            }
            val payload = RemoteSyncRequest(
                deviceId = "Android_Device_Client",
                syncedAt = System.currentTimeMillis(),
                expenses = remoteItems
            )

            // Serialize
            val moshi = RetrofitSyncClient.getMoshi()
            val adapter = moshi.adapter(RemoteSyncRequest::class.java)
            val jsonPayload = adapter.toJson(payload)

            addLog("Uploading ${targetList.size} items to endpoint: ${_syncUrl.value}")

            // Headers
            val headers = mutableMapOf<String, String>()
            headers["Content-Type"] = "application/json"
            if (_syncApiKey.value.isNotEmpty()) {
                headers["Authorization"] = "Bearer ${_syncApiKey.value}"
                addLog("Added secure API key header payload.")
            }

            val requestBody = jsonPayload.toRequestBody("application/json".toMediaTypeOrNull())

            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitSyncClient.instance.syncExpenses(
                        url = _syncUrl.value,
                        headers = headers,
                        body = requestBody
                    )
                }

                if (response.isSuccessful) {
                    addLog("Cloud Sync Server responded: HTTP ${response.code()} OK")
                    val bodyString = withContext(Dispatchers.IO) { response.body()?.string() }
                    
                    // Log a truncated snippet of the network response body to show it literally uploaded
                    val bodySnippet = if (bodyString != null && bodyString.length > 200) {
                        bodyString.take(200) + "..."
                    } else {
                        bodyString ?: "Empty response payload"
                    }
                    addLog("Remote body response:\n$bodySnippet")

                    // Mark synced in local database
                    val targetIds = targetList.map { it.id }.filter { it != 0L }
                    if (targetIds.isNotEmpty()) {
                        withContext(Dispatchers.IO) {
                            repository.markAsSynced(targetIds)
                        }
                        addLog("Marked ${targetIds.size} records as Synced in offline storage.")
                    }

                    _lastSyncTime.value = System.currentTimeMillis()
                    _syncStatus.value = SyncStatus.Success("Successfully synchronized ${targetList.size} records with Cloud.")
                } else {
                    val errorBody = withContext(Dispatchers.IO) { response.errorBody()?.string() } ?: "Unknown remote error"
                    addLog("Cloud rejected request: HTTP ${response.code()}\n$errorBody")
                    _syncStatus.value = SyncStatus.Error("Failed with state ${response.code()}: $errorBody")
                }
            } catch (e: Exception) {
                addLog("Transport layer error: ${e.message}")
                Log.e("ExpenseViewModel", "Sync exception: ${e.message}", e)
                _syncStatus.value = SyncStatus.Error(e.message ?: "Failed due to unresolved network exception.")
            }
        }
    }

    // --- GEMINI INTELLIGENT AI FEATS ---

    fun generateAiInsights() {
        viewModelScope.launch {
            _isGeneratingInsights.value = true
            _aiInsights.value = null
            addLog("Connecting to Gemini Personal Financial Assistant...")
            try {
                val expensesList = allExpenses.value
                val result = GeminiApiClient.generateInsights(expensesList)
                _aiInsights.value = result
                addLog("Personalized AI Spending insights calculated successfully.")
            } catch (e: Exception) {
                _aiInsights.value = "Failed to calculate spending trends: ${e.localizedMessage}"
                addLog("Gemini analysis error: ${e.message}")
            } finally {
                _isGeneratingInsights.value = false
            }
        }
    }

    fun parseSmartNoteText() {
        val note = _naturalLanguageNote.value.trim()
        if (note.isEmpty()) return

        viewModelScope.launch {
            _isParsingNote.value = true
            _parsedExpenseResult.value = null
            addLog("Sending conversational note to Gemini transaction parser...")
            try {
                val parsed = GeminiApiClient.parseNaturalLanguageExpense(note)
                if (parsed != null && parsed.amount > 0) {
                    _parsedExpenseResult.value = parsed
                    addLog("Successfully extracted transaction: \"${parsed.title}\" -> $${String.format("%.2f", parsed.amount)}")
                } else {
                    addLog("Gemini couldn't confidently parse transaction details. Try being more specific, e.g. 'Coffee for 5 dollars' or 'Subway sandwich $12'.")
                }
            } catch (e: Exception) {
                addLog("Cognitive parsing failed: ${e.message}")
            } finally {
                _isParsingNote.value = false
            }
        }
    }
}
