package com.vrsec.grainguardian.data.cloud

import com.vrsec.grainguardian.data.database.GrainInspectionDao
import com.vrsec.grainguardian.data.model.GrainInspectionEntity
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class CloudSyncPayloadTest {

    // Dummy DAO for test initialization
    private val fakeDao = object : GrainInspectionDao {
        override fun getAllInspections(): Flow<List<GrainInspectionEntity>> = throw NotImplementedError()
        override suspend fun getAllInspectionsList(): List<GrainInspectionEntity> = emptyList()
        override suspend fun getInspectionById(id: Long): GrainInspectionEntity? = null
        override fun getLatestInspection(): Flow<GrainInspectionEntity?> = throw NotImplementedError()
        override suspend fun countInspectionsInRange(startTime: Long, endTime: Long): Int = 0
        override fun getWarningsInspections(): Flow<List<GrainInspectionEntity>> = throw NotImplementedError()
        override fun getDeviceProblemsInspections(): Flow<List<GrainInspectionEntity>> = throw NotImplementedError()
        override suspend fun getPendingForSync(): List<GrainInspectionEntity> = emptyList()
        override suspend fun updateSyncState(id: Long, syncState: String) {}
        override fun countPendingSync(): Flow<Int> = throw NotImplementedError()
        override fun countConfirmedSync(): Flow<Int> = throw NotImplementedError()
        override suspend fun insertInspection(inspection: GrainInspectionEntity): Long = 1L
        override suspend fun deleteInspection(inspection: GrainInspectionEntity) {}
        override suspend fun clearAll() {}
    }

    @Test
    fun testBuildSyncPayloadJsonStructure() {
        val syncManager = CloudSyncManager(fakeDao)
        val sampleEntity = GrainInspectionEntity(
            id = 101L,
            cropName = "Paddy",
            mode = "Storage Check",
            moisture = 13.5f,
            t1Top = 27.5f,
            t2Middle = 28.0f,
            t3Bottom = 28.5f,
            humidity = 64.0f,
            status = "SAFE",
            riskScore = 15,
            dominantRisk = "Optimal condition",
            recommendationsJson = "Safe for storage|Inspect again in 7 days",
            timestamp = 1792348200000L,
            formattedDate = "19 Oct 2026, 10:00 AM"
        )

        val jsonStr = syncManager.buildSyncPayload("24:6F:28:AB:CD:EF", listOf(sampleEntity))
        assertNotNull(jsonStr)

        val root = JSONObject(jsonStr)
        assertEquals("24:6F:28:AB:CD:EF", root.getString("device_mac"))

        val records = root.getJSONArray("records")
        assertEquals(1, records.length())

        val rec = records.getJSONObject(0)
        assertEquals(sampleEntity.uuid, rec.getString("uuid"))
        assertEquals(sampleEntity.uuid, rec.getString("sync_uuid"))
        assertEquals(101, rec.getInt("seq_id"))
        assertEquals(1792348200000L, rec.getLong("timestamp"))
        assertEquals("Paddy", rec.getString("crop_name"))
        assertEquals(13.5, rec.getDouble("moisture"), 0.01)
        assertEquals(27.5, rec.getDouble("t1_top"), 0.01)
        assertEquals(28.0, rec.getDouble("t2_middle"), 0.01)
        assertEquals(28.5, rec.getDouble("t3_bottom"), 0.01)
        assertEquals("SAFE", rec.getString("status"))
        assertEquals(15, rec.getInt("risk_score"))
    }

    @Test
    fun testBuildSyncPayloadEmptyList() {
        val syncManager = CloudSyncManager(fakeDao)
        val jsonStr = syncManager.buildSyncPayload("24:6F:28:AB:CD:EF", emptyList())
        val root = JSONObject(jsonStr)
        assertEquals(0, root.getJSONArray("records").length())
    }

    @Test
    fun testCsvSerializationFormatting() {
        val sampleEntity = GrainInspectionEntity(
            id = 42L,
            cropName = "Paddy",
            mode = "Storage Check",
            moisture = 13.5f,
            t1Top = 28.5f,
            t2Middle = 29.0f,
            t3Bottom = 29.5f,
            humidity = 64.0f,
            status = "SAFE",
            riskScore = 14,
            dominantRisk = "Moisture Optimal",
            recommendationsJson = "Safe for storage|Inspect again in 7 days",
            timestamp = 1792348200000L,
            formattedDate = "19 Oct 2026, 10:00 AM",
            syncState = "CONFIRMED"
        )

        val header = "ID,UUID,Timestamp,FormattedDate,CropName,Mode,MoisturePct,T1TopC,T2MiddleC,T3BottomC,HumidityRH,Status,RiskScore,DominantRisk,SyncState,Recommendations"
        val cleanRecs = sampleEntity.recommendationsJson.replace(",", ";").replace("\"", "'")
        val cleanDominant = sampleEntity.dominantRisk.replace(",", ";")
        val row = "${sampleEntity.id},\"${sampleEntity.uuid}\",${sampleEntity.timestamp},\"${sampleEntity.formattedDate}\",\"${sampleEntity.cropName}\",\"${sampleEntity.mode}\",${sampleEntity.moisture},${sampleEntity.t1Top},${sampleEntity.t2Middle},${sampleEntity.t3Bottom},${sampleEntity.humidity},\"${sampleEntity.status}\",${sampleEntity.riskScore},\"$cleanDominant\",\"${sampleEntity.syncState}\",\"$cleanRecs\""

        assertTrue(header.startsWith("ID,UUID"))
        assertTrue(row.contains("42"))
        assertTrue(row.contains("Paddy"))
        assertTrue(row.contains("CONFIRMED"))
        assertTrue(row.contains("13.5"))
    }
}
