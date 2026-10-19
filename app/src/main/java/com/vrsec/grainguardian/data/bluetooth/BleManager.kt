package com.vrsec.grainguardian.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class ProbeSensorData(
    val moisture: Float = 0.0f,
    val t1Top: Float = 0.0f,
    val t2Middle: Float = 0.0f,
    val t3Bottom: Float = 0.0f,
    val ambientHumidity: Float = 0.0f,
    val batteryPercent: Int = 0,
    val isStable: Boolean = false,
    val packetsReceived: Int = 0,
    val moistureSensorOk: Boolean = false,
    val t1Ok: Boolean = false,
    val t2Ok: Boolean = false,
    val t3Ok: Boolean = false
)

data class DeviceInfo(
    val firmwareVersion: String = "2.0",
    val recordCount: Int = 0,
    val uptimeSeconds: Long = 0L,
    val discoveredProbes: Int = 3,
    val hasOled: Boolean = true
)

sealed interface SyncStatus {
    object Idle : SyncStatus
    data class Syncing(val current: Int, val total: Int) : SyncStatus
    data class Completed(val totalSynced: Int) : SyncStatus
    object Empty : SyncStatus
    object Cleared : SyncStatus
    data class Error(val message: String) : SyncStatus
}

data class HistoricalRecord(
    val seqId: Long,
    val uptimeSec: Long,
    val t1: Float,
    val t2: Float,
    val t3: Float,
    val moistureMv: Int,
    val moisture: Float,
    val humidity: Float,
    val statusFlags: Int,
    val timestamp: Long
)

sealed interface BleConnectionState {
    object BluetoothDisabled : BleConnectionState
    object PermissionsMissing : BleConnectionState
    object Disconnected : BleConnectionState
    object Scanning : BleConnectionState
    object DeviceNotFound : BleConnectionState
    data class Connecting(val deviceName: String) : BleConnectionState
    data class Connected(val deviceName: String, val deviceId: String, val battery: Int) : BleConnectionState
    data class SensorError(val message: String) : BleConnectionState
}

