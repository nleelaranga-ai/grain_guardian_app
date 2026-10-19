package com.vrsec.grainguardian.domain.engine

/**
 * Global assessment outcome status as defined in GrainGuardian FES v1.0.
 *
 * Evaluation Order:
 * 1. Physical validation (range, presence, timestamp) -> SENSOR_FAULT
 * 2. Freshness check against maxReadingAgeMinutes -> STALE
 * 3. Reading count and stability check -> INSUFFICIENT_EVIDENCE
 * 4. Agronomic rules -> ATTENTION or NORMAL
 */
enum class AssessmentStatus {
    NORMAL,
    ATTENTION,
    SENSOR_FAULT,
    STALE,
    INSUFFICIENT_EVIDENCE
}

/**
 * Connectivity state is an operational flag, NEVER an agronomic assessment value.
 * Losing internet must never change a grain assessment result.
 */
enum class ConnectivityState {
    BLE_CONNECTED_ONLINE,
    BLE_CONNECTED_OFFLINE,
    DEVICE_DISCONNECTED_LOCAL_AVAILABLE,
    NO_VALID_LOCAL_DATA
}
