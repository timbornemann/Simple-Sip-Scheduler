package de.timbornemann.simplesipscheduler.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import de.timbornemann.simplesipscheduler.R
import de.timbornemann.simplesipscheduler.presentation.MainActivity

import de.timbornemann.simplesipscheduler.SimpleSipApplication
import de.timbornemann.simplesipscheduler.data.repository.ReminderMode
import de.timbornemann.simplesipscheduler.receiver.ReminderTimeCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.time.LocalTime
import java.time.LocalDateTime
import java.time.ZoneId

class ReminderReceiver : BroadcastReceiver() {
    companion object {
        const val CHANNEL_ID = "drink_reminder_channel"
        const val NOTIFICATION_ID = 1
        private const val TAG = "ReminderReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.d(TAG, "onReceive called! Intent action: ${intent.action}")
        
        val pendingResult = goAsync()
        val app = context.applicationContext as SimpleSipApplication
        
        // Acquire WakeLock to ensure device stays awake for processing
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        val wakeLock = powerManager.newWakeLock(
            android.os.PowerManager.PARTIAL_WAKE_LOCK,
            "SimpleSipScheduler:ReminderWakeLock"
        )
        wakeLock.acquire(10 * 1000L) // 10 seconds timeout

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = app.settingsRepository
                val enabled = settings.reminderEnabled.first()
                
                android.util.Log.d(TAG, "Reminder enabled: $enabled")
                
                if (enabled) {
                    val intervalMinutes = settings.reminderInterval.first()
                    val reminderMode = settings.reminderMode.first()
                    val startHour = settings.quietHoursStart.first()
                    val endHour = settings.quietHoursEnd.first()
                    
                    val now = LocalTime.now()
                    val currentHour = now.hour
                    
                    android.util.Log.d(TAG, "Current hour: $currentHour, Quiet hours: $startHour-$endHour")
                    
                    // Simple quiet hours check (assuming start < end for day interval, or start > end for night interval)
                    val isQuietTime = if (startHour < endHour) {
                         currentHour in startHour until endHour
                    } else {
                         currentHour >= startHour || currentHour < endHour
                    }

                    android.util.Log.d(TAG, "Is quiet time: $isQuietTime")

                    if (!isQuietTime) {
                        // Check reminder mode
                        val shouldShow = when (reminderMode) {
                            ReminderMode.ALWAYS -> true
                            ReminderMode.ONLY_UNDER_TARGET -> {
                                // Check if progress is under target
                                val progress = app.drinkRepository.getTodayProgress().first() ?: 0
                                val target = settings.dailyTarget.first()
                                android.util.Log.d(TAG, "Progress: $progress, Target: $target")
                                progress < target
                            }
                        }
                        
                        android.util.Log.d(TAG, "Should show notification: $shouldShow (mode: $reminderMode)")
                        
                        if (shouldShow) {
                            android.util.Log.d(TAG, "Showing notification...")
                            showNotification(context, app)
                            android.util.Log.d(TAG, "Notification shown!")
                        }
                    } else {
                        android.util.Log.d(TAG, "Skipping notification due to quiet hours")
                    }
                    
                    val nextReminder = ReminderTimeCalculator.calculateNextReminderTime(
                        now = LocalDateTime.now(),
                        intervalMinutes = intervalMinutes,
                        quietStartHour = startHour,
                        quietEndHour = endHour
                    )
                    val nextReminderMillis = nextReminder.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val delayMs = (nextReminderMillis - System.currentTimeMillis()).coerceAtLeast(0)
                    
                    android.util.Log.d(TAG, "Rescheduling next reminder in ${delayMs}ms (at $nextReminder)")
                    
                    // Reschedule
                    ReminderManager.scheduleReminder(context, delayMs)
                    settings.setNextReminderAt(nextReminderMillis)
                } else {
                    android.util.Log.d(TAG, "Reminders disabled, not processing")
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error in ReminderReceiver", e)
            } finally {
                if (wakeLock.isHeld) {
                    wakeLock.release()
                }
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context, app: SimpleSipApplication) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Channel if needed
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Trink-Erinnerungen",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Erinnerung Wasser zu trinken"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 200, 500) // Stronger vibration
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(channel)

        val launchIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Get current progress for notification text
        val progress = kotlinx.coroutines.runBlocking {
            app.drinkRepository.getTodayProgress().first() ?: 0
        }
        val target = kotlinx.coroutines.runBlocking {
            app.settingsRepository.dailyTarget.first()
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_water_drop) 
            .setContentTitle("Zeit zu trinken!")
            .setContentText("$progress / $target ml")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
        
        // Check if goal is reached and show motivation notification
        if (progress >= target && target > 0) {
            showMotivationNotification(context, progress, target)
        }
    }
    
    private fun showMotivationNotification(context: Context, progress: Int, target: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Trink-Erinnerungen",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 250, 250, 250)
        }
        notificationManager.createNotificationChannel(channel)
        
        val launchIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val motivationalMessages = listOf(
            "Großartig! Ziel erreicht! 🎉",
            "Perfekt! Du hast dein Ziel erreicht! 💪",
            "Ausgezeichnet! Weiter so! 🌟"
        )
        val message = motivationalMessages.random()
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(message)
            .setContentText("$progress ml von $target ml getrunken")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }
}
