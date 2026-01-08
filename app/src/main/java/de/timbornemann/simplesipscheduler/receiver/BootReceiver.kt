package de.timbornemann.simplesipscheduler.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import de.timbornemann.simplesipscheduler.SimpleSipApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Receiver that reschedules reminders after device boot.
 * Alarms are lost when the device reboots, so we need to reschedule them.
 */
class BootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            
            Log.d(TAG, "Boot completed, checking reminder schedule")
            
            val pendingResult = goAsync()
            val app = context.applicationContext as SimpleSipApplication
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val settings = app.settingsRepository
                    val enabled = settings.reminderEnabled.first()
                    
                    if (enabled) {
                        Log.d(TAG, "Reminders are enabled, rescheduling...")
                        
                        val intervalMinutes = settings.reminderInterval.first()
                        val startHour = settings.quietHoursStart.first()
                        val endHour = settings.quietHoursEnd.first()
                        
                        val nextReminder = ReminderTimeCalculator.calculateNextReminderTime(
                            now = LocalDateTime.now(),
                            intervalMinutes = intervalMinutes,
                            quietStartHour = startHour,
                            quietEndHour = endHour
                        )
                        val nextReminderMillis = nextReminder.atZone(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()
                        val delayMs = (nextReminderMillis - System.currentTimeMillis()).coerceAtLeast(0)
                        
                        ReminderManager.scheduleReminder(context, delayMs)
                        settings.setNextReminderAt(nextReminderMillis)
                        
                        Log.d(TAG, "Reminder rescheduled for ${nextReminder}")
                    } else {
                        Log.d(TAG, "Reminders are disabled, not scheduling")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error rescheduling reminder after boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}

