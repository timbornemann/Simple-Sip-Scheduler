package de.timbornemann.simplesipscheduler.complication

import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import de.timbornemann.simplesipscheduler.SimpleSipApplication
import de.timbornemann.simplesipscheduler.data.repository.DrinkRepository
import de.timbornemann.simplesipscheduler.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first

/**
 * Complication data source that shows drink progress.
 */
class MainComplicationService : SuspendingComplicationDataSourceService() {

    private val drinkRepository: DrinkRepository
        get() = (applicationContext as SimpleSipApplication).drinkRepository

    private val settingsRepository: SettingsRepository
        get() = (applicationContext as SimpleSipApplication).settingsRepository

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return when (type) {
            ComplicationType.SHORT_TEXT -> createShortTextData(1250, 2500)
            ComplicationType.RANGED_VALUE -> createRangedValueData(1250, 2500)
            ComplicationType.LONG_TEXT -> createLongTextData(1250, 2500)
            else -> null
        }
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        val progress = drinkRepository.getTodayProgress().first() ?: 0
        val target = settingsRepository.dailyTarget.first()

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> createShortTextData(progress, target)
            ComplicationType.RANGED_VALUE -> createRangedValueData(progress, target)
            ComplicationType.LONG_TEXT -> createLongTextData(progress, target)
            else -> throw IllegalArgumentException("Unsupported complication type: ${request.complicationType}")
        }
    }

    private fun createShortTextData(progress: Int, target: Int): ShortTextComplicationData {
        val percentage = if (target > 0) {
            ((progress.toFloat() / target.toFloat()) * 100f).toInt().coerceIn(0, 100)
        } else {
            0
        }
        
        // Show percentage if space is limited, otherwise show progress/target
        val text = if (percentage >= 100) {
            "100%"
        } else {
            "$percentage%"
        }
        
        val contentDescription = "$progress ml von $target ml ($percentage%)"
        
        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text).build(),
            contentDescription = PlainComplicationText.Builder(contentDescription).build()
        ).build()
    }

    private fun createRangedValueData(progress: Int, target: Int): RangedValueComplicationData {
        val value = if (target > 0) {
            (progress.toFloat() / target.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
        
        val percentage = (value * 100f).toInt().coerceIn(0, 100)
        val contentDescription = "$progress ml von $target ml ($percentage%)"
        
        return RangedValueComplicationData.Builder(
            value = value,
            min = 0f,
            max = 1f,
            contentDescription = PlainComplicationText.Builder(contentDescription).build()
        ).build()
    }

    private fun createLongTextData(progress: Int, target: Int): LongTextComplicationData {
        val percentage = if (target > 0) {
            ((progress.toFloat() / target.toFloat()) * 100f).toInt().coerceIn(0, 100)
        } else {
            0
        }
        
        val text = "$progress ml von $target ml ($percentage%)"
        val contentDescription = "$progress ml von $target ml ($percentage%)"
        
        return LongTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text).build(),
            contentDescription = PlainComplicationText.Builder(contentDescription).build()
        ).build()
    }
}