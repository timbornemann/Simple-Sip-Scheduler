package de.timbornemann.simplesipscheduler.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class DaySum(val date: String, val total: Int)

@Dao
interface DrinkDao {
    @Insert
    suspend fun insert(entry: DrinkEntry)

    @Delete
    suspend fun delete(entry: DrinkEntry)

    @androidx.room.Update
    suspend fun update(entry: DrinkEntry)

    @Query("SELECT * FROM drink_entries WHERE timestamp >= :startOfDay AND timestamp < :endOfDay ORDER BY timestamp DESC")
    fun getEntriesForDay(startOfDay: Long, endOfDay: Long): Flow<List<DrinkEntry>>

    @Query("SELECT SUM(amountMl) FROM drink_entries WHERE timestamp >= :startOfDay AND timestamp < :endOfDay")
    fun getTotalForDay(startOfDay: Long, endOfDay: Long): Flow<Int?>
    
    @Query("SELECT * FROM drink_entries ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEntries(limit: Int): Flow<List<DrinkEntry>>

    // For Statistics
    @Query("SELECT * FROM drink_entries WHERE timestamp >= :startTime AND timestamp < :endTime")
    suspend fun getEntriesInRange(startTime: Long, endTime: Long): List<DrinkEntry>

    @Query("SELECT strftime('%Y-%m-%d', datetime(timestamp / 1000, 'unixepoch', 'localtime')) as date, SUM(amountMl) as total FROM drink_entries WHERE timestamp >= :startTime AND timestamp < :endTime GROUP BY date ORDER BY date ASC")
    fun getDailySumsInRange(startTime: Long, endTime: Long): Flow<List<DaySum>>

    // Average daily for last N days
    @Query("SELECT AVG(daily_total) FROM (SELECT strftime('%Y-%m-%d', datetime(timestamp / 1000, 'unixepoch', 'localtime')) as date, SUM(amountMl) as daily_total FROM drink_entries WHERE timestamp >= :startTime AND timestamp < :endTime GROUP BY date)")
    suspend fun getAverageDailyInRange(startTime: Long, endTime: Long): Double?

    // Best day (highest total)
    @Query("SELECT strftime('%Y-%m-%d', datetime(timestamp / 1000, 'unixepoch', 'localtime')) as date, SUM(amountMl) as total FROM drink_entries WHERE timestamp >= :startTime AND timestamp < :endTime GROUP BY date ORDER BY total DESC LIMIT 1")
    suspend fun getBestDayInRange(startTime: Long, endTime: Long): DaySum?

    // Worst day (lowest total, excluding 0)
    @Query("SELECT strftime('%Y-%m-%d', datetime(timestamp / 1000, 'unixepoch', 'localtime')) as date, SUM(amountMl) as total FROM drink_entries WHERE timestamp >= :startTime AND timestamp < :endTime GROUP BY date HAVING total > 0 ORDER BY total ASC LIMIT 1")
    suspend fun getWorstDayInRange(startTime: Long, endTime: Long): DaySum?

    // Get all daily sums for streak calculation
    @Query("SELECT strftime('%Y-%m-%d', datetime(timestamp / 1000, 'unixepoch', 'localtime')) as date, SUM(amountMl) as total FROM drink_entries WHERE timestamp >= :startTime GROUP BY date ORDER BY date DESC")
    suspend fun getDailySumsFromDate(startTime: Long): List<DaySum>

    // Delete old entries (older than specified timestamp)
    @Query("DELETE FROM drink_entries WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOldEntries(cutoffTimestamp: Long)

}
