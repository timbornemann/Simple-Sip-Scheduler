package de.timbornemann.simplesipscheduler.tile

import android.content.ComponentName
import android.content.Context
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.material.Colors
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.protolayout.material.Chip
import androidx.wear.protolayout.material.ChipColors
import androidx.wear.protolayout.material.CompactChip
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.tooling.preview.Preview
import androidx.wear.tiles.tooling.preview.TilePreviewData
import androidx.wear.tooling.preview.devices.WearDevices
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.tiles.SuspendingTileService
import de.timbornemann.simplesipscheduler.SimpleSipApplication
import de.timbornemann.simplesipscheduler.complication.MainComplicationService
import de.timbornemann.simplesipscheduler.presentation.MainActivity
import de.timbornemann.simplesipscheduler.receiver.ReminderManager
import de.timbornemann.simplesipscheduler.receiver.ReminderTimeCalculator
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.ZoneId

private const val RESOURCES_VERSION = "1"
private const val ID_ADD_50 = "add_50"
private const val ID_SUBTRACT_50 = "subtract_50"

@OptIn(ExperimentalHorologistApi::class)
class MainTileService : SuspendingTileService() {

    override suspend fun resourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ) = resources(requestParams)

    override suspend fun tileRequest(
        requestParams: RequestBuilders.TileRequest
    ): TileBuilders.Tile {
        val app = applicationContext as SimpleSipApplication
        
        // Handle Quick Drink Clicks
        val clickedId = requestParams.state?.lastClickableId
        if (clickedId == ID_ADD_50 || clickedId == ID_SUBTRACT_50) {
            val amount = if (clickedId == ID_ADD_50) 50 else -50
            
            // Add drink and wait for it to complete
            app.drinkRepository.addDrink(amount)
            
            // Reschedule reminder if enabled
            val reminderEnabled = app.settingsRepository.reminderEnabled.first()
            if (reminderEnabled) {
                val intervalMinutes = app.settingsRepository.reminderInterval.first()
                val startHour = app.settingsRepository.quietHoursStart.first()
                val endHour = app.settingsRepository.quietHoursEnd.first()
                
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
                
                ReminderManager.scheduleReminder(this, delayMs)
                app.settingsRepository.setNextReminderAt(nextReminderMillis)
            }
            
            // Update complication
            try {
                ComplicationDataSourceUpdateRequester.create(
                    this,
                    ComponentName(this, MainComplicationService::class.java)
                ).requestUpdateAll()
            } catch (e: Exception) {
                // Ignore if complication update fails
            }
        }

        // Use direct query to get fresh data after insert
        val progress = app.drinkRepository.getTodayProgressDirect()
        val target = app.settingsRepository.dailyTarget.first()
        
        return tile(requestParams, this, progress, target)
    }
}

private fun resources(
    requestParams: RequestBuilders.ResourcesRequest
): ResourceBuilders.Resources {
    return ResourceBuilders.Resources.Builder()
        .setVersion(RESOURCES_VERSION)
        .addIdToImageMapping(
            "water_drop_icon",
            ResourceBuilders.ImageResource.Builder()
                .setAndroidResourceByResId(
                    ResourceBuilders.AndroidImageResourceByResId.Builder()
                        .setResourceId(de.timbornemann.simplesipscheduler.R.drawable.ic_water_drop)
                        .build()
                )
                .build()
        )
        .build()
}

private fun tile(
    requestParams: RequestBuilders.TileRequest,
    context: Context,
    progress: Int,
    target: Int
): TileBuilders.Tile {
    val singleTileTimeline = TimelineBuilders.Timeline.Builder()
        .addTimelineEntry(
            TimelineBuilders.TimelineEntry.Builder()
                .setLayout(
                    LayoutElementBuilders.Layout.Builder()
                        .setRoot(tileLayout(requestParams, context, progress, target))
                        .build()
                )
                .build()
        )
        .build()

    return TileBuilders.Tile.Builder()
        .setResourcesVersion(RESOURCES_VERSION)
        .setTileTimeline(singleTileTimeline)
        // Refresh tile every 30 minutes to ensure day change is detected
        .setFreshnessIntervalMillis(30 * 60 * 1000L)
        .build()
}

