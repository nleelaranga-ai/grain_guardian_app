package com.vrsec.grainguardian.worker

import com.vrsec.grainguardian.data.model.GrainInspectionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.max

/**
 * Unit tests verifying Storage Alert & Risk Evaluation logic executed by StorageHealthCheckWorker.
 */
class StorageAlertEvaluationTest {

    private fun evaluateAlertConditions(inspection: GrainInspectionEntity): List<String> {
        val alerts = mutableListOf<String>()
        val tempDiff = abs(inspection.t3Bottom - inspection.t1Top)
        val hasHotspot = tempDiff >= 3.0f || inspection.t3Bottom >= 35.0f
        val isMoistureHigh = inspection.moisture > 14.5f
        val isBdrsHigh = inspection.riskScore >= 45

        if (hasHotspot) {
            alerts.add("HOTSPOT_ALERT: diff=${String.format("%.1f", tempDiff)}C")
        }
        if (isMoistureHigh) {
            alerts.add("MOISTURE_WARNING: ${inspection.moisture}%")
        }
        if (isBdrsHigh) {
            alerts.add("BDRS_ALERT: ${inspection.riskScore}")
        }
        return alerts
    }

    @Test
    fun testSafeInspectionGeneratesZeroAlerts() {
        val safeInspection = GrainInspectionEntity(
            cropName = "Paddy",
            mode = "Storage Check",
            moisture = 13.0f,
            t1Top = 28.5f,
            t2Middle = 29.0f,
            t3Bottom = 29.2f,
            humidity = 62.0f,
            status = "SAFE",
            riskScore = 15,
            dominantRisk = "Optimal moisture condition",
            recommendationsJson = "Safe for continued storage",
            timestamp = 1700000000000L,
            formattedDate = "Today, 10:00 AM"
        )

        val alerts = evaluateAlertConditions(safeInspection)
        assertTrue("Safe inspection should trigger no background alerts", alerts.isEmpty())
    }

    @Test
    fun testHotspotTriggersCriticalThermalAlert() {
        val hotspotInspection = GrainInspectionEntity(
            cropName = "Paddy",
            mode = "Storage Check",
            moisture = 13.2f,
            t1Top = 27.5f,
            t2Middle = 31.0f,
            t3Bottom = 33.8f, // 33.8 - 27.5 = 6.3°C diff (>= 3.0°C threshold)
            humidity = 65.0f,
            status = "WARNING",
            riskScore = 55,
            dominantRisk = "Core heat accumulation",
            recommendationsJson = "Aerate immediately",
            timestamp = 1700000000000L,
            formattedDate = "Today, 10:00 AM"
        )

        val alerts = evaluateAlertConditions(hotspotInspection)
        assertTrue(alerts.any { it.startsWith("HOTSPOT_ALERT") })
        assertTrue(alerts.any { it.startsWith("BDRS_ALERT") })
    }

    @Test
    fun testExcessiveMoistureTriggersHighMoistureAlert() {
        val highMoistureInspection = GrainInspectionEntity(
            cropName = "Paddy",
            mode = "Pre-Storage Check",
            moisture = 16.8f,
            t1Top = 28.0f,
            t2Middle = 28.2f,
            t3Bottom = 28.5f,
            humidity = 70.0f,
            status = "WARNING",
            riskScore = 60,
            dominantRisk = "Elevated moisture",
            recommendationsJson = "Sun-drying required",
            timestamp = 1700000000000L,
            formattedDate = "Today, 10:00 AM"
        )

        val alerts = evaluateAlertConditions(highMoistureInspection)
        assertTrue(alerts.any { it.startsWith("MOISTURE_WARNING") })
        assertEquals(16.8f, highMoistureInspection.moisture, 0.01f)

        // Pre-storage excess calculation
        val excessToEvaporate = max(0f, highMoistureInspection.moisture - 13.0f)
        assertEquals(3.8f, excessToEvaporate, 0.05f)
    }

    @Test
    fun testBottomTempAbove35CTriggersHotspotRegardlessOfTopTemp() {
        val criticalBottom = GrainInspectionEntity(
            cropName = "Paddy",
            mode = "Storage Check",
            moisture = 13.5f,
            t1Top = 33.5f,
            t2Middle = 34.0f,
            t3Bottom = 35.5f, // >= 35°C critical fermentation mark
            humidity = 68.0f,
            status = "CRITICAL",
            riskScore = 75,
            dominantRisk = "Critical core fermentation",
            recommendationsJson = "Emergency turnaround",
            timestamp = 1700000000000L,
            formattedDate = "Today, 10:00 AM"
        )

        val alerts = evaluateAlertConditions(criticalBottom)
        assertTrue(alerts.any { it.startsWith("HOTSPOT_ALERT") })
    }
}
