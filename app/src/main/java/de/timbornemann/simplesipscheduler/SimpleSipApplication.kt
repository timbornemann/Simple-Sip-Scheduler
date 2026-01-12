package de.timbornemann.simplesipscheduler

import android.app.Application
import android.content.ComponentName
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import de.timbornemann.simplesipscheduler.complication.MainComplicationService
import de.timbornemann.simplesipscheduler.data.database.DrinkDatabase
import de.timbornemann.simplesipscheduler.data.health.HealthConnectManager
import de.timbornemann.simplesipscheduler.data.repository.DrinkRepository
import de.timbornemann.simplesipscheduler.data.repository.SettingsRepository
import de.timbornemann.simplesipscheduler.receiver.MidnightReceiver
import de.timbornemann.simplesipscheduler.tile.MainTileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SimpleSipApplication : Application() {
    val database by lazy { DrinkDatabase.getDatabase(this) }
    val drinkRepository by lazy { DrinkRepository(database.drinkDao()) }
    val settingsRepository by lazy { SettingsRepository(this) }
    val healthConnectManager by lazy { HealthConnectManager(this) }
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onCreate() {
        super.onCreate()
        
        // Schedule midnight alarm for day change updates
        MidnightReceiver.scheduleMidnightAlarm(this)
        
        // Cleanup old entries on app start
        applicationScope.launch {
            drinkRepository.cleanupOldEntries()
        }
        
        // Listen for data changes and update Tile and Complication
        applicationScope.launch {
            drinkRepository.getTodayProgress().collect {
                // Update Tile
                TileService.getUpdater(this@SimpleSipApplication)
                    .requestUpdate(MainTileService::class.java)
                
                // Update Complication
                try {
                    ComplicationDataSourceUpdateRequester.create(
                        this@SimpleSipApplication,
                        ComponentName(this@SimpleSipApplication, MainComplicationService::class.java)
                    ).requestUpdateAll()
                } catch (e: Exception) {
                    // Ignore if complication update fails
                }
            }
        }
    }
}
