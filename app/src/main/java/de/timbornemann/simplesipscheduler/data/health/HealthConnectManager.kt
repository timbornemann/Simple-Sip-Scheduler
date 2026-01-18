package de.timbornemann.simplesipscheduler.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.units.Volume
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class HealthConnectManager(private val context: Context) {
    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    val hydrationPermissions: Set<String> = setOf(
        HealthPermission.getWritePermission(HydrationRecord::class),
        HealthPermission.getReadPermission(HydrationRecord::class)
    )

    fun isAvailable(): Boolean {
        return HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    }

    suspend fun hasHydrationPermissions(): Boolean {
        val granted = client.permissionController.getGrantedPermissions()
        return granted.containsAll(hydrationPermissions)
    }

    suspend fun writeHydrationIfPermitted(amountMl: Int, timestampMillis: Long) {
        if (amountMl <= 0) {
            return
        }
        if (!isAvailable()) {
            return
        }
        if (!hasHydrationPermissions()) {
            return
        }
        val instant = Instant.ofEpochMilli(timestampMillis)
        val zonedDateTime = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault())
        val record = HydrationRecord(
            startTime = instant,
            startZoneOffset = zonedDateTime.offset,
            endTime = instant.plusSeconds(1),
            endZoneOffset = zonedDateTime.offset,
            volume = Volume.milliliters(amountMl.toDouble())
        )
        client.insertRecords(listOf(record))
    }
}
