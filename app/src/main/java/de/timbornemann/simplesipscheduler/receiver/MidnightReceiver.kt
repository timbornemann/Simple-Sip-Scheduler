package de.timbornemann.simplesipscheduler.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import de.timbornemann.simplesipscheduler.complication.MainComplicationService
import de.timbornemann.simplesipscheduler.tile.MainTileService
import java.util.Calendar

/**
 * Receiver that triggers at midnight to update the tile and complication
 * to show the new day's progress (which starts at 0).
 */
class MidnightReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "MidnightReceiver"
        private const val REQUEST_CODE = 2001
        
        /**
         * Schedule the midnight alarm to trigger at 00:00:05 (5 seconds after midnight)
         */
        fun scheduleMidnightAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            if (alarmManager == null) {
                Log.e(TAG, "AlarmManager not available")
                return
            }
            
            val intent = Intent(context, MidnightReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Calculate next midnight + 5 seconds
            val calendar = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 5)
                set(Calendar.MILLISECOND, 0)
            }
            
            // If it's already past midnight, schedule for tomorrow
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            
            try {
                // Use setAndAllowWhileIdle for battery efficiency
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
                Log.d(TAG, "Midnight alarm scheduled for ${calendar.time}")
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling midnight alarm", e)
            }
        }
        
        /**
         * Cancel the midnight alarm
         */
        fun cancelMidnightAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, MidnightReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                Log.d(TAG, "Midnight alarm cancelled")
            }
        }
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Midnight alarm triggered - updating tile and complication")
        
        try {
            // Request tile update
            TileService.getUpdater(context).requestUpdate(MainTileService::class.java)
            Log.d(TAG, "Tile update requested")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating tile", e)
        }
        
        try {
            // Request complication update
            ComplicationDataSourceUpdateRequester.create(
                context,
                ComponentName(context, MainComplicationService::class.java)
            ).requestUpdateAll()
            Log.d(TAG, "Complication update requested")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating complication", e)
        }
        
        // Schedule next midnight alarm
        scheduleMidnightAlarm(context)
    }
}

