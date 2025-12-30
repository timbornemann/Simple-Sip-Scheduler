package de.timbornemann.simplesipscheduler

import android.app.Application
import de.timbornemann.simplesipscheduler.data.database.DrinkDatabase
import de.timbornemann.simplesipscheduler.data.repository.DrinkRepository
import de.timbornemann.simplesipscheduler.data.repository.SettingsRepository

class SimpleSipApplication : Application() {
    val database by lazy { DrinkDatabase.getDatabase(this) }
    val drinkRepository by lazy { DrinkRepository(database.drinkDao()) }
    val settingsRepository by lazy { SettingsRepository(this) }
}
