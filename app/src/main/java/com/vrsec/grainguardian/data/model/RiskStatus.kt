package com.vrsec.grainguardian.data.model

enum class RiskStatus(val label: String, val teluguLabel: String) {
    SAFE("SAFE", "సురక్షితం"),
    WARNING("WARNING", "హెచ్చరిక"),
    HIGH_RISK("HIGH RISK", "అత్యధిక ప్రమాదం")
}
