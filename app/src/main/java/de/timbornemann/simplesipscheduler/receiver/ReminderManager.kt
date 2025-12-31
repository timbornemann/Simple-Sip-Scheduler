package de.timbornemann.simplesipscheduler.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log
import java.util.concurrent.TimeUnit

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
            
            // Use setAndAllowWhileIdle for battery efficiency.
            // This is inexact (system can batch it) but ensures it fires even in Doze mode.
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
            Log.d(TAG, "Reminder scheduled for ~${delayMs}ms from now")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling reminder", e)
        }
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
