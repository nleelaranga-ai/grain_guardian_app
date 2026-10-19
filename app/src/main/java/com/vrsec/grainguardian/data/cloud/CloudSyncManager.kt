package com.vrsec.grainguardian.data.cloud

import com.vrsec.grainguardian.data.database.GrainInspectionDao
import com.vrsec.grainguardian.data.model.GrainInspectionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

sealed interface CloudSyncState {
    object Idle : CloudSyncState
    object Syncing : CloudSyncState
    data class Success(val inserted: Int, val skipped: Int) : CloudSyncState
    data class Offline(val reason: String) : CloudSyncState
}

class CloudSyncManager(
    private val dao: GrainInspectionDao,
    private val defaultEndpoint: String = "http://10.0.2.2:8000/api/v1/telemetry/sync"
) {
    private val _syncState = MutableStateFlow<CloudSyncState>(CloudSyncState.Idle)
    val syncState: StateFlow<CloudSyncState> = _syncState.asStateFlow()

    fun buildSyncPayload(
        deviceMac: String,
        inspections: List<GrainInspectionEntity>
    ): String {
        val root = JSONObject()
        root.put("device_mac", deviceMac)
        val recordsArray = JSONArray()

        for (item in inspections) {
            val rec = JSONObject()
            rec.put("uuid", item.uuid)
            rec.put("sync_uuid", item.uuid)
            rec.put("seq_id", item.id.toInt())
            rec.put("timestamp", item.timestamp)
            rec.put("crop_name", item.cropName)
            rec.put("mode", item.mode)
            rec.put("moisture", item.moisture.toDouble())
            rec.put("t1_top", item.t1Top.toDouble())
            rec.put("t2_middle", item.t2Middle.toDouble())
            rec.put("t3_bottom", item.t3Bottom.toDouble())
            rec.put("ambient_humidity", item.humidity.toDouble())
            rec.put("status", item.status)
            rec.put("risk_score", item.riskScore)
            rec.put("dominant_risk", item.dominantRisk)
            rec.put("recommendations", item.recommendationsJson)
            recordsArray.put(rec)
        }
        root.put("records", recordsArray)
        return root.toString()
    }

    suspend fun syncInspectionsToCloud(
        deviceMac: String = "24:6F:28:AB:CD:EF",
        customEndpoint: String? = null
    ): CloudSyncState = withContext(Dispatchers.IO) {
        _syncState.value = CloudSyncState.Syncing
        val endpointUrl = customEndpoint?.ifBlank { null } ?: defaultEndpoint

        try {
            val pending = dao.getPendingForSync()
            val inspections = if (pending.isNotEmpty()) pending else dao.getAllInspectionsList()
            if (inspections.isEmpty()) {
                val state = CloudSyncState.Success(0, 0)
                _syncState.value = state
                return@withContext state
            }

            val jsonString = buildSyncPayload(deviceMac, inspections)

            val url = URL(endpointUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            conn.doOutput = true

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(jsonString)
                writer.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val respJson = JSONObject(responseText)
                val inserted = respJson.optInt("inserted", 0)
                val skipped = respJson.optInt("duplicates_skipped", 0)

                // Update local Room database records to CONFIRMED
                for (item in inspections) {
                    dao.updateSyncState(item.id, "CONFIRMED")
                }

                val state = CloudSyncState.Success(inserted, skipped)
                _syncState.value = state
                state
            } else {
                val state = CloudSyncState.Offline("Cloud responded with HTTP $responseCode")
                _syncState.value = state
                state
            }
        } catch (e: Exception) {
            val state = CloudSyncState.Offline("Cloud server offline (${e.message ?: "timeout"})")
            _syncState.value = state
            state
        }
    }
}
