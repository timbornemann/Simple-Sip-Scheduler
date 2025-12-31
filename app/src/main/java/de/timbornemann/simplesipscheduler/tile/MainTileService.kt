package de.timbornemann.simplesipscheduler.tile

import android.content.Context
import android.content.Intent as AndroidIntent
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.material.Colors
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.protolayout.material.layouts.PrimaryLayout
import androidx.wear.protolayout.material.Chip
import androidx.wear.protolayout.material.ChipColors
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.tooling.preview.Preview
import androidx.wear.tiles.tooling.preview.TilePreviewData
import androidx.wear.tooling.preview.devices.WearDevices
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.tiles.SuspendingTileService
import de.timbornemann.simplesipscheduler.SimpleSipApplication
import de.timbornemann.simplesipscheduler.presentation.MainActivity
import de.timbornemann.simplesipscheduler.receiver.ReminderReceiver
import kotlinx.coroutines.flow.first

private const val RESOURCES_VERSION = "0"

@OptIn(ExperimentalHorologistApi::class)
class MainTileService : SuspendingTileService() {

    override suspend fun resourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ) = resources(requestParams)

    override suspend fun tileRequest(
        requestParams: RequestBuilders.TileRequest
    ): TileBuilders.Tile {
        val app = applicationContext as SimpleSipApplication
        val progress = app.drinkRepository.getTodayProgress().first() ?: 0
        val target = app.settingsRepository.dailyTarget.first()
        
        return tile(requestParams, this, progress, target)
    }
}

private fun resources(
    requestParams: RequestBuilders.ResourcesRequest
): ResourceBuilders.Resources {
    return ResourceBuilders.Resources.Builder()
        .setVersion(RESOURCES_VERSION)
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
        .build()
}

private fun tileLayout(
    requestParams: RequestBuilders.TileRequest,
    context: Context,
    progress: Int,
    target: Int
): LayoutElementBuilders.LayoutElement {
    val launchIntent = ActionBuilders.LaunchAction.Builder()
        .setAndroidActivity(
            ActionBuilders.AndroidActivity.Builder()
                .setClassName(MainActivity::class.java.name)
                .setPackageName(context.packageName)
                .build()
        )
        .build()
        
    val clickable = ModifiersBuilders.Clickable.Builder()
        .setOnClick(launchIntent)
        .build()

    // Calculate progress percentage for visual indicator
    val progressPercent = if (target > 0) {
        ((progress.toFloat() / target.toFloat()) * 100f).toInt().coerceIn(0, 100)
    } else {
        0
    }
    
    // Water-themed colors
    val waterBlue = 0xFF29B6F6.toInt()
    val waterCyan = 0xFF03DAC5.toInt()
    val waterLightBlue = 0xFF00BCD4.toInt()
    val white = 0xFFFFFFFF.toInt()
    val darkGray = 0xFF1A1A1A.toInt()
    
    return PrimaryLayout.Builder(requestParams.deviceConfiguration)
        .setResponsiveContentInsetEnabled(true)
        .setContent(
            LayoutElementBuilders.Column.Builder()
                .addContent(
                    // Title
                    Text.Builder(context, "Heute")
                        .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                        .setColor(argb(waterBlue))
                        .build()
                )
                .addContent(
                    // Large progress number
                    LayoutElementBuilders.Row.Builder()
                        .addContent(
                            Text.Builder(context, "$progress")
                                .setTypography(Typography.TYPOGRAPHY_DISPLAY2)
                                .setColor(argb(waterCyan))
                                .build()
                        )
                        .addContent(
                            Text.Builder(context, " / $target")
                                .setTypography(Typography.TYPOGRAPHY_DISPLAY3)
                                .setColor(argb(white))
                                .build()
                        )
                        .addContent(
                            Text.Builder(context, " ml")
                                .setTypography(Typography.TYPOGRAPHY_BODY1)
                                .setColor(argb(white))
                                .build()
                        )
                        .build()
                )
                .addContent(
                    // Progress percentage indicator
                    Text.Builder(context, "$progressPercent%")
                        .setTypography(Typography.TYPOGRAPHY_CAPTION2)
                        .setColor(argb(waterLightBlue))
                        .build()
                )
                .addContent(
                    Chip.Builder(context, clickable, requestParams.deviceConfiguration)
                        .setPrimaryLabelContent("Öffnen")
                        .setChipColors(
                            ChipColors.primaryChipColors(
                                Colors(
                                    waterBlue,
                                    white,
                                    darkGray,
                                    darkGray
                                )
                            )
                        )
                        .build()
                )
                .build()
        )
        .setPrimaryChipContent(
            Chip.Builder(context, clickable, requestParams.deviceConfiguration)
                .setPrimaryLabelContent("Trinken (+)")
                .setChipColors(
                    ChipColors.secondaryChipColors(
                        Colors(
                            waterCyan,
                            white,
                            darkGray,
                            darkGray
                        )
                    )
                )
                .build()
        )
        .build()
}

@Preview(device = WearDevices.SMALL_ROUND)
@Preview(device = WearDevices.LARGE_ROUND)
fun tilePreview(context: Context) = TilePreviewData(::resources) {
    tile(it, context, 1250, 2500)
}