package com.example.pasajeapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.thresholdDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "threshold_preferences"
)

interface ThresholdStore {
    val savedThreshold: Flow<Long?>
    suspend fun saveThreshold(threshold: Long)
}

class ThresholdPreferences(context: Context) : ThresholdStore {
    private val dataStore = context.applicationContext.thresholdDataStore

    override val savedThreshold: Flow<Long?> = dataStore.data.map { preferences ->
        preferences[THRESHOLD_KEY]
    }

    override suspend fun saveThreshold(threshold: Long) {
        dataStore.edit { preferences ->
            preferences[THRESHOLD_KEY] = threshold
        }
    }

    private companion object {
        val THRESHOLD_KEY = longPreferencesKey("threshold_amount")
    }
}
