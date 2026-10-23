package com.keuangan.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "keuangan_preferences")

class UserPreferences(private val context: Context) {

    companion object {
        val SCRIPT_URL_KEY = stringPreferencesKey("google_script_url")
        val CURRENT_USER_KEY = stringPreferencesKey("current_user_profile")
        const val DEFAULT_USER = "Suami"
    }

    val scriptUrlFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SCRIPT_URL_KEY] ?: ""
    }

    val currentUserFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[CURRENT_USER_KEY] ?: DEFAULT_USER
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
}
