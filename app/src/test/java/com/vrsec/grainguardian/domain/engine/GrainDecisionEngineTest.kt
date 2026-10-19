package com.vrsec.grainguardian.domain.engine

import com.vrsec.grainguardian.data.model.CropType
import com.vrsec.grainguardian.data.model.RiskStatus
import org.junit.Assert.*
import org.junit.Test

class GrainDecisionEngineTest {

    @Test
    fun testPaddySafeMoistureReturnsSafeDryingStatus() {
        val result = GrainDecisionEngine.evaluateDrying(CropType.PADDY, 12.8f)
        assertEquals(RiskStatus.SAFE, result.status)
        assertTrue(result.isMoistureSafe)
        assertTrue(result.bdrs in 5..18)
        assertEquals(100 - result.bdrs, result.gshi)
        assertTrue(result.whyReasons.isNotEmpty())
        assertTrue(result.whyReasonsTelugu.isNotEmpty())
        assertTrue(result.recommendedActions.isNotEmpty())
    }

    @Test
    fun testPaddyWarningMoistureReturnsWarningDryingStatus() {
        // Safe limit is 13.5%. 14.5% is within +2.5% warning window
        val result = GrainDecisionEngine.evaluateDrying(CropType.PADDY, 14.5f)
        assertEquals(RiskStatus.WARNING, result.status)
        assertFalse(result.isMoistureSafe)
        assertTrue(result.bdrs in 35..65)
        assertTrue(result.headline.contains("NOT READY", ignoreCase = true))
    }

    @Test
    fun testPaddyHighRiskMoistureReturnsHighRiskDryingStatus() {
        // Safe limit is 13.5%. 18.0% exceeds safe + 2.5%
        val result = GrainDecisionEngine.evaluateDrying(CropType.PADDY, 18.0f)
        assertEquals(RiskStatus.HIGH_RISK, result.status)
        assertFalse(result.isMoistureSafe)
        assertTrue(result.bdrs >= 70)
    }

    @Test
    fun testNormalStorageThermalProfileReturnsSafe() {
        // TGI = 25.94 - 25.38 = 0.56°C, M = 13.4%
        val result = GrainDecisionEngine.evaluateStorage(
            crop = CropType.PADDY,
            moisture = 13.4f,
            t1Top = 25.38f,
            t2Middle = 25.56f,
            t3Bottom = 25.94f,
            humidity = 64.4f
        )
        assertEquals(RiskStatus.SAFE, result.status)
        assertEquals(0.56f, result.tgi, 0.05f)
        assertTrue(result.bdrs in 6..20)
        assertEquals(100 - result.bdrs, result.gshi)
        assertEquals("GRAIN STORAGE IS SAFE", result.headline)
        assertEquals("నిల్వ సురక్షితంగా ఉంది", result.headlineTelugu)
    }

    @Test
    fun testStorageHotspotGradientTriggersWarningOrHighRisk() {
        // High core temperature creates severe hotspot: TGI = 38.5 - 28.0 = 10.5°C
        val result = GrainDecisionEngine.evaluateStorage(
            crop = CropType.PADDY,
            moisture = 13.0f,
            t1Top = 28.0f,
            t2Middle = 38.5f,
            t3Bottom = 29.0f,
            humidity = 65.0f
        )
        // Peak > 38.0°C triggers HIGH_RISK
        assertEquals(RiskStatus.HIGH_RISK, result.status)
        assertEquals(10.5f, result.tgi, 0.05f)
        assertTrue(result.dominantRisk.contains("Hotspot", ignoreCase = true))
        assertTrue(result.whyReasons.any { it.contains("Internal heat accumulation", ignoreCase = true) })
    }

    @Test
    fun testDifferentCropsAdhereToDistinctMoistureLimits() {
        assertEquals(13.5f, CropType.PADDY.safeMoistureThreshold, 0.01f)
        assertEquals(14.0f, CropType.MAIZE.safeMoistureThreshold, 0.01f)
        assertEquals(12.0f, CropType.WHEAT.safeMoistureThreshold, 0.01f)
        assertEquals(10.0f, CropType.PULSES.safeMoistureThreshold, 0.01f)

        // 13.8% is SAFE for Maize (<=14.0%), but WARNING for Paddy (>13.5%) and HIGH_RISK for Wheat (>12.0%)
        val maizeResult = GrainDecisionEngine.evaluateDrying(CropType.MAIZE, 13.8f)
        val paddyResult = GrainDecisionEngine.evaluateDrying(CropType.PADDY, 13.8f)
        val wheatResult = GrainDecisionEngine.evaluateDrying(CropType.WHEAT, 13.8f)

        assertEquals(RiskStatus.SAFE, maizeResult.status)
        assertEquals(RiskStatus.WARNING, paddyResult.status)
        assertEquals(RiskStatus.WARNING, wheatResult.status)
    }

    @Test
    fun testMathematicalBoundsOfBDRSandGSHI() {
        val dryResult = GrainDecisionEngine.evaluateDrying(CropType.PADDY, 10.0f)
        assertTrue(dryResult.bdrs in 0..100)
        assertTrue(dryResult.gshi in 0..100)
        assertEquals(100, dryResult.bdrs + dryResult.gshi)

        val wetResult = GrainDecisionEngine.evaluateDrying(CropType.PADDY, 28.0f)
        assertTrue(wetResult.bdrs in 0..100)
        assertTrue(wetResult.gshi in 0..100)
        assertEquals(100, wetResult.bdrs + wetResult.gshi)
    }
}
