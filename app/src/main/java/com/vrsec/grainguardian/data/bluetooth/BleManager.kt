package com.vrsec.grainguardian.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import kotlin.math.abs

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

    private val _isDeveloperSandboxEnabled = MutableStateFlow(false)
    val isDeveloperSandboxEnabled: StateFlow<Boolean> = _isDeveloperSandboxEnabled.asStateFlow()

    private var bluetoothGatt: BluetoothGatt? = null
    private var scanner: BluetoothLeScanner? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var scanTimeoutRunnable: Runnable? = null
    private var sandboxJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Recent readings buffer for authentic stability checking
    private val recentMoistureReadings = mutableListOf<Float>()
    private val recentT1Readings = mutableListOf<Float>()
    private var packetCounter = 0

    companion object {
        val SERVICE_UUID: UUID = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB")
        val CHARACTERISTIC_UUID: UUID = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB")
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")
        const val SCAN_PERIOD_MS = 10000L
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

        if (!isBluetoothEnabled()) {
            _connectionState.value = BleConnectionState.BluetoothDisabled
            return
        }

        if (!hasPermissions()) {
            _connectionState.value = BleConnectionState.PermissionsMissing
            return
        }

        scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            _connectionState.value = BleConnectionState.DeviceNotFound
            return
        }

        _connectionState.value = BleConnectionState.Scanning

        scanTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        scanTimeoutRunnable = Runnable {
            stopScan()
            if (_connectionState.value is BleConnectionState.Scanning) {
                _connectionState.value = BleConnectionState.DeviceNotFound
            }
        }
        mainHandler.postDelayed(scanTimeoutRunnable!!, SCAN_PERIOD_MS)

        val filter = ScanFilter.Builder()
            .setDeviceName("GrainGuardian-01")
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            scanner?.startScan(listOf(filter), settings, scanCallback)
        } catch (e: Exception) {
            _connectionState.value = BleConnectionState.DeviceNotFound
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        scanTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        try {
            scanner?.stopScan(scanCallback)
        } catch (_: Exception) {}
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let { device ->
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
        _connectionState.value = BleConnectionState.Connecting(device.name ?: "GrainGuardian Probe")
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt?.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                _connectionState.value = BleConnectionState.Disconnected
                resetSensors()
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS && gatt != null) {
                val service = gatt.getService(SERVICE_UUID)
                val characteristic = service?.getCharacteristic(CHARACTERISTIC_UUID)
                if (characteristic != null) {
                    gatt.setCharacteristicNotification(characteristic, true)
                    val descriptor = characteristic.getDescriptor(CCCD_UUID)
                    descriptor?.let {
                        it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        gatt.writeDescriptor(it)
                    }
                    val deviceName = gatt.device.name ?: "GrainGuardian-01"
                    _connectionState.value = BleConnectionState.Connected(
                        deviceName = deviceName,
                        deviceId = gatt.device.address,
                        battery = _liveSensorData.value.batteryPercent
                    )
                } else {
                    _connectionState.value = BleConnectionState.SensorError("Probe service characteristic not found")
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            characteristic?.value?.let { bytes ->
                parseSensorPacket(bytes)
            }
        }
    }

    private fun parseSensorPacket(bytes: ByteArray) {
        try {
            val text = String(bytes, Charsets.UTF_8).trim()
            // Format: M:13.4,T1:28.9,T2:30.1,T3:31.4,H:66,B:84
            val parts = text.split(",")
            var m = 0f; var t1 = 0f; var t2 = 0f; var t3 = 0f; var h = 65f; var b = 80
            for (part in parts) {
                val kv = part.split(":")
                if (kv.size == 2) {
                    when (kv[0].trim().uppercase()) {
                        "M" -> m = kv[1].trim().toFloatOrNull() ?: 0f
                        "T1" -> t1 = kv[1].trim().toFloatOrNull() ?: 0f
                        "T2" -> t2 = kv[1].trim().toFloatOrNull() ?: 0f
                        "T3" -> t3 = kv[1].trim().toFloatOrNull() ?: 0f
                        "H" -> h = kv[1].trim().toFloatOrNull() ?: 65f
                        "B" -> b = kv[1].trim().toIntOrNull() ?: 80
                    }
                }
            }

            packetCounter++
            updateLiveReadings(m, t1, t2, t3, h, b)
        } catch (_: Exception) {}
    }

    private fun updateLiveReadings(m: Float, t1: Float, t2: Float, t3: Float, h: Float, b: Int) {
        recentMoistureReadings.add(m)
        recentT1Readings.add(t1)
        if (recentMoistureReadings.size > 5) recentMoistureReadings.removeAt(0)
        if (recentT1Readings.size > 5) recentT1Readings.removeAt(0)

        // Authentic stability detection
        val isStable = if (recentMoistureReadings.size >= 4) {
            val mMax = recentMoistureReadings.maxOrNull() ?: 0f
            val mMin = recentMoistureReadings.minOrNull() ?: 0f
            val tMax = recentT1Readings.maxOrNull() ?: 0f
            val tMin = recentT1Readings.minOrNull() ?: 0f
            (mMax - mMin <= 0.25f) && (tMax - tMin <= 0.35f)
        } else false

        val sensorsOk = m > 0.0f && t1 > 0.0f && t2 > 0.0f && t3 > 0.0f

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
        _liveSensorData.value = ProbeSensorData()
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        stopScan()
        sandboxJob?.cancel()
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        _connectionState.value = BleConnectionState.Disconnected
        resetSensors()
    }

    // Developer Sandbox Emulation (Off by default, used for development without hardware)
    private fun runSandboxScan() {
        coroutineScope.launch {
            _connectionState.value = BleConnectionState.Scanning
            delay(2000)
            _connectionState.value = BleConnectionState.Connected(
                deviceName = "GrainGuardian-01 (Sandbox)",
                deviceId = "GG-01-A0CD",
                battery = 84
            )
            startSandboxStreaming()
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
                // Gradually settles to a stable reading
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
