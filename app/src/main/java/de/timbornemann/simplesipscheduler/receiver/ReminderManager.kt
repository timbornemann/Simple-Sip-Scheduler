package de.timbornemann.simplesipscheduler.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log

object ReminderManager {
    private const val REQUEST_CODE = 1001
    private const val TAG = "ReminderManager"
    
    fun scheduleReminder(context: Context, delayMs: Long) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            if (alarmManager == null) {
                Log.e(TAG, "AlarmManager service not available")
                return
            }

            val intent = Intent(context, ReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Cancel previous if existing
            alarmManager.cancel(pendingIntent)

            // Set new alarm
            val triggerAtMillis = SystemClock.elapsedRealtime() + delayMs
            
            Log.d(TAG, "Scheduling reminder: delayMs=$delayMs, triggerAt=$triggerAtMillis")
            
            // Check if we can schedule exact alarms (required for Android 12+)
            val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }
            
            Log.d(TAG, "canScheduleExact=$canScheduleExact, SDK=${Build.VERSION.SDK_INT}")
            
            when {
                // Android 12+ with exact alarm permission
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && canScheduleExact -> {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "Scheduled exact alarm (Android 12+)")
                }
                // Android 12+ WITHOUT exact alarm permission - use inexact alarm as fallback
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                    Log.w(TAG, "Scheduled INEXACT alarm (no exact alarm permission)")
                }
                // Android 6-11: can use exact alarms without special permission
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "Scheduled exact alarm (Android 6-11)")
                }
                // Older Android versions
                else -> {
                    alarmManager.setExact(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "Scheduled exact alarm (legacy)")
                }
            }
            Log.d(TAG, "Reminder scheduled for ~${delayMs}ms from now")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling reminder", e)
        }
    }
    
    /**
     * Check if exact alarms can be scheduled.
     * Returns true if on Android 11 or below, or if permission is granted on Android 12+.
     */
    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        return alarmManager?.canScheduleExactAlarms() ?: false
    }

    fun cancelReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }
}
