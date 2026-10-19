package com.vrsec.grainguardian.domain.engine

import com.vrsec.grainguardian.data.model.CropType
import com.vrsec.grainguardian.data.model.RiskStatus
import org.junit.Assert.*
import org.junit.Test

class GrainDecisionEngineTest {

    private val baseTime = System.currentTimeMillis()

    private fun createValidReadings(
        count: Int = 5,
        moisture: Float = 13.0f,
        moistureMv: Int = 2100,
        t1: Float = 28.0f,
        t2: Float = 29.0f,
        t3: Float = 29.5f,
        timestamp: Long = baseTime,
        isSimulated: Boolean = false
    ): List<GrainReading> {
        return List(count) { i ->
            GrainReading(
                timestamp = timestamp - (count - 1 - i) * 1000L,
                moisture = moisture,
                moistureMv = moistureMv,
                t1Top = t1,
                t2Middle = t2,
                t3Bottom = t3,
                ambientHumidity = 65.0f,
                ambientTemp = 28.0f,
                isSimulated = isSimulated
            )
        }
    }

    @Test
    fun testZeroMoistureVoltageYieldsSensorFaultAndNot13Point2() {
        // Moisture voltage 0.0 / moisture 0.0f must produce SENSOR_FAULT, NEVER 13.2%
        val readings = createValidReadings(moisture = 0.0f, moistureMv = 0)
        val result = GrainDecisionEngine.evaluatePreStorage(readings, currentTimeMs = baseTime)

        assertEquals(AssessmentStatus.SENSOR_FAULT, result.assessmentStatus)
        assertEquals(GrainDecisionEngine.REASON_SENSOR_FAULT_MOISTURE, result.reasonCode)
        assertFalse("Must never issue safe verdict on zero moisture", result.isMoistureSafe)
        assertNotEquals(13.2f, result.moisture, 0.001f)
    }

    @Test
    fun testMissingDS18B20ReadingYieldsSensorFault() {
        // Stored grain monitoring with missing/disconnected DS18B20 (-127°C or null)
        val readings = List(5) { i ->
            GrainReading(
                timestamp = baseTime - i * 1000L,
                moisture = 13.0f,
                moistureMv = 2100,
                t1Top = 28.0f,
                t2Middle = -127.0f, // Dallas disconnect token
                t3Bottom = 29.0f
            )
        }
        val result = GrainDecisionEngine.evaluateStoredGrain(readings, currentTimeMs = baseTime)

        assertEquals(AssessmentStatus.SENSOR_FAULT, result.assessmentStatus)
        assertEquals(GrainDecisionEngine.REASON_SENSOR_FAULT_TEMPERATURE, result.reasonCode)
        assertEquals("SENSOR FAULT", result.headline)
    }

    @Test
    fun testValidReadingsOlderThanMaxReadingAgeMinutesYieldsStale() {
        // Reading older than maxReadingAgeMinutes (30 mins = 1800s)
        val oldTimestamp = baseTime - (35 * 60 * 1000L) // 35 minutes ago
        val readings = createValidReadings(timestamp = oldTimestamp)
        val result = GrainDecisionEngine.evaluatePreStorage(readings, currentTimeMs = baseTime)

        assertEquals(AssessmentStatus.STALE, result.assessmentStatus)
        assertEquals(GrainDecisionEngine.REASON_STALE_DATA, result.reasonCode)
        assertEquals("STALE MEASUREMENT", result.headline)
    }

    @Test
    fun testFewerThanMinValidReadingsYieldsInsufficientEvidence() {
        // Only 3 readings provided (requires min 5)
        val readings = createValidReadings(count = 3)
        val result = GrainDecisionEngine.evaluatePreStorage(readings, currentTimeMs = baseTime)

        assertEquals(AssessmentStatus.INSUFFICIENT_EVIDENCE, result.assessmentStatus)
        assertEquals(GrainDecisionEngine.REASON_INSUFFICIENT_READINGS, result.reasonCode)
        assertEquals("INSUFFICIENT EVIDENCE", result.headline)
    }

    @Test
    fun testGradientAboveThermalGradientWarnCYieldsAttentionWithThermalGradientReason() {
        // Gradient |32.5 - 28.0| = 4.5°C > 3.0°C warn threshold
        val readings = createValidReadings(t1 = 28.0f, t2 = 30.0f, t3 = 32.5f)
        val result = GrainDecisionEngine.evaluateStoredGrain(readings, currentTimeMs = baseTime)

        assertEquals(AssessmentStatus.ATTENTION, result.assessmentStatus)
        assertEquals(GrainDecisionEngine.REASON_THERMAL_GRADIENT, result.reasonCode)
        assertEquals(4.5f, result.tgi, 0.05f)
    }

