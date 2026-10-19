package com.vrsec.grainguardian.data.repository

import android.content.Context
import com.vrsec.grainguardian.data.bluetooth.BleConnectionState
import com.vrsec.grainguardian.data.bluetooth.BleManager
import com.vrsec.grainguardian.data.bluetooth.DeviceInfo
import com.vrsec.grainguardian.data.bluetooth.HistoricalRecord
import com.vrsec.grainguardian.data.bluetooth.ProbeSensorData
import com.vrsec.grainguardian.data.bluetooth.SyncStatus
import com.vrsec.grainguardian.data.cloud.CloudSyncManager
import com.vrsec.grainguardian.data.cloud.CloudSyncState
import com.vrsec.grainguardian.data.database.GrainInspectionDao
import com.vrsec.grainguardian.data.model.CropType
import com.vrsec.grainguardian.data.model.GrainInspectionEntity
import com.vrsec.grainguardian.domain.engine.GrainDecisionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InspectionRepository(
    private val dao: GrainInspectionDao,
    context: Context
) {
    val bleManager = BleManager(context)
    val cloudSyncManager = CloudSyncManager(dao)
    private val repoScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val allInspections: Flow<List<GrainInspectionEntity>> = dao.getAllInspections()
    val latestInspection: Flow<GrainInspectionEntity?> = dao.getLatestInspection()

    val bleConnectionState: StateFlow<BleConnectionState> = bleManager.connectionState
    val liveSensorData: StateFlow<ProbeSensorData> = bleManager.liveSensorData
    val isDeveloperSandboxEnabled: StateFlow<Boolean> = bleManager.isDeveloperSandboxEnabled
    val deviceInfo: StateFlow<DeviceInfo?> = bleManager.deviceInfo
    val syncStatus: StateFlow<SyncStatus> = bleManager.syncStatus
    val historicalRecordsFlow: SharedFlow<HistoricalRecord> = bleManager.historicalRecordsFlow
    val cloudSyncState: StateFlow<CloudSyncState> = cloudSyncManager.syncState

    init {
        // Collect and persist incoming historical records from ESP32 LittleFS flash
        repoScope.launch {
            bleManager.historicalRecordsFlow.collect { record ->
                val time = record.timestamp
                val existingCount = dao.countInspectionsInRange(time - 30000L, time + 30000L)
                if (existingCount == 0 && record.moisture > 0f) {
                    val decision = GrainDecisionEngine.evaluateStorage(
                        crop = CropType.PADDY,
                        moisture = record.moisture,
                        t1Top = record.t1,
                        t2Middle = record.t2,
                        t3Bottom = record.t3,
                        humidity = record.humidity
                    )

                    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                    val formatted = dateFormat.format(Date(time))

                    val entity = GrainInspectionEntity(
                        cropName = "Paddy",
                        mode = "Storage Check",
                        moisture = decision.moisture,
                        t1Top = decision.t1Top,
                        t2Middle = decision.t2Middle,
                        t3Bottom = decision.t3Bottom,
                        humidity = decision.humidity,
                        status = decision.status.name,
                        riskScore = decision.bdrs,
                        dominantRisk = decision.dominantRisk,
                        recommendationsJson = decision.recommendedActions.joinToString("|"),
                        timestamp = time,
                        formattedDate = formatted
                    )
                    dao.insertInspection(entity)
                }
            }
        }
    }

    fun requestDeviceInfo() {
        bleManager.requestDeviceInfo()
    }

    fun requestHistorySync() {
        bleManager.requestHistorySync()
    }

    fun clearDeviceHistory() {
        bleManager.clearDeviceHistory()
    }

    fun pingProbe(): Boolean {
        return bleManager.pingDevice()
    }

    fun triggerTestBeep(): Boolean {
        return bleManager.triggerTestBeep()
    }

    suspend fun syncToCloud(endpoint: String? = null): CloudSyncState {
        return cloudSyncManager.syncInspectionsToCloud(customEndpoint = endpoint)
    }

    val warningInspections: Flow<List<GrainInspectionEntity>> = dao.getWarningsInspections()
    val deviceProblemInspections: Flow<List<GrainInspectionEntity>> = dao.getDeviceProblemsInspections()
    val pendingSyncCount: Flow<Int> = dao.countPendingSync()
    val confirmedSyncCount: Flow<Int> = dao.countConfirmedSync()

    suspend fun save(inspection: GrainInspectionEntity): Long {
        return dao.insertInspection(inspection)
    }

    fun listAll(): Flow<List<GrainInspectionEntity>> = dao.getAllInspections()

    fun listWarnings(): Flow<List<GrainInspectionEntity>> = dao.getWarningsInspections()

    fun listDeviceProblems(): Flow<List<GrainInspectionEntity>> = dao.getDeviceProblemsInspections()

    suspend fun getDetails(id: Long): GrainInspectionEntity? {
        return dao.getInspectionById(id)
    }

    suspend fun pendingForSync(): List<GrainInspectionEntity> {
        return dao.getPendingForSync()
    }

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

    suspend fun exportInspectionsToCsv(context: Context): File = withContext(Dispatchers.IO) {
        val inspections = dao.getAllInspectionsList()
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, "grainguardian_inspections_${System.currentTimeMillis()}.csv")

        file.printWriter().use { out ->
            out.println("ID,UUID,Timestamp,FormattedDate,CropName,Mode,MoisturePct,T1TopC,T2MiddleC,T3BottomC,HumidityRH,Status,RiskScore,DominantRisk,SyncState,Recommendations")
            for (rec in inspections) {
                val cleanRecs = rec.recommendationsJson.replace(",", ";").replace("\"", "'")
                val cleanDominant = rec.dominantRisk.replace(",", ";")
                out.println("${rec.id},\"${rec.uuid}\",${rec.timestamp},\"${rec.formattedDate}\",\"${rec.cropName}\",\"${rec.mode}\",${rec.moisture},${rec.t1Top},${rec.t2Middle},${rec.t3Bottom},${rec.humidity},\"${rec.status}\",${rec.riskScore},\"$cleanDominant\",\"${rec.syncState}\",\"$cleanRecs\"")
            }
        }
        file
    }
}
