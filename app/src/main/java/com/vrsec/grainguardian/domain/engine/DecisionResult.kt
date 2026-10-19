package com.vrsec.grainguardian.domain.engine

import com.vrsec.grainguardian.data.model.RiskStatus

data class DecisionResult(
    val assessmentStatus: AssessmentStatus = AssessmentStatus.NORMAL,
    val reasonCode: String? = null,
    val contributingReadings: List<GrainReading> = emptyList(),
    val ruleVersion: String = "0.1.0-unvalidated",
    val evaluatedAt: Long = System.currentTimeMillis(),
    val source: String = "REAL", // "REAL" or "SIMULATED"
    val status: RiskStatus = RiskStatus.SAFE,
    val moisture: Float = 0f,
    val isMoistureSafe: Boolean = true,
    val t1Top: Float = 0f,
    val t2Middle: Float = 0f,
    val t3Bottom: Float = 0f,
    val humidity: Float = 0f,
    val tgi: Float = 0f, // Temperature Gradient Index
    val bdrs: Int = 0, // Biological Degradation Risk Score 0-100
    val gshi: Int = 100, // Grain Storage Health Index 0-100
    val dominantRisk: String = "",
    val dominantRiskTelugu: String = "",
    val whyReasons: List<String> = emptyList(),
    val whyReasonsTelugu: List<String> = emptyList(),
    val recommendedActions: List<String> = emptyList(),
    val recommendedActionsTelugu: List<String> = emptyList(),
    val headline: String = "",
    val headlineTelugu: String = "",
    val subtitle: String = "",
    val subtitleTelugu: String = "",
    val isSimulated: Boolean = false
)
