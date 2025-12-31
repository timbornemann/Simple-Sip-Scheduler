package de.timbornemann.simplesipscheduler.data.repository

import de.timbornemann.simplesipscheduler.data.database.DrinkDao
import de.timbornemann.simplesipscheduler.data.database.DrinkEntry
import de.timbornemann.simplesipscheduler.data.database.DaySum
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId

class DrinkRepository(private val drinkDao: DrinkDao) {
    
    fun getTodayProgress(): Flow<Int?> {
        val (start, end) = getDayRange(LocalDate.now())
        return drinkDao.getTotalForDay(start, end)
    }

    fun getTodayEntries(): Flow<List<DrinkEntry>> {
        val (start, end) = getDayRange(LocalDate.now())
        return drinkDao.getEntriesForDay(start, end)
    }

    suspend fun addDrink(amountMl: Int) {
        val entry = DrinkEntry(
            timestamp = System.currentTimeMillis(),
            amountMl = amountMl
        )
        drinkDao.insert(entry)
    }

    suspend fun deleteEntry(entry: DrinkEntry) {
        drinkDao.delete(entry)
    }

    suspend fun updateEntry(entry: DrinkEntry) {
        drinkDao.update(entry)
    }

    private fun getDayRange(date: LocalDate): Pair<Long, Long> {
        val zoneId = ZoneId.systemDefault()
        val start = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return start to end
    }

    fun getWeekStats(): Flow<List<de.timbornemann.simplesipscheduler.data.database.DaySum>> {
        val today = LocalDate.now()
        val startOfWeek = today.minusDays(6) // Last 7 days
        val zoneId = ZoneId.systemDefault()
        val start = startOfWeek.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return drinkDao.getDailySumsInRange(start, end)
    }

    fun getMonthStats(): Flow<List<de.timbornemann.simplesipscheduler.data.database.DaySum>> {
        val today = LocalDate.now()
        val startOfMonth = today.withDayOfMonth(1)
        val zoneId = ZoneId.systemDefault()
        val start = startOfMonth.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return drinkDao.getDailySumsInRange(start, end)
    }

    suspend fun getAverageDaily(days: Int): Int {
        val today = LocalDate.now()
        val startDate = today.minusDays(days.toLong() - 1)
        val zoneId = ZoneId.systemDefault()
        val start = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val average = drinkDao.getAverageDailyInRange(start, end)
        return average?.toInt() ?: 0
    }

    suspend fun getBestDay(days: Int = 30): DaySum? {
        val today = LocalDate.now()
        val startDate = today.minusDays(days.toLong() - 1)
        val zoneId = ZoneId.systemDefault()
        val start = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return drinkDao.getBestDayInRange(start, end)
    }

    suspend fun getWorstDay(days: Int = 30): DaySum? {
        val today = LocalDate.now()
        val startDate = today.minusDays(days.toLong() - 1)
        val zoneId = ZoneId.systemDefault()
        val start = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return drinkDao.getWorstDayInRange(start, end)
    }

    suspend fun getCurrentStreak(target: Int): Int {
        // Get daily sums from last 365 days (enough for streak calculation)
        val today = LocalDate.now()
        val startDate = today.minusDays(365)
        val zoneId = ZoneId.systemDefault()
        val start = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        
        val dailySums = drinkDao.getDailySumsFromDate(start)
        
        // Create a map for quick lookup
        val dailyMap = dailySums.associateBy { daySum ->
            try {
                LocalDate.parse(daySum.date)
            } catch (e: Exception) {
                null
            }
        }.filterKeys { it != null }
        
        // Calculate streak: consecutive days from today backwards where total >= target
        var streak = 0
        var currentDate = today
        
        while (true) {
            val daySum = dailyMap[currentDate]
            if (daySum != null && daySum.total >= target) {
                streak++
                currentDate = currentDate.minusDays(1)
            } else {
                // Today might not have data yet, so check yesterday
                if (streak == 0 && currentDate == today) {
                    currentDate = currentDate.minusDays(1)
                    continue
                }
                break
            }
        }
        
        return streak
    }

    fun getPreviousWeekStats(): Flow<List<DaySum>> {
        val today = LocalDate.now()
        val endOfPreviousWeek = today.minusDays(7)
        val startOfPreviousWeek = endOfPreviousWeek.minusDays(6)
        val zoneId = ZoneId.systemDefault()
        val start = startOfPreviousWeek.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = endOfPreviousWeek.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return drinkDao.getDailySumsInRange(start, end)
    }
}
