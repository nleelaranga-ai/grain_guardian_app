package com.vrsec.grainguardian.domain.engine

/**
 * Validated physical sensor reading packet with strict physical bounds checking.
 * A disconnected sensor, 0 mV reading, or Dallas -127.0°C error token will fail physical validity.
 */
data class GrainReading(
    val timestamp: Long,
    val moisture: Float?,               // % moisture, null if missing/disconnected
    val moistureMv: Int? = null,        // raw analog millivolts from SEN0193
    val t1Top: Float? = null,           // DS18B20 10cm depth probe
    val t2Middle: Float? = null,        // DS18B20 40cm depth probe
    val t3Bottom: Float? = null,        // DS18B20 80cm depth probe
    val ambientHumidity: Float? = null, // DHT ambient humidity
    val ambientTemp: Float? = null,     // DHT ambient temperature
    val isSimulated: Boolean = false
) {
    fun isMoisturePhysicallyValid(): Boolean {
        if (moisture == null) return false
        if (moistureMv != null && moistureMv <= 0) return false
        // Zero or negative moisture is physically impossible and indicates open-circuit or uncalibrated probe
        if (moisture <= 0f) return false
        return moisture in 8.0f..35.0f
    }

    fun isT1Valid(): Boolean = t1Top != null && t1Top in -10.0f..85.0f && t1Top != -127.0f
    fun isT2Valid(): Boolean = t2Middle != null && t2Middle in -10.0f..85.0f && t2Middle != -127.0f
    fun isT3Valid(): Boolean = t3Bottom != null && t3Bottom in -10.0f..85.0f && t3Bottom != -127.0f

    fun areStorageTempsValid(): Boolean = isT1Valid() && isT2Valid() && isT3Valid()
}
