package com.ldtoypad.remote.data.local.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "connection_settings")

class ConnectionSettingsStore(private val context: Context) {
    private val BASE_URL_KEY = stringPreferencesKey("base_url")
    private val REMEMBER_KEY = booleanPreferencesKey("remember")

    val baseUrl: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[BASE_URL_KEY]
    }

    val rememberConnection: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[REMEMBER_KEY] ?: true
    }

    suspend fun saveConnection(url: String, remember: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BASE_URL_KEY] = url
            preferences[REMEMBER_KEY] = remember
        }
    }

    suspend fun clearConnection() {
        context.dataStore.edit { preferences ->
            preferences.remove(BASE_URL_KEY)
            preferences.remove(REMEMBER_KEY)
        }
    }
}