private fun tileLayout(
    requestParams: RequestBuilders.TileRequest,
    context: Context,
    progress: Int,
    target: Int
): LayoutElementBuilders.LayoutElement {
    // Launch app intent
    val launchIntent = ActionBuilders.LaunchAction.Builder()
        .setAndroidActivity(
            ActionBuilders.AndroidActivity.Builder()
                .setClassName(MainActivity::class.java.name)
                .setPackageName(context.packageName)
                .build()
        )
        .build()
    
    // Quick drink actions (+50ml and -50ml) - LoadAction
    val add50Clickable = ModifiersBuilders.Clickable.Builder()
        .setId(ID_ADD_50)
        .setOnClick(ActionBuilders.LoadAction.Builder().build())
        .build()
    
    val subtract50Clickable = ModifiersBuilders.Clickable.Builder()
        .setId(ID_SUBTRACT_50)
        .setOnClick(ActionBuilders.LoadAction.Builder().build())
        .build()

    // Colors
    val waterBlue = 0xFF29B6F6.toInt()
    val waterBlueWithAlpha = 0xCC29B6F6.toInt() // 0.8 alpha
    val white = 0xFFFFFFFF.toInt()
    val textGray = 0xB3FFFFFF.toInt() // 0.7 alpha
    val centerBoxBackground = 0xCC1A1A1A.toInt() // 0.8 alpha
    val backgroundRingColor = 0xFF1A1A1A.toInt()
    val buttonColor = 0xFF1A1A1A.toInt() // Dark background for chip

    // Progress calculations
    val progressFraction = if (target > 0) {
        (progress.toFloat() / target.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    // GAP AT TOP to match Overview Screen
    val startAngle = 20f
    val sweepAngle = 320f
    val arcLength = (progressFraction * sweepAngle).coerceIn(0f, sweepAngle)

    // 1. Background Arc (Ring)
    val backgroundArc = LayoutElementBuilders.Arc.Builder()
        .setAnchorAngle(
            DimensionBuilders.DegreesProp.Builder().setValue(startAngle).build()
        )
        .setAnchorType(LayoutElementBuilders.ARC_ANCHOR_START)
        .addContent(
            LayoutElementBuilders.ArcLine.Builder()
                .setLength(DimensionBuilders.DegreesProp.Builder().setValue(sweepAngle).build())
                .setThickness(DimensionBuilders.DpProp.Builder().setValue(6f).build())
                .setColor(argb(backgroundRingColor))
                .setStrokeCap(LayoutElementBuilders.STROKE_CAP_ROUND)
                .build()
        )
        .build()

    // 2. Progress Arc (Ring)
    val progressArc = LayoutElementBuilders.Arc.Builder()
        .setAnchorAngle(
            DimensionBuilders.DegreesProp.Builder().setValue(startAngle).build()
        )
        .setAnchorType(LayoutElementBuilders.ARC_ANCHOR_START)
        .addContent(
            LayoutElementBuilders.ArcLine.Builder()
                .setLength(DimensionBuilders.DegreesProp.Builder().setValue(arcLength).build())
                .setThickness(DimensionBuilders.DpProp.Builder().setValue(6f).build())
                .setColor(argb(waterBlue))
                .setStrokeCap(LayoutElementBuilders.STROKE_CAP_ROUND)
                .build()
        )
        .build()

    // 3. Center Content (Bubble)
    val centerContent = LayoutElementBuilders.Box.Builder()
        .setWidth(DimensionBuilders.DpProp.Builder().setValue(120f).build())
        .setHeight(DimensionBuilders.DpProp.Builder().setValue(120f).build())
        .setModifiers(
            ModifiersBuilders.Modifiers.Builder()
                .setBackground(
                    ModifiersBuilders.Background.Builder()
                        .setColor(argb(centerBoxBackground))
                        .setCorner(ModifiersBuilders.Corner.Builder().setRadius(DimensionBuilders.DpProp.Builder().setValue(60f).build()).build())
                        .build()
                )
                .setClickable(
                    ModifiersBuilders.Clickable.Builder()
                        .setOnClick(launchIntent)
                        .build()
                )
                .build()
        )
        .addContent(
            LayoutElementBuilders.Column.Builder()
                .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                .addContent(
                    // Icon
                    LayoutElementBuilders.Image.Builder()
                        .setResourceId("water_drop_icon")
                        .setWidth(DimensionBuilders.DpProp.Builder().setValue(32f).build())
                        .setHeight(DimensionBuilders.DpProp.Builder().setValue(32f).build())
                        .build()
                )
                .addContent(
                    // Progress
                    Text.Builder(context, "$progress")
                        .setTypography(Typography.TYPOGRAPHY_TITLE1)
                        .setColor(argb(waterBlue))
                        .build()
                )
                .addContent(
                    // Target
                    Text.Builder(context, "/ $target")
                        .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                        .setColor(argb(textGray))
                        .build()
                )
                .build()
        )
        .build()

    // 4. Quick Drink Buttons (Plus and Minus at bottom)
    val quickDrinkButtons = LayoutElementBuilders.Box.Builder()
        .setModifiers(
            ModifiersBuilders.Modifiers.Builder()
                .setPadding(
                    ModifiersBuilders.Padding.Builder()
                        .setBottom(DimensionBuilders.DpProp.Builder().setValue(10f).build())
                        .build()
                )
                .build()
        )
        .addContent(
            LayoutElementBuilders.Row.Builder()
                .addContent(
                    CompactChip.Builder(context, "-50", subtract50Clickable, requestParams.deviceConfiguration)
                        .setChipColors(ChipColors.primaryChipColors(Colors(
                            waterBlue,
                            white,
                            buttonColor,
                            buttonColor
                        )))
                        .build()
                )
                .addContent(
                    LayoutElementBuilders.Spacer.Builder()
                        .setWidth(DimensionBuilders.DpProp.Builder().setValue(6f).build())
                        .build()
                )
                .addContent(
                    CompactChip.Builder(context, "+50", add50Clickable, requestParams.deviceConfiguration)
                        .setChipColors(ChipColors.primaryChipColors(Colors(
                            waterBlue,
                            white,
                            buttonColor,
                            buttonColor
                        )))
                        .build()
                )
                .build()
        )
        .build()

    // Root Layout: BOX (No scrolling)
    return LayoutElementBuilders.Box.Builder()
        .setWidth(DimensionBuilders.expand())
        .setHeight(DimensionBuilders.expand())
        .addContent(backgroundArc)
        .addContent(progressArc)
        .addContent(centerContent)
        .addContent(
            // Position button at bottom center
            LayoutElementBuilders.Box.Builder()
                .setWidth(DimensionBuilders.expand())
                .setHeight(DimensionBuilders.expand())
                .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_BOTTOM)
                .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                .addContent(quickDrinkButtons)
                .build()
        )
        .build()
}

@Preview(device = WearDevices.SMALL_ROUND)
@Preview(device = WearDevices.LARGE_ROUND)
fun tilePreview(context: Context) = TilePreviewData(::resources) {
    tile(it, context, 1250, 2500)
}