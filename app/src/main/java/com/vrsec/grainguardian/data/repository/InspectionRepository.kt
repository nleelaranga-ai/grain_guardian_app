package com.vrsec.grainguardian.data.repository

import android.content.Context
import com.vrsec.grainguardian.data.bluetooth.BleConnectionState
import com.vrsec.grainguardian.data.bluetooth.BleManager
import com.vrsec.grainguardian.data.bluetooth.ProbeSensorData
import com.vrsec.grainguardian.data.database.GrainInspectionDao
import com.vrsec.grainguardian.data.model.GrainInspectionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class InspectionRepository(
    private val dao: GrainInspectionDao,
    context: Context
) {
    val bleManager = BleManager(context)

    val allInspections: Flow<List<GrainInspectionEntity>> = dao.getAllInspections()
    val latestInspection: Flow<GrainInspectionEntity?> = dao.getLatestInspection()

    val bleConnectionState: StateFlow<BleConnectionState> = bleManager.connectionState
    val liveSensorData: StateFlow<ProbeSensorData> = bleManager.liveSensorData
    val isDeveloperSandboxEnabled: StateFlow<Boolean> = bleManager.isDeveloperSandboxEnabled

    suspend fun getInspectionById(id: Long): GrainInspectionEntity? {
        return dao.getInspectionById(id)
    }

    suspend fun saveInspection(inspection: GrainInspectionEntity): Long {
        return dao.insertInspection(inspection)
    }

    suspend fun deleteInspection(inspection: GrainInspectionEntity) {
        dao.deleteInspection(inspection)
    }

    suspend fun clearAllHistory() {
        dao.clearAll()
    }

    suspend fun loadBenchmarkData() {
        val currentTime = System.currentTimeMillis()
        val oneDayMillis = 86400000L

        val benchmarkData = listOf(
            GrainInspectionEntity(
                cropName = "Paddy",
                mode = "Storage Check",
                moisture = 13.2f,
                t1Top = 28.8f,
                t2Middle = 29.4f,
                t3Bottom = 29.9f,
                humidity = 64.0f,
                status = "SAFE",
                riskScore = 12,
                dominantRisk = "Optimal moisture condition",
                recommendationsJson = "Safe for continued storage|Maintain dry ventilation|Inspect again in 7 days",
                timestamp = currentTime,
                formattedDate = "Today, 10:30 AM"
            ),
            GrainInspectionEntity(
                cropName = "Paddy",
                mode = "Storage Check",
                moisture = 14.9f,
                t1Top = 29.5f,
                t2Middle = 32.1f,
                t3Bottom = 34.0f,
                humidity = 72.0f,
                status = "WARNING",
                riskScore = 48,
                dominantRisk = "Elevated moisture & bottom heat accumulation",
                recommendationsJson = "Turn grain pile to dissipate heat|Ensure daytime aeration|Monitor moisture daily",
                timestamp = currentTime - (1 * oneDayMillis),
                formattedDate = "Yesterday, 04:45 PM"
            )
        )
        benchmarkData.forEach { dao.insertInspection(it) }
    }
}
