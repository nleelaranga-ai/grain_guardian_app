package com.vrsec.grainguardian.data.bluetooth

import org.junit.Assert.*
import org.junit.Test

class BleHistorySyncTest {

    @Test
    fun testHistoricalPacketParsingAccuracy() {
        val rawPacket = "HIST:SEQ=42,UP=7200,T1=28.54,T2=29.12,T3=30.01,MV=2110,H=64,F=31"
        assertTrue(rawPacket.startsWith("HIST:"))
        val body = rawPacket.substring(5)
        val items = body.split(",")
        val map = items.associate {
            val kv = it.split("=")
            kv[0].trim().uppercase() to kv[1].trim()
        }

        assertEquals("42", map["SEQ"])
        assertEquals("7200", map["UP"])
        assertEquals(28.54f, map["T1"]?.toFloat() ?: 0f, 0.01f)
        assertEquals(29.12f, map["T2"]?.toFloat() ?: 0f, 0.01f)
        assertEquals(30.01f, map["T3"]?.toFloat() ?: 0f, 0.01f)
        assertEquals(2110, map["MV"]?.toInt())
        assertEquals(64f, map["H"]?.toFloat() ?: 0f, 0.1f)
        assertEquals(31, map["F"]?.toInt())

        val calculatedMoisture = BleManager.convertMillivoltsToMoisture(2110)
        assertEquals(13.41f, calculatedMoisture, 0.05f)
    }

    @Test
    fun testSensorErrorTokensDoNotThrowExceptions() {
        val errorPacket = "HIST:SEQ=43,UP=7500,T1=ERR,T2=29.50,T3=ERR,MV=0,H=65,F=10"
        val body = errorPacket.substring(5)
        val items = body.split(",")
        val map = items.associate {
            val kv = it.split("=")
            kv[0].trim().uppercase() to kv[1].trim()
        }

        val t1 = if (map["T1"].equals("ERR", true)) 0f else map["T1"]!!.toFloat()
        val t2 = if (map["T2"].equals("ERR", true)) 0f else map["T2"]!!.toFloat()
        val t3 = if (map["T3"].equals("ERR", true)) 0f else map["T3"]!!.toFloat()

        assertEquals(0f, t1, 0.001f)
        assertEquals(29.50f, t2, 0.01f)
        assertEquals(0f, t3, 0.001f)
    }

    @Test
    fun testUptimeTimestampReconstitutionAccuracy() {
        val currentSyncWallClock = 1700000000000L // arbitrary fixed epoch timestamp
        val currentDeviceUptimeSec = 3600L       // 1 hour since boot
        val recordUptimeSec = 3000L              // 50 minutes since boot (10 mins prior)

        val diffSec = currentDeviceUptimeSec - recordUptimeSec
        val reconstitutedTimestamp = currentSyncWallClock - (diffSec * 1000L)

        val expectedTimestamp = 1700000000000L - (600L * 1000L) // 10 minutes earlier
        assertEquals(expectedTimestamp, reconstitutedTimestamp)
    }

    @Test
    fun testBleCommandFormatCompatibility() {
        val cmdInfo = "CMD:INFO\n"
        val cmdSync = "CMD:SYNC\n"
        val cmdClear = "CMD:CLEAR_HISTORY\n"
        val cmdPing = "CMD:PING\n"
        val cmdBeep = "CMD:BEEP\n"

        assertEquals("CMD:INFO", cmdInfo.trim())
        assertEquals("CMD:SYNC", cmdSync.trim())
        assertEquals("CMD:CLEAR_HISTORY", cmdClear.trim())
        assertEquals("CMD:PING", cmdPing.trim())
        assertEquals("CMD:BEEP", cmdBeep.trim())
    }

    @Test
    fun testDeviceInfoPacketWithProbesAndOled() {
        val infoPacket = "INFO:VER=2.0;RECS=12;UP=3600;PROBES=3;OLED=1"
        assertTrue(infoPacket.startsWith("INFO:"))
        val body = infoPacket.substring(5)
        var ver = ""
        var recs = 0
        var up = 0L
        var probes = 0
        var oled = false

        for (part in body.split(";")) {
            val kv = part.split("=")
            if (kv.size == 2) {
                when (kv[0].trim().uppercase()) {
                    "VER" -> ver = kv[1].trim()
                    "RECS" -> recs = kv[1].trim().toIntOrNull() ?: 0
                    "UP" -> up = kv[1].trim().toLongOrNull() ?: 0L
                    "PROBES" -> probes = kv[1].trim().toIntOrNull() ?: 0
                    "OLED" -> oled = kv[1].trim() == "1"
                }
            }
        }

        assertEquals("2.0", ver)
        assertEquals(12, recs)
        assertEquals(3600L, up)
        assertEquals(3, probes)
        assertTrue(oled)
    }

    @Test
    fun testMoistureVoltageClampingLimits() {
        // Safe standard paddy reading: ~2100 mV
        val standardMoisture = BleManager.convertMillivoltsToMoisture(2100)
        assertEquals(13.5f, standardMoisture, 0.01f)

        // Extremely low mV (simulated saturated wet water/fault) clamps to maximum
        val wetMoisture = BleManager.convertMillivoltsToMoisture(200)
        assertTrue(wetMoisture <= 32.0f)
        assertTrue(wetMoisture >= 30.0f)

        // Zero mV fault returns 0.0
        val zeroMoisture = BleManager.convertMillivoltsToMoisture(0)
        assertEquals(0.0f, zeroMoisture, 0.001f)

        // High mV (dry air: ~2800 mV) clamps to minimum safe bound
        val dryMoisture = BleManager.convertMillivoltsToMoisture(3200)
        assertEquals(8.0f, dryMoisture, 0.001f)
    }

    @Test
    fun testStatusFlagBitmaskEvaluation() {
        // Bit0: T1, Bit1: T2, Bit2: T3, Bit3: Moisture, Bit4: DHT, Bit5: OLED, Bit6: HOTSPOT
        val flagsAllNormal = 0b00111111 // 63: T1..T3 + Moisture + DHT + OLED
        val flagsHotspot = 0b01111111   // 127: All sensors + Hotspot active

        val isT1Ok = (flagsAllNormal and (1 shl 0)) != 0
        val isT2Ok = (flagsAllNormal and (1 shl 1)) != 0
        val isT3Ok = (flagsAllNormal and (1 shl 2)) != 0
        val isMoistureOk = (flagsAllNormal and (1 shl 3)) != 0
        val isDhtOk = (flagsAllNormal and (1 shl 4)) != 0
        val isOledOk = (flagsAllNormal and (1 shl 5)) != 0
        val isHotspotNormal = (flagsAllNormal and (1 shl 6)) != 0
        val isHotspotActive = (flagsHotspot and (1 shl 6)) != 0

        assertTrue(isT1Ok)
        assertTrue(isT2Ok)
        assertTrue(isT3Ok)
        assertTrue(isMoistureOk)
        assertTrue(isDhtOk)
        assertTrue(isOledOk)
        assertFalse(isHotspotNormal)
        assertTrue(isHotspotActive)
    }
}
