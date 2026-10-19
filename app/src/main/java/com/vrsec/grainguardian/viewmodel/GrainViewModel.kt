package com.vrsec.grainguardian.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vrsec.grainguardian.data.bluetooth.BleConnectionState
import com.vrsec.grainguardian.data.bluetooth.ProbeSensorData
import com.vrsec.grainguardian.data.bluetooth.DeviceInfo
import com.vrsec.grainguardian.data.bluetooth.SyncStatus
import com.vrsec.grainguardian.data.cloud.CloudSyncState
import com.vrsec.grainguardian.data.model.AssessmentMode
import com.vrsec.grainguardian.data.model.CropType
import com.vrsec.grainguardian.data.model.DeviceStatus
import com.vrsec.grainguardian.data.model.GrainInspectionEntity
import com.vrsec.grainguardian.data.repository.InspectionRepository
import com.vrsec.grainguardian.domain.engine.AssessmentStatus
import com.vrsec.grainguardian.domain.engine.DecisionResult
import com.vrsec.grainguardian.domain.engine.GrainDecisionEngine
import com.vrsec.grainguardian.domain.engine.GrainReading
import com.vrsec.grainguardian.domain.engine.RuleConfig
import com.vrsec.grainguardian.domain.localization.AppLanguage
import com.vrsec.grainguardian.domain.localization.LocalizationManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface MeasurementState {
    object Idle : MeasurementState
    object BlockedNoHardware : MeasurementState
    object CheckingSensors : MeasurementState
    data class ReceivingPackets(val packetsCount: Int) : MeasurementState
    data class WaitingForStability(val progress: Int) : MeasurementState
    object StableValidating : MeasurementState
    object ComputingDecision : MeasurementState
    object Completed : MeasurementState
    data class Error(val messageEn: String, val messageTe: String) : MeasurementState
}

class GrainViewModel(private val repository: InspectionRepository) : ViewModel() {

    val currentLanguage: StateFlow<AppLanguage> = LocalizationManager.currentLanguage

    val allInspections: StateFlow<List<GrainInspectionEntity>> = repository.allInspections
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val warningInspections: StateFlow<List<GrainInspectionEntity>> = repository.warningInspections
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deviceProblemInspections: StateFlow<List<GrainInspectionEntity>> = repository.deviceProblemInspections
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingSyncCount: StateFlow<Int> = repository.pendingSyncCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val confirmedSyncCount: StateFlow<Int> = repository.confirmedSyncCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val latestInspection: StateFlow<GrainInspectionEntity?> = repository.latestInspection
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val bleConnectionState: StateFlow<BleConnectionState> = repository.bleConnectionState
    val liveSensorData: StateFlow<ProbeSensorData> = repository.liveSensorData
    val isDeveloperSandboxEnabled: StateFlow<Boolean> = repository.isDeveloperSandboxEnabled
    val deviceInfo: StateFlow<DeviceInfo?> = repository.deviceInfo
    val syncStatus: StateFlow<SyncStatus> = repository.syncStatus
    val cloudSyncState: StateFlow<CloudSyncState> = repository.cloudSyncState

