package de.timbornemann.simplesipscheduler

import android.app.Application
import de.timbornemann.simplesipscheduler.data.database.DrinkDatabase
import de.timbornemann.simplesipscheduler.data.repository.DrinkRepository
import de.timbornemann.simplesipscheduler.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SimpleSipApplication : Application() {
    val database by lazy { DrinkDatabase.getDatabase(this) }
    val drinkRepository by lazy { DrinkRepository(database.drinkDao()) }
    val settingsRepository by lazy { SettingsRepository(this) }
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onCreate() {
        super.onCreate()
        // Cleanup old entries on app start
        applicationScope.launch {
            drinkRepository.cleanupOldEntries()
        }
    }
}
