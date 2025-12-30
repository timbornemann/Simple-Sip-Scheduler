package de.timbornemann.simplesipscheduler.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "drink_entries")
data class DrinkEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val amountMl: Int
)
