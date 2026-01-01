package de.timbornemann.simplesipscheduler.presentation

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.timbornemann.simplesipscheduler.data.database.DrinkEntry
import de.timbornemann.simplesipscheduler.data.repository.DrinkRepository
import de.timbornemann.simplesipscheduler.data.repository.SettingsRepository
import de.timbornemann.simplesipscheduler.receiver.ReminderManager
import de.timbornemann.simplesipscheduler.receiver.ReminderTimeCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import de.timbornemann.simplesipscheduler.data.database.DaySum
import java.time.LocalDateTime
import java.time.ZoneId

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

    val reminderMode = settingsRepository.reminderMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULT_REMINDER_MODE)

    val nextReminderAt = settingsRepository.nextReminderAt
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Enhanced Statistics - General (30 days)
    private val _averageDaily = MutableStateFlow<Int?>(null)
    val averageDaily = _averageDaily.asStateFlow()

    private val _bestDay = MutableStateFlow<DaySum?>(null)
    val bestDay = _bestDay.asStateFlow()

    private val _worstDay = MutableStateFlow<DaySum?>(null)
    val worstDay = _worstDay.asStateFlow()

    private val _currentStreak = MutableStateFlow<Int?>(null)
    val currentStreak = _currentStreak.asStateFlow()

    // Week-specific statistics
    private val _weekAverage = MutableStateFlow<Int?>(null)
    val weekAverage = _weekAverage.asStateFlow()

    private val _weekBestDay = MutableStateFlow<DaySum?>(null)
    val weekBestDay = _weekBestDay.asStateFlow()

    private val _weekWorstDay = MutableStateFlow<DaySum?>(null)
    val weekWorstDay = _weekWorstDay.asStateFlow()

    // Month-specific statistics
    private val _monthAverage = MutableStateFlow<Int?>(null)
    val monthAverage = _monthAverage.asStateFlow()

    private val _monthBestDay = MutableStateFlow<DaySum?>(null)
    val monthBestDay = _monthBestDay.asStateFlow()

    private val _monthWorstDay = MutableStateFlow<DaySum?>(null)
    val monthWorstDay = _monthWorstDay.asStateFlow()

    val previousWeekStats = drinkRepository.getPreviousWeekStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadEnhancedStatistics()
        cleanupOldEntries()
    }

    private fun loadEnhancedStatistics() {
        viewModelScope.launch {
            // Load average daily (last 7 days) - general
            _averageDaily.value = drinkRepository.getAverageDaily(7)
            
            // Load best and worst days (last 30 days) - general
            _bestDay.value = drinkRepository.getBestDay(30)
            _worstDay.value = drinkRepository.getWorstDay(30)
            
            // Load week-specific statistics
            _weekAverage.value = drinkRepository.getWeekAverage()
            _weekBestDay.value = drinkRepository.getWeekBestDay()
            _weekWorstDay.value = drinkRepository.getWeekWorstDay()
            
            // Load month-specific statistics
            _monthAverage.value = drinkRepository.getMonthAverage()
            _monthBestDay.value = drinkRepository.getMonthBestDay()
            _monthWorstDay.value = drinkRepository.getMonthWorstDay()
            
            // Load current streak
            val target = dailyTarget.value
            _currentStreak.value = drinkRepository.getCurrentStreak(target)
        }
    }

    private fun cleanupOldEntries() {
        viewModelScope.launch {
            drinkRepository.cleanupOldEntries()
        }
    }

    fun refreshStatistics() {
        loadEnhancedStatistics()
    }

    fun addDrink(amount: Int) {
        viewModelScope.launch {
            drinkRepository.addDrink(amount)
            // Refresh statistics after adding drink
            loadEnhancedStatistics()
            // Cleanup old entries periodically
            cleanupOldEntries()
            if (reminderEnabled.value) {
                // Reschedule with current interval
                scheduleReminderWithNextTime(reminderInterval.value)
            }
        }
    }
    
    fun setReminderInterval(minutes: Int) {
         viewModelScope.launch {
            settingsRepository.setReminderInterval(minutes)
             if (reminderEnabled.value) {
                scheduleReminderWithNextTime(minutes)
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
            // Refresh streak with new target
            _currentStreak.value = drinkRepository.getCurrentStreak(target)
        }
    }

    fun toggleReminder(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.setReminderEnabled(enabled)
                if (enabled) {
                    // Get the interval from the repository to ensure we have the latest value
                    val intervalMinutes = settingsRepository.reminderInterval.first()
                    scheduleReminderWithNextTime(intervalMinutes)
                } else {
                    ReminderManager.cancelReminder(application)
                    settingsRepository.setNextReminderAt(null)
                }
            } catch (e: Exception) {
                // If scheduling fails, disable the reminder setting
                settingsRepository.setReminderEnabled(false)
                settingsRepository.setNextReminderAt(null)
            }
        }
    }

    fun setReminderMode(mode: de.timbornemann.simplesipscheduler.data.repository.ReminderMode) {
        viewModelScope.launch {
            settingsRepository.setReminderMode(mode)
        }
    }

    private suspend fun scheduleReminderWithNextTime(intervalMinutes: Int) {
        val intervalMs = intervalMinutes.toLong() * 60 * 1000L
        ReminderManager.scheduleReminder(application, intervalMs)
        val nextReminder = ReminderTimeCalculator.calculateNextReminderTime(
            now = LocalDateTime.now(),
            intervalMinutes = intervalMinutes,
            quietStartHour = quietHoursStart.value,
            quietEndHour = quietHoursEnd.value
        )
        val nextReminderMillis = nextReminder.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        settingsRepository.setNextReminderAt(nextReminderMillis)
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