class BleManager(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _liveSensorData = MutableStateFlow(ProbeSensorData())
    val liveSensorData: StateFlow<ProbeSensorData> = _liveSensorData.asStateFlow()

    private val _deviceInfo = MutableStateFlow<DeviceInfo?>(null)
    val deviceInfo: StateFlow<DeviceInfo?> = _deviceInfo.asStateFlow()

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _historicalRecordsFlow = MutableSharedFlow<HistoricalRecord>(extraBufferCapacity = 64)
    val historicalRecordsFlow: SharedFlow<HistoricalRecord> = _historicalRecordsFlow.asSharedFlow()

    private val _isDeveloperSandboxEnabled = MutableStateFlow(false)
    val isDeveloperSandboxEnabled: StateFlow<Boolean> = _isDeveloperSandboxEnabled.asStateFlow()

    private var bluetoothGatt: BluetoothGatt? = null
    private var activeCharacteristic: BluetoothGattCharacteristic? = null
    private var scanner: BluetoothLeScanner? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var scanTimeoutRunnable: Runnable? = null
    private var sandboxJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Recent readings buffer for authentic stability checking
    private val recentMoistureReadings = mutableListOf<Float>()
    private val recentT1Readings = mutableListOf<Float>()
    private var packetCounter = 0

    // Persistent sensor accumulators across individual incoming BLE packets
    private var accumulatedMoisture: Float = 0.0f
    private var accumulatedT1: Float = 0.0f
    private var accumulatedT2: Float = 0.0f
    private var accumulatedT3: Float = 0.0f
    private var accumulatedHumidity: Float = 65.0f
    private var accumulatedBattery: Int = 85
    private var accumulatedMoistureRaw: Int = 0
    private var accumulatedMoistureMV: Int = 0

    // Timestamp reconstitution tracking
    private var lastDeviceUptimeSec: Long = 0L
    private var currentSyncWallClock: Long = 0L

    companion object {
        // Primary 128-bit UUIDs (Configured in physical ESP32 prototype)
        val PRIMARY_SERVICE_UUID: UUID = UUID.fromString("12345678-1234-1234-1234-1234567890AB")
        val PRIMARY_CHARACTERISTIC_UUID: UUID = UUID.fromString("12345678-1234-1234-1234-1234567890AC")

        // Fallback 16-bit / 128-bit Base UUIDs
        val FALLBACK_SERVICE_UUID: UUID = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB")
        val FALLBACK_CHARACTERISTIC_UUID: UUID = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB")

        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")
        const val SCAN_PERIOD_MS = 12000L

        /**
         * Converts analog millivolts from SEN0193 capacitive moisture probe into Paddy Moisture %
         * Standard calibration curve:
         * ~2100 mV corresponds to ~13.5% (Ideal storage baseline)
         * ~2600 mV corresponds to ~10.0% (Over-dry)
         * ~1600 mV corresponds to ~18.0% (Wet harvest)
         */
        fun convertMillivoltsToMoisture(mv: Int): Float {
            if (mv <= 0) return 0.0f
            val calculated = 13.5f + (2100f - mv.toFloat()) / 500f * 4.5f
            return calculated.coerceIn(8.0f, 32.0f)
        }
    }

    fun hasPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    fun setDeveloperSandboxMode(enabled: Boolean) {
        _isDeveloperSandboxEnabled.value = enabled
        if (!enabled && _connectionState.value is BleConnectionState.Connected) {
            disconnect()
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (_isDeveloperSandboxEnabled.value) {
            runSandboxScan()
            return
        }

        if (!hasPermissions()) {
            _connectionState.value = BleConnectionState.PermissionsMissing
            return
        }

        if (!isBluetoothEnabled()) {
            _connectionState.value = BleConnectionState.BluetoothDisabled
            return
        }

        scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            _connectionState.value = BleConnectionState.BluetoothDisabled
            return
        }

        _connectionState.value = BleConnectionState.Scanning

        val scanFilterPrimary = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(PRIMARY_SERVICE_UUID))
            .build()

        val scanFilterFallback = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(FALLBACK_SERVICE_UUID))
            .build()

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanTimeoutRunnable = Runnable {
            stopScan()
            if (_connectionState.value is BleConnectionState.Scanning) {
                _connectionState.value = BleConnectionState.DeviceNotFound
            }
        }
        mainHandler.postDelayed(scanTimeoutRunnable!!, SCAN_PERIOD_MS)

        try {
            scanner?.startScan(listOf(scanFilterPrimary, scanFilterFallback), scanSettings, scanCallback)
        } catch (_: Exception) {
            scanner?.startScan(scanCallback)
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        scanTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        scanTimeoutRunnable = null
        try {
            scanner?.stopScan(scanCallback)
        } catch (_: Exception) {}
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            val device = result?.device ?: return
            val name = device.name ?: result.scanRecord?.deviceName ?: ""

            val matchesService = result.scanRecord?.serviceUuids?.any {
                it.uuid == PRIMARY_SERVICE_UUID || it.uuid == FALLBACK_SERVICE_UUID
            } ?: false

            val matchesName = name.contains("GrainGuardian", ignoreCase = true) ||
                              name.contains("ESP32", ignoreCase = true) ||
                              name.contains("Probe", ignoreCase = true)

            if (matchesService || matchesName) {
                stopScan()
                connectToDevice(device)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            _connectionState.value = BleConnectionState.DeviceNotFound
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
        val name = device.name ?: "GrainGuardian"
        _connectionState.value = BleConnectionState.Connecting(name)
        bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            device.connectGatt(context, false, gattCallback)
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt?.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                activeCharacteristic = null
                _connectionState.value = BleConnectionState.Disconnected
                resetSensors()
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS && gatt != null) {
                // Find primary service, fallback service, or any service containing target characteristic
                val service = gatt.getService(PRIMARY_SERVICE_UUID)
                    ?: gatt.getService(FALLBACK_SERVICE_UUID)
                    ?: gatt.services.firstOrNull { s ->
                        s.getCharacteristic(PRIMARY_CHARACTERISTIC_UUID) != null ||
                        s.getCharacteristic(FALLBACK_CHARACTERISTIC_UUID) != null
                    }

                val characteristic = service?.getCharacteristic(PRIMARY_CHARACTERISTIC_UUID)
                    ?: service?.getCharacteristic(FALLBACK_CHARACTERISTIC_UUID)
                    ?: service?.characteristics?.firstOrNull { c ->
                        (c.properties and (BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_READ)) != 0
                    }

                if (characteristic != null) {
                    activeCharacteristic = characteristic
                    gatt.setCharacteristicNotification(characteristic, true)
                    val descriptor = characteristic.getDescriptor(CCCD_UUID)
                    if (descriptor != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                        } else {
                            @Suppress("DEPRECATION")
                            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            @Suppress("DEPRECATION")
                            gatt.writeDescriptor(descriptor)
                        }
                    }
                    val deviceName = gatt.device.name ?: "GrainGuardian"
                    _connectionState.value = BleConnectionState.Connected(
                        deviceName = deviceName,
                        deviceId = gatt.device.address,
                        battery = if (accumulatedBattery > 0) accumulatedBattery else 85
                    )
                    // Request device status on connect
                    mainHandler.postDelayed({
                        requestDeviceInfo()
                    }, 500)
                } else {
                    _connectionState.value = BleConnectionState.SensorError("Probe service characteristic not found")
                }
            } else {
                _connectionState.value = BleConnectionState.SensorError("Failed to discover probe services")
            }
        }

        // Android 13+ (API 33+) Callback
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            parseSensorPacket(value)
        }

        // Android 12 and below Callback
        @Deprecated("Used for Android 12 and lower")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            @Suppress("DEPRECATION")
            characteristic?.value?.let { bytes ->
                parseSensorPacket(bytes)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun sendCommand(command: String): Boolean {
        if (_isDeveloperSandboxEnabled.value) {
            handleSandboxCommand(command)
            return true
        }
        val gatt = bluetoothGatt ?: return false
        val char = activeCharacteristic ?: return false
        val bytes = command.toByteArray(Charsets.UTF_8)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val result = gatt.writeCharacteristic(char, bytes, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
            result == BluetoothStatusCodes.SUCCESS
        } else {
            @Suppress("DEPRECATION")
            char.value = bytes
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(char)
        }
    }

    fun requestDeviceInfo() {
        sendCommand("CMD:INFO\n")
    }

    fun requestHistorySync() {
        currentSyncWallClock = System.currentTimeMillis()
        sendCommand("CMD:SYNC\n")
    }

    fun clearDeviceHistory() {
        sendCommand("CMD:CLEAR_HISTORY\n")
    }

    fun pingDevice(): Boolean {
        return sendCommand("CMD:PING\n")
    }

    fun triggerTestBeep(): Boolean {
        return sendCommand("CMD:BEEP\n")
    }

    fun parseSensorPacket(bytes: ByteArray) {
        try {
            val text = String(bytes, Charsets.UTF_8).trim()
            if (text.isEmpty() || text == "GrainGuardian Ready") return

            if (text == "PONG" || text == "BEEP:OK") {
                return
            }

            // 1. INFO Packet: INFO:VER=2.0;RECS=5;UP=12400;PROBES=3;OLED=1
            if (text.startsWith("INFO:")) {
                val body = text.substring(5)
                var ver = "2.0"
                var recs = 0
                var up = 0L
                var probes = 3
                var oled = true
                val parts = body.split(";")
                for (part in parts) {
                    val kv = part.split("=")
                    if (kv.size == 2) {
                        when (kv[0].trim().uppercase()) {
                            "VER" -> ver = kv[1].trim()
                            "RECS" -> recs = kv[1].trim().toIntOrNull() ?: 0
                            "UP" -> up = kv[1].trim().toLongOrNull() ?: 0L
                            "PROBES" -> probes = kv[1].trim().toIntOrNull() ?: 3
                            "OLED" -> oled = kv[1].trim() == "1"
                        }
                    }
                }
                lastDeviceUptimeSec = up
                currentSyncWallClock = System.currentTimeMillis()
                _deviceInfo.value = DeviceInfo(
                    firmwareVersion = ver,
                    recordCount = recs,
                    uptimeSeconds = up,
                    discoveredProbes = probes,
                    hasOled = oled
                )
                return
            }

            // 2. SYNC:START,TOTAL=...
            if (text.startsWith("SYNC:START")) {
                val total = text.substringAfter("TOTAL=", "0").trim().toIntOrNull() ?: 0
                currentSyncWallClock = System.currentTimeMillis()
                _syncStatus.value = SyncStatus.Syncing(current = 0, total = total)
                return
            }

            // 3. SYNC:END,SENT=...
            if (text.startsWith("SYNC:END")) {
                val count = text.substringAfter("SENT=", "0").trim().toIntOrNull() ?: 0
                _syncStatus.value = SyncStatus.Completed(totalSynced = count)
                _deviceInfo.value = _deviceInfo.value?.copy(recordCount = 0)
                return
            }

            // 4. SYNC:EMPTY
            if (text == "SYNC:EMPTY") {
                _syncStatus.value = SyncStatus.Empty
                return
            }

            // 5. CLEAR:OK
            if (text == "CLEAR:OK") {
                _syncStatus.value = SyncStatus.Cleared
                _deviceInfo.value = _deviceInfo.value?.copy(recordCount = 0)
                return
            }

            // 6. HIST:SEQ=1,UP=2400,T1=28.5,T2=29.1,T3=29.8,MV=2110,H=64,F=31
            if (text.startsWith("HIST:")) {
                val body = text.substring(5)
                val items = body.split(",")
                var seq: Long = 0L
                var up: Long = 0L
                var t1 = 0f
                var t2 = 0f
                var t3 = 0f
                var mv = 0
                var h = 65f
                var flags = 0

                for (item in items) {
                    val kv = item.split("=")
                    if (kv.size == 2) {
                        val key = kv[0].trim().uppercase()
                        val v = kv[1].trim()
                        when (key) {
                            "SEQ" -> seq = v.toLongOrNull() ?: 0L
                            "UP" -> up = v.toLongOrNull() ?: 0L
                            "T1" -> if (!v.equals("ERR", true)) t1 = v.toFloatOrNull() ?: 0f
                            "T2" -> if (!v.equals("ERR", true)) t2 = v.toFloatOrNull() ?: 0f
                            "T3" -> if (!v.equals("ERR", true)) t3 = v.toFloatOrNull() ?: 0f
                            "MV" -> mv = v.toIntOrNull() ?: 0
                            "H" -> h = v.toFloatOrNull() ?: 65f
                            "F" -> flags = v.toIntOrNull() ?: 0
                        }
                    }
                }

                val calculatedMoisture = convertMillivoltsToMoisture(mv)
                val recordTimestamp = if (lastDeviceUptimeSec > 0L && currentSyncWallClock > 0L) {
                    val diffSec = lastDeviceUptimeSec - up
                    if (diffSec >= 0) currentSyncWallClock - (diffSec * 1000L) else currentSyncWallClock
                } else {
                    System.currentTimeMillis()
                }

                val record = HistoricalRecord(
                    seqId = seq,
                    uptimeSec = up,
                    t1 = t1,
                    t2 = t2,
                    t3 = t3,
                    moistureMv = mv,
                    moisture = calculatedMoisture,
                    humidity = h,
                    statusFlags = flags,
                    timestamp = recordTimestamp
                )

                coroutineScope.launch {
                    _historicalRecordsFlow.emit(record)
                }

                val current = _syncStatus.value
                if (current is SyncStatus.Syncing) {
                    _syncStatus.value = current.copy(current = current.current + 1)
                }
                return
            }

            // 7. Live Telemetry stream: supports single-item transmissions (T1=25.38) and multi-item delimited strings
            val items = text.split(Regex("[,;\n\r]+"))
            var updated = false

            for (item in items) {
                val trimmed = item.trim()
                if (trimmed.isEmpty()) continue

                val parts = trimmed.split(Regex("[:=]"), 2)
                if (parts.size == 2) {
                    val key = parts[0].trim().uppercase()
                    val valueStr = parts[1].trim()

                    if (valueStr.equals("ERR", ignoreCase = true)) {
                        continue
                    }

                    when (key) {
                        "T1" -> {
                            valueStr.toFloatOrNull()?.let {
                                accumulatedT1 = it
                                updated = true
                            }
                        }
                        "T2" -> {
                            valueStr.toFloatOrNull()?.let {
                                accumulatedT2 = it
                                updated = true
                            }
                        }
                        "T3" -> {
                            valueStr.toFloatOrNull()?.let {
                                accumulatedT3 = it
                                updated = true
                            }
                        }
                        "AIR", "AT" -> {
                            // Ambient air temperature
                        }
                        "HUM", "H" -> {
                            valueStr.toFloatOrNull()?.let {
                                accumulatedHumidity = it
                                updated = true
                            }
                        }
                        "M" -> {
                            valueStr.toFloatOrNull()?.let {
                                accumulatedMoisture = it
                                updated = true
                            }
                        }
                        "MV" -> {
                            valueStr.toIntOrNull()?.let { mv ->
                                accumulatedMoistureMV = mv
                                accumulatedMoisture = convertMillivoltsToMoisture(mv)
                                updated = true
                            }
                        }
                        "MR" -> {
                            valueStr.toIntOrNull()?.let { mr ->
                                accumulatedMoistureRaw = mr
                                if (accumulatedMoistureMV == 0) {
                                    val approxMv = (mr * 3300) / 4095
                                    accumulatedMoisture = convertMillivoltsToMoisture(approxMv)
                                    updated = true
                                }
                            }
                        }
                        "B", "BAT" -> {
                            valueStr.toIntOrNull()?.let {
                                accumulatedBattery = it
                                updated = true
                            }
                        }
                    }
                }
            }

            if (updated) {
                packetCounter++
                updateLiveReadings(
                    m = accumulatedMoisture,
                    t1 = accumulatedT1,
                    t2 = accumulatedT2,
                    t3 = accumulatedT3,
                    h = accumulatedHumidity,
                    b = accumulatedBattery
                )
            }
        } catch (_: Exception) {}
    }

    private fun updateLiveReadings(m: Float, t1: Float, t2: Float, t3: Float, h: Float, b: Int) {
        if (m > 0f) {
            recentMoistureReadings.add(m)
            if (recentMoistureReadings.size > 5) recentMoistureReadings.removeAt(0)
        }
        if (t1 > 0f) {
            recentT1Readings.add(t1)
            if (recentT1Readings.size > 5) recentT1Readings.removeAt(0)
        }

        // Authentic stability detection: checks last 4 valid readings
        val isStable = if (recentMoistureReadings.size >= 4 && recentT1Readings.size >= 4) {
            val mMax = recentMoistureReadings.maxOrNull() ?: 0f
            val mMin = recentMoistureReadings.minOrNull() ?: 0f
            val tMax = recentT1Readings.maxOrNull() ?: 0f
            val tMin = recentT1Readings.minOrNull() ?: 0f
            (mMax - mMin <= 0.35f) && (tMax - tMin <= 0.45f)
        } else false

        _liveSensorData.value = ProbeSensorData(
            moisture = m,
            t1Top = t1,
            t2Middle = t2,
            t3Bottom = t3,
            ambientHumidity = h,
            batteryPercent = b,
            isStable = isStable,
            packetsReceived = packetCounter,
            moistureSensorOk = m > 0.0f,
            t1Ok = t1 > 0.0f,
            t2Ok = t2 > 0.0f,
            t3Ok = t3 > 0.0f
        )
    }

    private fun resetSensors() {
        packetCounter = 0
        recentMoistureReadings.clear()
        recentT1Readings.clear()
        accumulatedMoisture = 0.0f
        accumulatedT1 = 0.0f
        accumulatedT2 = 0.0f
        accumulatedT3 = 0.0f
        accumulatedHumidity = 65.0f
        accumulatedBattery = 85
        accumulatedMoistureRaw = 0
        accumulatedMoistureMV = 0
        _syncStatus.value = SyncStatus.Idle
        _liveSensorData.value = ProbeSensorData()
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        stopScan()
        sandboxJob?.cancel()
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        activeCharacteristic = null
        _connectionState.value = BleConnectionState.Disconnected
        resetSensors()
    }

    // Developer Sandbox Emulation (Off by default, used for testing without hardware)
    private fun runSandboxScan() {
        coroutineScope.launch {
            _connectionState.value = BleConnectionState.Scanning
            delay(2000)
            _connectionState.value = BleConnectionState.Connected(
                deviceName = "GrainGuardian (Sandbox)",
                deviceId = "GG-01-A0CD",
                battery = 84
            )
            _deviceInfo.value = DeviceInfo(firmwareVersion = "2.0", recordCount = 3, uptimeSeconds = 3600L)
            startSandboxStreaming()
        }
    }

    private fun handleSandboxCommand(command: String) {
        coroutineScope.launch {
            val trimmed = command.trim()
            when (trimmed) {
                "CMD:INFO" -> {
                    delay(150)
                    parseSensorPacket("INFO:VER=2.0;RECS=3;UP=3600;PROBES=3;OLED=1".toByteArray(Charsets.UTF_8))
                }
                "CMD:PING" -> {
                    delay(50)
                    parseSensorPacket("PONG".toByteArray(Charsets.UTF_8))
                }
                "CMD:BEEP" -> {
                    delay(50)
                    parseSensorPacket("BEEP:OK".toByteArray(Charsets.UTF_8))
                }
                "CMD:SYNC" -> {
                    delay(150)
                    parseSensorPacket("SYNC:START,TOTAL=3".toByteArray(Charsets.UTF_8))
                    delay(150)
                    parseSensorPacket("HIST:SEQ=1,UP=2400,T1=28.5,T2=29.1,T3=29.8,MV=2110,H=64.0,F=31".toByteArray(Charsets.UTF_8))
                    delay(150)
                    parseSensorPacket("HIST:SEQ=2,UP=3000,T1=28.7,T2=29.3,T3=30.0,MV=2090,H=63.5,F=31".toByteArray(Charsets.UTF_8))
                    delay(150)
                    parseSensorPacket("HIST:SEQ=3,UP=3600,T1=28.9,T2=29.5,T3=30.2,MV=2080,H=63.0,F=31".toByteArray(Charsets.UTF_8))
                    delay(100)
                    parseSensorPacket("SYNC:END,SENT=3".toByteArray(Charsets.UTF_8))
                }
                "CMD:CLEAR_HISTORY" -> {
                    delay(150)
                    parseSensorPacket("CLEAR:OK".toByteArray(Charsets.UTF_8))
                }
            }
        }
    }

    private fun startSandboxStreaming() {
        sandboxJob?.cancel()
        sandboxJob = coroutineScope.launch {
            var counter = 0
            var currentM = 13.6f
            while (isActive) {
                delay(600)
                counter++
                if (counter > 5) currentM = 13.2f
                updateLiveReadings(
                    m = currentM,
                    t1 = 29.2f,
                    t2 = 30.5f,
                    t3 = 31.8f,
                    h = 66f,
                    b = 84
                )
            }
        }
    }
}
