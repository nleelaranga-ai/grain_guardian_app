package com.vrsec.grainguardian.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.vrsec.grainguardian.data.database.GrainGuardianDatabase
import com.vrsec.grainguardian.domain.localization.LocalizationManager
import com.vrsec.grainguardian.domain.notification.GrainNotificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class StorageHealthCheckWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val database = GrainGuardianDatabase.getDatabase(applicationContext)
            val dao = database.inspectionDao()
            val latest = dao.getLatestInspectionDirect() ?: return@withContext Result.success()

            val isTelugu = LocalizationManager.isTeluguRaw()
            val tempDiff = abs(latest.t3Bottom - latest.t1Top)
            val hasHotspot = tempDiff >= 3.0f || latest.t3Bottom >= 35.0f
            val isMoistureHigh = latest.moisture > 14.5f
            val isBdrsHigh = latest.riskScore >= 45

            if (hasHotspot) {
                val title = if (isTelugu) "⚠️ హాట్‌స్పాట్ హెచ్చరిక! (తాప వ్యత్యాసం: ${String.format("%.1f", tempDiff)}°C)"
                    else "⚠️ Hotspot Alert! (Temp Gradient: ${String.format("%.1f", tempDiff)}°C)"
                val msg = if (isTelugu) "నిల్వ లోతుల్లో తీవ్ర ఉష్ణోగ్రత పెరుగుదల ఉంది. వెంటనే ధాన్యాన్ని కలపండి మరియు గాలి ప్రసరణ చేయండి."
                    else "Thermal hotspot detected in storage core. Aerate or turn grain immediately to prevent mold."
                GrainNotificationManager.showStorageAlert(
                    context = applicationContext,
                    notificationId = 1002,
                    title = title,
                    message = msg,
                    isCritical = true
                )
            } else if (isMoistureHigh) {
                val title = if (isTelugu) "⚠️ అధిక తేమ హెచ్చరిక! (${latest.moisture}%)"
                    else "⚠️ High Moisture Warning! (${latest.moisture}%)"
                val msg = if (isTelugu) "ధాన్యం తేమ 14.5% కంటే ఎక్కువగా ఉంది. బయోలాజికల్ క్షీణత మరియు పురుగుల ప్రమాదం ఉంది."
                    else "Grain moisture exceeds safe threshold (14.5%). Risk of mold and fungal spoilage."
                GrainNotificationManager.showStorageAlert(
                    context = applicationContext,
                    notificationId = 1003,
                    title = title,
                    message = msg,
                    isCritical = false
                )
            } else if (isBdrsHigh) {
                val title = if (isTelugu) "నిల్వ ఆరోగ్య క్షీణత (BDRS: ${latest.riskScore})"
                    else "Storage Health Degradation (BDRS: ${latest.riskScore})"
                val msg = if (isTelugu) "ధాన్యం నిల్వ ప్రమాద స్కోరు పెరిగింది. దయచేసి వివరాలను పరిశీలించండి."
                    else "Biological degradation risk is elevated. Check storage ventilation and aeration."
                GrainNotificationManager.showStorageAlert(
                    context = applicationContext,
                    notificationId = 1004,
                    title = title,
                    message = msg,
                    isCritical = false
                )
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        const val PERIODIC_WORK_TAG = "grainguardian_periodic_health_check"

        fun schedulePeriodicWork(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<StorageHealthCheckWorker>(
                15, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        fun triggerImmediateCheck(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<StorageHealthCheckWorker>().build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}