    // Derived device status
    val deviceStatus: StateFlow<DeviceStatus> = combine(
        repository.bleConnectionState,
        repository.liveSensorData
    ) { state, sensor ->
        when (state) {
            is BleConnectionState.Connected -> DeviceStatus(
                deviceName = state.deviceName,
                deviceId = state.deviceId,
                batteryPercent = if (sensor.batteryPercent > 0) sensor.batteryPercent else state.battery,
                isConnected = true,
                isScanning = false,
                moistureSensorOk = sensor.moistureSensorOk,
                tempSensor1Ok = sensor.t1Ok,
                tempSensor2Ok = sensor.t2Ok,
                tempSensor3Ok = sensor.t3Ok
            )
            is BleConnectionState.Scanning -> DeviceStatus(
                isScanning = true,
                isConnected = false
            )
            is BleConnectionState.Connecting -> DeviceStatus(
                deviceName = state.deviceName,
                isScanning = false,
                isConnected = false
            )
            is BleConnectionState.DeviceNotFound -> DeviceStatus(
                errorMessage = "GrainGuardian probe not detected",
                isConnected = false
            )
            is BleConnectionState.BluetoothDisabled -> DeviceStatus(
                errorMessage = "Bluetooth is turned off",
                isConnected = false
            )
            else -> DeviceStatus(isConnected = false)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DeviceStatus())

    val isProbeConnected: StateFlow<Boolean> = repository.bleConnectionState.map {
        it is BleConnectionState.Connected
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _selectedCrop = MutableStateFlow(CropType.PADDY)
    val selectedCrop = _selectedCrop.asStateFlow()

    private val _selectedMode = MutableStateFlow(AssessmentMode.DRYING_READINESS)
    val selectedMode = _selectedMode.asStateFlow()

    private val _measurementState = MutableStateFlow<MeasurementState>(MeasurementState.Idle)
    val measurementState = _measurementState.asStateFlow()

    private val _measurementProgress = MutableStateFlow(0)
    val measurementProgress = _measurementProgress.asStateFlow()

    private val _measurementPhase = MutableStateFlow("")
    val measurementPhase = _measurementPhase.asStateFlow()

    private val _liveMoistureDisplay = MutableStateFlow("--")
    val liveMoistureDisplay = _liveMoistureDisplay.asStateFlow()

    private val _liveTempDisplay = MutableStateFlow("--")
    val liveTempDisplay = _liveTempDisplay.asStateFlow()

    private val _currentDecision = MutableStateFlow<DecisionResult?>(null)
    val currentDecision = _currentDecision.asStateFlow()

    init {
        // Collect live sensor stream and update UI displays
        viewModelScope.launch {
            repository.liveSensorData.collect { sensor ->
                if (sensor.moisture > 0f) {
                    _liveMoistureDisplay.value = String.format(Locale.US, "%.1f%%", sensor.moisture)
                }
                if (sensor.t1Top > 0f) {
                    _liveTempDisplay.value = String.format(Locale.US, "%.1f°C", sensor.t1Top)
                }
            }
        }
    }

    fun setLanguage(lang: AppLanguage) {
        LocalizationManager.setLanguage(lang)
    }

    fun selectCrop(crop: CropType) {
        _selectedCrop.value = crop
    }

    fun selectAssessmentMode(mode: AssessmentMode) {
        _selectedMode.value = mode
    }

    fun startDryingCheckFlow() {
        _selectedCrop.value = CropType.PADDY
        _selectedMode.value = AssessmentMode.DRYING_READINESS
    }

    fun startStorageCheckFlow() {
        _selectedCrop.value = CropType.PADDY
        _selectedMode.value = AssessmentMode.STORAGE_HEALTH
    }

    fun hasBlePermissions(): Boolean {
        return repository.bleManager.hasPermissions()
    }

    fun scanForDevices() {
        repository.bleManager.startScan()
    }

    fun disconnectProbe() {
        repository.bleManager.disconnect()
    }

    fun setDeveloperSandboxMode(enabled: Boolean) {
        repository.bleManager.setDeveloperSandboxMode(enabled)
    }

    fun startLiveMeasurement(onComplete: (AssessmentMode) -> Unit) {
        viewModelScope.launch {
            // Rule 1: No measurement without connected hardware
            if (!isProbeConnected.value) {
                _measurementState.value = MeasurementState.BlockedNoHardware
                _measurementPhase.value = if (LocalizationManager.isTeluguRaw()) 
                    "ప్రోబ్ కనెక్ట్ కాలేదు. కొలత ప్రారంభించలేము." 
                    else "Probe not connected. Cannot start measurement."
                return@launch
            }

            _measurementProgress.value = 5
            _measurementState.value = MeasurementState.CheckingSensors
            _measurementPhase.value = if (LocalizationManager.isTeluguRaw()) 
                "సెన్సార్ల స్థితిని తనిఖీ చేస్తోంది..." 
                else "Verifying sensor probe self-check..."
            delay(800)

            val currentSensors = repository.liveSensorData.value
            _measurementState.value = MeasurementState.ReceivingPackets(currentSensors.packetsReceived)
            _measurementProgress.value = 25
            _measurementPhase.value = if (LocalizationManager.isTeluguRaw()) 
                "సెన్సార్ ప్యాకెట్లను స్వీకరిస్తోంది..." 
                else "Receiving sensor data packets..."

            // Wait for stability
            var stabilityWaitSeconds = 0
            var isStable = false
            while (stabilityWaitSeconds < 10 && !isStable) {
                delay(700)
                stabilityWaitSeconds++
                val sensorData = repository.liveSensorData.value
                val currentProgress = (25 + stabilityWaitSeconds * 6).coerceAtMost(80)
                _measurementProgress.value = currentProgress
                _measurementState.value = MeasurementState.WaitingForStability(currentProgress)
                _measurementPhase.value = if (LocalizationManager.isTeluguRaw()) 
                    "కొలతలు స్థిరపడేవరకు వేచి ఉండండి (${10 - stabilityWaitSeconds}s)..." 
                    else "Waiting for readings to stabilize (${10 - stabilityWaitSeconds}s)..."

                if (sensorData.isStable) {
                    isStable = true
                }
            }

            _measurementProgress.value = 90
            _measurementState.value = MeasurementState.StableValidating
            _measurementPhase.value = if (LocalizationManager.isTeluguRaw()) 
                "కొలతలు స్థిరంగా ఉన్నాయి. ఫలితం లెక్కిస్తోంది..." 
                else "Readings stable. Computing decision..."
            delay(500)

            val finalSensor = repository.liveSensorData.value
            val isDrying = _selectedMode.value == AssessmentMode.DRYING_READINESS
            val isSandbox = repository.isDeveloperSandboxEnabled.value
            val now = System.currentTimeMillis()

            val readingList = List(5) { i ->
                GrainReading(
                    timestamp = now - (4 - i) * 1000L,
                    moisture = if (finalSensor.moisture > 0f) finalSensor.moisture else null,
                    moistureMv = if (finalSensor.moistureSensorOk) 2100 else null,
                    t1Top = if (finalSensor.t1Top > 0f) finalSensor.t1Top else null,
                    t2Middle = if (finalSensor.t2Middle > 0f) finalSensor.t2Middle else null,
                    t3Bottom = if (finalSensor.t3Bottom > 0f) finalSensor.t3Bottom else null,
                    ambientHumidity = if (finalSensor.ambientHumidity > 0f) finalSensor.ambientHumidity else 65f,
                    isSimulated = isSandbox
                )
            }

            _measurementState.value = MeasurementState.ComputingDecision
            val decision = if (isDrying) {
                GrainDecisionEngine.evaluatePreStorage(readingList)
            } else {
                GrainDecisionEngine.evaluateStoredGrain(readingList)
            }

            if (decision.assessmentStatus == AssessmentStatus.SENSOR_FAULT ||
                decision.assessmentStatus == AssessmentStatus.STALE ||
                decision.assessmentStatus == AssessmentStatus.INSUFFICIENT_EVIDENCE
            ) {
                _measurementState.value = MeasurementState.Error(
                    messageEn = decision.subtitle,
                    messageTe = decision.subtitleTelugu
                )
                _measurementPhase.value = if (LocalizationManager.isTeluguRaw()) 
                    "లోపం: ${decision.headlineTelugu}" 
                    else "Error: ${decision.headline}"
                return@launch
            }

            _currentDecision.value = decision
            _measurementProgress.value = 100
            _measurementState.value = MeasurementState.Completed
            _measurementPhase.value = if (LocalizationManager.isTeluguRaw()) "విశ్లేషణ పూర్తయింది!" else "Analysis complete!"
            delay(400)

            onComplete(_selectedMode.value)
        }
    }

    fun saveCurrentInspection(onSaved: () -> Unit) {
        val decision = _currentDecision.value ?: return
        viewModelScope.launch {
            val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            val formattedDate = dateFormat.format(Date())

            val entity = GrainInspectionEntity(
                cropName = _selectedCrop.value.displayName,
                mode = if (_selectedMode.value == AssessmentMode.DRYING_READINESS) "Drying Check" else "Storage Check",
                moisture = decision.moisture,
                t1Top = decision.t1Top,
                t2Middle = decision.t2Middle,
                t3Bottom = decision.t3Bottom,
                humidity = decision.humidity,
                status = decision.status.name,
                riskScore = decision.bdrs,
                dominantRisk = decision.dominantRisk,
                recommendationsJson = decision.recommendedActions.joinToString("|"),
                timestamp = System.currentTimeMillis(),
                formattedDate = formattedDate
            )

            repository.saveInspection(entity)
            onSaved()
        }
    }

    fun loadBenchmarkData() {
        viewModelScope.launch {
            repository.loadBenchmarkData()
        }
    }

    suspend fun getInspectionById(id: Long): GrainInspectionEntity? {
        return repository.getInspectionById(id)
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
        }
    }

    fun resetMeasurementState() {
        _measurementState.value = MeasurementState.Idle
        _measurementProgress.value = 0
        _measurementPhase.value = ""
    }

    fun requestDeviceInfo() {
        repository.requestDeviceInfo()
    }

    fun requestHistorySync() {
        repository.requestHistorySync()
    }

    fun clearDeviceHistory() {
        repository.clearDeviceHistory()
    }

    fun pingProbe(): Boolean {
        return repository.pingProbe()
    }

    fun triggerTestBeep(): Boolean {
        return repository.triggerTestBeep()
    }

    fun syncToCloud(endpoint: String? = null) {
        viewModelScope.launch {
            repository.syncToCloud(endpoint)
        }
    }
}
