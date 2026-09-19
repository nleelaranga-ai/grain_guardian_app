package com.vrsec.grainguardian.domain.engine

import com.vrsec.grainguardian.data.model.RiskStatus

data class DecisionResult(
    val status: RiskStatus,
    val moisture: Float,
    val isMoistureSafe: Boolean,
    val t1Top: Float,
    val t2Middle: Float,
    val t3Bottom: Float,
    val humidity: Float,
    val tgi: Float, // Temperature Gradient Index
    val bdrs: Int, // Biological Degradation Risk Score 0-100
    val gshi: Int, // Grain Storage Health Index 0-100
    val dominantRisk: String,
    val dominantRiskTelugu: String,
    val whyReasons: List<String>,
    val whyReasonsTelugu: List<String>,
    val recommendedActions: List<String>,
    val recommendedActionsTelugu: List<String>,
    val headline: String,
    val headlineTelugu: String,
    val subtitle: String,
    val subtitleTelugu: String
)
