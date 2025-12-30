package de.timbornemann.simplesipscheduler.presentation

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.timbornemann.simplesipscheduler.data.database.DrinkEntry
import de.timbornemann.simplesipscheduler.data.repository.DrinkRepository
import de.timbornemann.simplesipscheduler.data.repository.SettingsRepository
import de.timbornemann.simplesipscheduler.receiver.ReminderManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class MainViewModel(
    private val application: Application,
    private val drinkRepository: DrinkRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val dailyTarget = settingsRepository.dailyTarget
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULT_TARGET)

    val todayProgress = drinkRepository.getTodayProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todayEntries = drinkRepository.getTodayEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val buttonConfig = settingsRepository.buttonConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf(100, 250, 500))

    val reminderEnabled = settingsRepository.reminderEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val weekStats = drinkRepository.getWeekStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthStats = drinkRepository.getMonthStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reminderInterval = settingsRepository.reminderInterval
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULT_INTERVAL_MINUTES)

    val quietHoursStart = settingsRepository.quietHoursStart
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULT_QUIET_START)

    val quietHoursEnd = settingsRepository.quietHoursEnd
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULT_QUIET_END)

    fun addDrink(amount: Int) {
        viewModelScope.launch {
            drinkRepository.addDrink(amount)
            if (reminderEnabled.value) {
                // Reschedule with current interval
                val interval = reminderInterval.value.toLong() * 60 * 1000L
                ReminderManager.scheduleReminder(application, interval)
            }
        }
    }
    
    fun setReminderInterval(minutes: Int) {
         viewModelScope.launch {
            settingsRepository.setReminderInterval(minutes)
             if (reminderEnabled.value) {
                val interval = minutes.toLong() * 60 * 1000L
                ReminderManager.scheduleReminder(application, interval)
            }
        }
    }

    fun setQuietHours(start: Int, end: Int) {
         viewModelScope.launch {
            settingsRepository.setQuietHours(start, end)
        }
    }
    
    fun updateButtonConfig(config: List<Int>) {
         viewModelScope.launch {
            settingsRepository.updateButtonConfig(config)
        }
    }

    fun deleteEntry(entry: DrinkEntry) {
        viewModelScope.launch {
            drinkRepository.deleteEntry(entry)
        }
    }

    fun updateEntry(entry: DrinkEntry, newAmount: Int) {
        viewModelScope.launch {
            drinkRepository.updateEntry(entry.copy(amountMl = newAmount))
        }
    }

    fun updateDailyTarget(target: Int) {
        viewModelScope.launch {
            settingsRepository.setDailyTarget(target)
        }
    }

    fun toggleReminder(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.setReminderEnabled(enabled)
                if (enabled) {
                    // Get the interval from the repository to ensure we have the latest value
                    val intervalMinutes = settingsRepository.reminderInterval.first()
                    val interval = intervalMinutes.toLong() * 60 * 1000L
                    ReminderManager.scheduleReminder(application, interval)
                } else {
                    ReminderManager.cancelReminder(application)
                }
            } catch (e: Exception) {
                // If scheduling fails, disable the reminder setting
                settingsRepository.setReminderEnabled(false)
            }
        }
    }
}

class MainViewModelFactory(
    private val application: Application,
    private val drinkRepository: DrinkRepository,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application, drinkRepository, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
