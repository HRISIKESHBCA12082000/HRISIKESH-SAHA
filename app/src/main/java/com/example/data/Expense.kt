package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val category: String, // e.g., Food, Shopping, Bills, Entertainment, Travel, Others
    val timestamp: Long,  // Epoch time in milliseconds
    val description: String = "",
    val isSynced: Boolean = false
)
