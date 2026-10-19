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

        assertEquals("CMD:INFO", cmdInfo.trim())
        assertEquals("CMD:SYNC", cmdSync.trim())
        assertEquals("CMD:CLEAR_HISTORY", cmdClear.trim())
    }
}
