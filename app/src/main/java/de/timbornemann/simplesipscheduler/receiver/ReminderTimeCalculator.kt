package de.timbornemann.simplesipscheduler.receiver

import java.time.LocalDateTime
import java.time.LocalTime

object ReminderTimeCalculator {
    fun calculateNextReminderTime(
        now: LocalDateTime,
        intervalMinutes: Int,
        quietStartHour: Int,
        quietEndHour: Int
    ): LocalDateTime {
        var candidate = now.plusMinutes(intervalMinutes.toLong())
        while (isQuietTime(candidate.toLocalTime(), quietStartHour, quietEndHour)) {
            candidate = candidate.plusMinutes(intervalMinutes.toLong())
        }
        return candidate.withSecond(0).withNano(0)
    }

    fun isQuietTime(time: LocalTime, quietStartHour: Int, quietEndHour: Int): Boolean {
        val hour = time.hour
        return if (quietStartHour < quietEndHour) {
            hour in quietStartHour until quietEndHour
        } else {
            hour >= quietStartHour || hour < quietEndHour
        }
    }
}
