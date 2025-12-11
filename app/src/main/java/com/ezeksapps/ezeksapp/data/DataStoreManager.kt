package com.ezeksapps.ezeksapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

/* Manages user settings, TODO: actually make these configurable outside of SetupScreen by adding a SettingsScreen */
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class DataStoreManager(private val context: Context) {
    companion object {
        private val ONLINE_MODE_ENABLED = booleanPreferencesKey("online_mode_enabled")
        private val SETUP_COMPLETED = booleanPreferencesKey("setup_completed")
        private val USER_DEFAULT_LANG = stringPreferencesKey("user_default_lang")
    }

    suspend fun setUserDefaultLang(langCode: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_DEFAULT_LANG] = langCode
        }
    }

    val userDefaultLang: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            prefs[USER_DEFAULT_LANG] ?: "en"
        }

    suspend fun setOnlineModeEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[ONLINE_MODE_ENABLED] = enabled
        }
    }

    val onlineModeEnabled: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            prefs[ONLINE_MODE_ENABLED] ?: true
        }


    suspend fun setSetupCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[SETUP_COMPLETED] = completed
        }
    }

    val setupCompleted: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            prefs[SETUP_COMPLETED] ?: false
        }

    /* resetToDefaults() & exportPreferences() are for the future SettingsScreen implementation */

    suspend fun resetToDefaults() {
        context.dataStore.edit { prefs ->
            prefs.clear()
            prefs[ONLINE_MODE_ENABLED] = false
            prefs[USER_DEFAULT_LANG] = "en"
            prefs[SETUP_COMPLETED] = false
        }
    }

    suspend fun exportPreferences(): String {
        val prefs = context.dataStore.data.first()
        return prefs.asMap().entries.joinToString("\n") { (key, value) ->
            "${key.name}: $value"
        }
    }
}