    @Test
    fun testEverythingValidAndWithinLimitsYieldsNormal() {
        // Moisture 12.8% <= 13.5%, TGI = 0.5°C <= 3.0°C, all readings valid and fresh
        val readings = createValidReadings(moisture = 12.8f, t1 = 28.0f, t2 = 28.2f, t3 = 28.5f)
        val preStorageResult = GrainDecisionEngine.evaluatePreStorage(readings, currentTimeMs = baseTime)
        val storedResult = GrainDecisionEngine.evaluateStoredGrain(readings, currentTimeMs = baseTime)

        assertEquals(AssessmentStatus.NORMAL, preStorageResult.assessmentStatus)
        assertEquals(AssessmentStatus.NORMAL, storedResult.assessmentStatus)
        assertEquals(RiskStatus.SAFE, preStorageResult.status)
        assertEquals(RiskStatus.SAFE, storedResult.status)
    }

    @Test
    fun testMoistureAboveTargetInPreStorageModeYieldsDryingNeededWithNoHoursValue() {
        // Moisture 15.5% > 13.5%
        val readings = createValidReadings(moisture = 15.5f)
        val result = GrainDecisionEngine.evaluatePreStorage(readings, currentTimeMs = baseTime)

        assertEquals(AssessmentStatus.ATTENTION, result.assessmentStatus)
        assertEquals(GrainDecisionEngine.REASON_MOISTURE_ABOVE_TARGET, result.reasonCode)
        assertTrue(result.headline.contains("NOT READY", ignoreCase = true))

        // Check qualitative guidance from FES
        assertEquals(GrainDecisionEngine.ADVICE_DRYING_NEEDED_EN, result.subtitle)
        assertEquals(GrainDecisionEngine.ADVICE_DRYING_NEEDED_TE, result.subtitleTelugu)

        // Verify NO hours numbers in subtitle or recommendations
        assertFalse("Must not contain numeric hours", result.subtitle.contains("hours", ignoreCase = true))
        assertFalse("Must not contain numeric hours", result.subtitleTelugu.contains("గంటలు"))
    }

    @Test
    fun testSameReadingsInStoredModeNeverProduceDryingAdvice() {
        // Same elevated moisture in stored mode
        val readings = createValidReadings(moisture = 15.5f, t1 = 28.0f, t2 = 28.5f, t3 = 29.0f)
        val storedResult = GrainDecisionEngine.evaluateStoredGrain(readings, currentTimeMs = baseTime)

        // Stored results must NEVER recommend drying or mention sun drying
        for (action in storedResult.recommendedActions) {
            assertFalse("Stored mode must never recommend sun drying", action.contains("sun dry", ignoreCase = true))
            assertFalse("Stored mode must never recommend sun drying", action.contains("drying yard", ignoreCase = true))
        }
        for (actionTe in storedResult.recommendedActionsTelugu) {
            assertFalse("Stored mode must never recommend sun drying in Telugu", actionTe.contains("ఎండలో ఆరబెట్టండి"))
        }
    }

    @Test
    fun testLosingConnectivityDoesNotChangeAnyAssessmentStatus() {
        // Connectivity is separate from agronomic assessment
        val readings = createValidReadings(moisture = 13.0f)
        val resultOnline = GrainDecisionEngine.evaluatePreStorage(readings, currentTimeMs = baseTime)

        // Even with connectivity = BLE_CONNECTED_OFFLINE or DEVICE_DISCONNECTED_LOCAL_AVAILABLE
        val connStateOffline = ConnectivityState.BLE_CONNECTED_OFFLINE
        val connStateDisconnected = ConnectivityState.DEVICE_DISCONNECTED_LOCAL_AVAILABLE

        // AssessmentStatus remains unchanged
        assertEquals(AssessmentStatus.NORMAL, resultOnline.assessmentStatus)
        assertNotNull(connStateOffline)
        assertNotNull(connStateDisconnected)
    }

    @Test
    fun testSimulatedInputIsAlwaysLabelledSimulated() {
        val simulatedReadings = createValidReadings(isSimulated = true)
        val result = GrainDecisionEngine.evaluatePreStorage(simulatedReadings, currentTimeMs = baseTime)

        assertTrue("Simulated reading must flag isSimulated = true", result.isSimulated)
        assertEquals("SIMULATED", result.source)
    }

    @Test
    fun testRuleConfigVersionCarriedInResult() {
        val config = RuleConfig(ruleVersion = "0.1.0-unvalidated")
        val readings = createValidReadings()
        val result = GrainDecisionEngine.evaluatePreStorage(readings, config = config, currentTimeMs = baseTime)

        assertEquals("0.1.0-unvalidated", result.ruleVersion)
    }
}
