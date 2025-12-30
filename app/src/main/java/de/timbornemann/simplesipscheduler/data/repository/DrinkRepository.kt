package de.timbornemann.simplesipscheduler.data.repository

import de.timbornemann.simplesipscheduler.data.database.DrinkDao
import de.timbornemann.simplesipscheduler.data.database.DrinkEntry
import kotlinx.coroutines.flow.Flow
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
}
