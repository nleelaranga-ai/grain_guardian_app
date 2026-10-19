package com.vrsec.grainguardian

import android.app.Application
import com.vrsec.grainguardian.data.database.GrainGuardianDatabase
import com.vrsec.grainguardian.data.repository.InspectionRepository
import com.vrsec.grainguardian.domain.notification.GrainNotificationManager
import com.vrsec.grainguardian.worker.StorageHealthCheckWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class GrainGuardianApp : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { GrainGuardianDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { InspectionRepository(database.inspectionDao(), this) }

    override fun onCreate() {
        super.onCreate()
        // Initialize Notification Channels
        GrainNotificationManager.createNotificationChannels(this)
        // Schedule periodic background storage health monitoring via WorkManager
        StorageHealthCheckWorker.schedulePeriodicWork(this)
    }
}
