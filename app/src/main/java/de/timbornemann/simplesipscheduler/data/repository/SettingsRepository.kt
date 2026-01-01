package de.timbornemann.simplesipscheduler.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    
    companion object {
        val DAILY_TARGET_KEY = intPreferencesKey("daily_target")
        val BUTTON_CONFIG_KEY = stringPreferencesKey("button_config")
        val REMINDER_ENABLED_KEY = booleanPreferencesKey("reminder_enabled")
        val REMINDER_INTERVAL_KEY = intPreferencesKey("reminder_interval")
        val REMINDER_MODE_KEY = stringPreferencesKey("reminder_mode")
        val QUIET_HOURS_START_KEY = intPreferencesKey("quiet_hours_start")
        val QUIET_HOURS_END_KEY = intPreferencesKey("quiet_hours_end")
        val NEXT_REMINDER_AT_KEY = longPreferencesKey("next_reminder_at")

        const val DEFAULT_TARGET = 2500
        const val DEFAULT_BUTTONS = "100,250,500"
        const val DEFAULT_INTERVAL_MINUTES = 120 // 2 hours
        const val DEFAULT_QUIET_START = 22 // 10 PM
        const val DEFAULT_QUIET_END = 7   // 7 AM
        val DEFAULT_REMINDER_MODE = ReminderMode.ALWAYS
    }

    val dailyTarget: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[DAILY_TARGET_KEY] ?: DEFAULT_TARGET
        }
        
    val buttonConfig: Flow<List<Int>> = context.dataStore.data
        .map { preferences ->
            (preferences[BUTTON_CONFIG_KEY] ?: DEFAULT_BUTTONS)
                .split(",")
                .mapNotNull { it.toIntOrNull() }
        }

    val reminderEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[REMINDER_ENABLED_KEY] ?: false
        }

    val reminderInterval: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[REMINDER_INTERVAL_KEY] ?: DEFAULT_INTERVAL_MINUTES
        }

    val quietHoursStart: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[QUIET_HOURS_START_KEY] ?: DEFAULT_QUIET_START
        }

    val quietHoursEnd: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[QUIET_HOURS_END_KEY] ?: DEFAULT_QUIET_END
        }

    val reminderMode: Flow<ReminderMode> = context.dataStore.data
        .map { preferences ->
            val modeString = preferences[REMINDER_MODE_KEY] ?: DEFAULT_REMINDER_MODE.name
            try {
                ReminderMode.valueOf(modeString)
            } catch (e: Exception) {
                DEFAULT_REMINDER_MODE
            }
        }

    val nextReminderAt: Flow<Long?> = context.dataStore.data
        .map { preferences ->
            preferences[NEXT_REMINDER_AT_KEY]
        }

    suspend fun setDailyTarget(target: Int) {
        context.dataStore.edit { preferences ->
            preferences[DAILY_TARGET_KEY] = target
        }
    }

    suspend fun updateButtonConfig(config: List<Int>) {
        val configString = config.joinToString(",")
        context.dataStore.edit { preferences ->
            preferences[BUTTON_CONFIG_KEY] = configString
        }
    }

    suspend fun setReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[REMINDER_ENABLED_KEY] = enabled
        }
    }

    suspend fun setReminderInterval(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[REMINDER_INTERVAL_KEY] = minutes
        }
    }

    suspend fun setQuietHours(start: Int, end: Int) {
        context.dataStore.edit { preferences ->
            preferences[QUIET_HOURS_START_KEY] = start
            preferences[QUIET_HOURS_END_KEY] = end
        }
    }

    suspend fun setReminderMode(mode: ReminderMode) {
        context.dataStore.edit { preferences ->
            preferences[REMINDER_MODE_KEY] = mode.name
        }
    }

    suspend fun setNextReminderAt(epochMillis: Long?) {
        context.dataStore.edit { preferences ->
            if (epochMillis == null) {
                preferences.remove(NEXT_REMINDER_AT_KEY)
            } else {
                preferences[NEXT_REMINDER_AT_KEY] = epochMillis
            }
        }
    }
}

