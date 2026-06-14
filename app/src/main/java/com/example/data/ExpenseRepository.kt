package com.example.data

import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val expenseDao: ExpenseDao) {
    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()

    suspend fun insert(expense: Expense) {
        expenseDao.insertExpense(expense)
    }

    suspend fun update(expense: Expense) {
        expenseDao.updateExpense(expense)
    }

    suspend fun delete(expense: Expense) {
        expenseDao.deleteExpense(expense)
    }

    suspend fun getUnsyncedExpenses(): List<Expense> {
        return expenseDao.getUnsyncedExpenses()
    }

    suspend fun markAsSynced(ids: List<Long>) {
        expenseDao.markAsSynced(ids)
    }

    suspend fun clearAll() {
        expenseDao.clearAllExpenses()
    }
}
