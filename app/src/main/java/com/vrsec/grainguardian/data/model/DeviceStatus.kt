package com.vrsec.grainguardian.data.model

data class DeviceStatus(
    val deviceName: String = "GrainGuardian Probe",
    val deviceId: String = "--",
    val batteryPercent: Int = 0,
    val firmwareVersion: String = "1.0.3",
    val isConnected: Boolean = false,
    val isScanning: Boolean = false,
    val moistureSensorOk: Boolean = false,
    val tempSensor1Ok: Boolean = false,
    val tempSensor2Ok: Boolean = false,
    val tempSensor3Ok: Boolean = false,
    val errorMessage: String? = null
)
