package com.vrsec.grainguardian

import android.app.Application
import com.vrsec.grainguardian.data.database.GrainGuardianDatabase
import com.vrsec.grainguardian.data.repository.InspectionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class GrainGuardianApp : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { GrainGuardianDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { InspectionRepository(database.inspectionDao(), this) }
}
