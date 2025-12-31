package de.timbornemann.simplesipscheduler.receiver

import android.app.Notification
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class ReminderReceiver : BroadcastReceiver() {
    companion object {
        const val CHANNEL_ID = "drink_reminder_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_ADD_DRINK = "de.timbornemann.simplesipscheduler.ACTION_ADD_DRINK"
        const val EXTRA_AMOUNT = "amount"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val app = context.applicationContext as SimpleSipApplication
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = app.settingsRepository
                val enabled = settings.reminderEnabled.first()
                
                if (enabled) {
                    val intervalMinutes = settings.reminderInterval.first()
                    val reminderMode = settings.reminderMode.first()
                    val startHour = settings.quietHoursStart.first()
                    val endHour = settings.quietHoursEnd.first()
                    
                    val now = LocalTime.now()
                    val currentHour = now.hour
                    
                    // Simple quiet hours check (assuming start < end for day interval, or start > end for night interval)
                    val isQuietTime = if (startHour < endHour) {
                         currentHour in startHour until endHour
                    } else {
                         currentHour >= startHour || currentHour < endHour
                    }

                    if (!isQuietTime) {
                        // Check reminder mode
                        val shouldShow = when (reminderMode) {
                            ReminderMode.ALWAYS -> true
                            ReminderMode.ONLY_UNDER_TARGET -> {
                                // Check if progress is under target
                                val progress = app.drinkRepository.getTodayProgress().first() ?: 0
                                val target = settings.dailyTarget.first()
                                progress < target
                            }
                        }
                        
                        if (shouldShow) {
                            showNotification(context, app)
                        }
                    }
                    
                    // Reschedule
                    ReminderManager.scheduleReminder(context, TimeUnit.MINUTES.toMillis(intervalMinutes.toLong()))
                }
            } finally {
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
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Erinnerung Wasser zu trinken"
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
        
        // Create quick action intents
        val actionAmounts = listOf(100, 250, 500)
        val actionIntents = actionAmounts.mapIndexed { index, amount ->
            val actionIntent = Intent(ACTION_ADD_DRINK).apply {
                setPackage(context.packageName)
                putExtra(EXTRA_AMOUNT, amount)
            }
            PendingIntent.getBroadcast(
                context,
                index + 100, // Unique request codes
                actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) 
            .setContentTitle("Zeit zu trinken!")
            .setContentText("$progress / $target ml")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_input_add, "+100ml", actionIntents[0])
            .addAction(android.R.drawable.ic_input_add, "+250ml", actionIntents[1])
            .addAction(android.R.drawable.ic_input_add, "+500ml", actionIntents[2])
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
            NotificationManager.IMPORTANCE_DEFAULT
        )
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
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }
}
