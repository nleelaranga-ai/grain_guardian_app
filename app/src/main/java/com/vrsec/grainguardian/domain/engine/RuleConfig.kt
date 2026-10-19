package com.vrsec.grainguardian.domain.engine

import org.json.JSONObject

/**
 * Versioned agronomic rules configuration.
 * NOTE: All thresholds are flagged UNVALIDATED until verified by owner with official agricultural source.
 */
data class PaddyRuleConfig(
    val moistureTargetPercent: Float = 13.5f,      // UNVALIDATED: standard paddy safe storage threshold
    val thermalGradientWarnC: Float = 3.0f,         // UNVALIDATED: maximum safe top-to-bottom temperature gradient
    val maxTemperatureWarnC: Float = 35.0f,         // UNVALIDATED: maximum ambient/grain temperature before hotspot warning
    val maxReadingAgeMinutes: Int = 30,             // UNVALIDATED: maximum reading staleness window
    val minValidReadings: Int = 5,                  // UNVALIDATED: stability filter requirement
    val validated: Boolean = false                  // MUST remain false until verified with agronomic citations
)

data class RuleConfig(
    val ruleVersion: String = "0.1.0-unvalidated",
    val paddy: PaddyRuleConfig = PaddyRuleConfig()
) {
    companion object {
        val DEFAULT = RuleConfig()

        fun fromJson(jsonStr: String): RuleConfig {
            return try {
                val obj = JSONObject(jsonStr)
                val version = obj.optString("ruleVersion", "0.1.0-unvalidated")
                val paddyObj = obj.optJSONObject("paddy")
                val paddyConfig = if (paddyObj != null) {
                    PaddyRuleConfig(
                        moistureTargetPercent = paddyObj.optDouble("moistureTargetPercent", 13.5).toFloat(),
                        thermalGradientWarnC = paddyObj.optDouble("thermalGradientWarnC", 3.0).toFloat(),
                        maxTemperatureWarnC = paddyObj.optDouble("maxTemperatureWarnC", 35.0).toFloat(),
                        maxReadingAgeMinutes = paddyObj.optInt("maxReadingAgeMinutes", 30),
                        minValidReadings = paddyObj.optInt("minValidReadings", 5),
                        validated = paddyObj.optBoolean("validated", false)
                    )
                } else {
                    PaddyRuleConfig()
                }
                RuleConfig(ruleVersion = version, paddy = paddyConfig)
            } catch (e: Exception) {
                DEFAULT
            }
        }
    }
}
