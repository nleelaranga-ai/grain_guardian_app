package com.vrsec.grainguardian.data.bluetooth

import org.junit.Assert.*
import org.junit.Test

class SensorCalibrationTest {

    private fun convertMillivoltsToMoisture(mv: Int): Float {
        return BleManager.convertMillivoltsToMoisture(mv)
    }

    @Test
    fun testZeroOrNegativeMillivoltsReturnsZero() {
        assertEquals(0.0f, convertMillivoltsToMoisture(0), 0.001f)
        assertEquals(0.0f, convertMillivoltsToMoisture(-500), 0.001f)
    }

    @Test
    fun testBaseline2100mVYieldsExact13Point5Percent() {
        // 2100 mV is the empirical calibration point for 13.5% safe paddy moisture
        val moisture = convertMillivoltsToMoisture(2100)
        assertEquals(13.5f, moisture, 0.01f)
    }

    @Test
    fun testLiveSerialReading2110mVYieldsSafeMoisture() {
        // Live reading from user's running ESP32 was 2110 mV
        val moisture = convertMillivoltsToMoisture(2110)
        assertEquals(13.41f, moisture, 0.05f)
        assertTrue("Live reading must be within safe paddy storage limit", moisture <= 13.5f)
    }

    @Test
    fun testDryAirVoltageClampsCorrectly() {
        // In dry air, SEN0193 outputs ~2800-3000 mV (over-dry grain)
        val moisture = convertMillivoltsToMoisture(2900)
        assertEquals(8.0f, moisture, 0.1f) // Clamped to minimum realistic grain moisture
    }

    @Test
    fun testWetGrainVoltageCalculatesHighMoisture() {
        // Wet paddy at harvest outputs ~1500-1600 mV
        val moisture = convertMillivoltsToMoisture(1600)
        assertEquals(18.0f, moisture, 0.1f)
        assertTrue("Wet harvest must register above 17% moisture", moisture >= 17.0f)
    }
}
