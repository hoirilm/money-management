package com.keuangan.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.keuangan.app.data.model.CategoryItem
import com.keuangan.app.data.model.FinanceConstants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "keuangan_preferences")

class UserPreferences(private val context: Context) {

    private val gson = Gson()

    companion object {
        val SCRIPT_URL_KEY = stringPreferencesKey("google_script_url")
        val CURRENT_USER_KEY = stringPreferencesKey("current_user_profile")
        val CUSTOM_EXPENSE_CATEGORIES_KEY = stringPreferencesKey("custom_expense_categories")
        val CUSTOM_INCOME_CATEGORIES_KEY = stringPreferencesKey("custom_income_categories")
        val CUSTOM_ACCOUNTS_KEY = stringPreferencesKey("custom_payment_accounts")

        const val DEFAULT_USER = "Suami"
    }

    val scriptUrlFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SCRIPT_URL_KEY] ?: ""
    }

    val currentUserFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[CURRENT_USER_KEY] ?: DEFAULT_USER
    }

    val expenseCategoriesFlow: Flow<List<CategoryItem>> = context.dataStore.data.map { preferences ->
        val json = preferences[CUSTOM_EXPENSE_CATEGORIES_KEY]
        if (!json.isNullOrBlank()) {
            try {
                val type = object : TypeToken<List<CategoryItem>>() {}.type
                gson.fromJson<List<CategoryItem>>(json, type) ?: FinanceConstants.expenseCategories
            } catch (e: Exception) {
                FinanceConstants.expenseCategories
            }
        } else {
            FinanceConstants.expenseCategories
        }
    }

    val incomeCategoriesFlow: Flow<List<CategoryItem>> = context.dataStore.data.map { preferences ->
        val json = preferences[CUSTOM_INCOME_CATEGORIES_KEY]
        if (!json.isNullOrBlank()) {
            try {
                val type = object : TypeToken<List<CategoryItem>>() {}.type
                gson.fromJson<List<CategoryItem>>(json, type) ?: FinanceConstants.incomeCategories
            } catch (e: Exception) {
                FinanceConstants.incomeCategories
            }
        } else {
            FinanceConstants.incomeCategories
        }
    }

    val accountsFlow: Flow<List<String>> = context.dataStore.data.map { preferences ->
        val json = preferences[CUSTOM_ACCOUNTS_KEY]
        if (!json.isNullOrBlank()) {
            try {
                val type = object : TypeToken<List<String>>() {}.type
                gson.fromJson<List<String>>(json, type) ?: FinanceConstants.paymentAccounts
            } catch (e: Exception) {
                FinanceConstants.paymentAccounts
            }
        } else {
            FinanceConstants.paymentAccounts
        }
    }

    suspend fun saveScriptUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[SCRIPT_URL_KEY] = url.trim()
        }
    }

    suspend fun saveCurrentUser(name: String) {
        context.dataStore.edit { preferences ->
            preferences[CURRENT_USER_KEY] = name.trim()
        }
    }

    suspend fun saveExpenseCategories(list: List<CategoryItem>) {
        context.dataStore.edit { preferences ->
            preferences[CUSTOM_EXPENSE_CATEGORIES_KEY] = gson.toJson(list)
        }
    }

    suspend fun saveIncomeCategories(list: List<CategoryItem>) {
        context.dataStore.edit { preferences ->
            preferences[CUSTOM_INCOME_CATEGORIES_KEY] = gson.toJson(list)
        }
    }

    suspend fun saveAccounts(list: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[CUSTOM_ACCOUNTS_KEY] = gson.toJson(list)
        }
    }
}
