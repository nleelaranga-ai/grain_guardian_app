package com.vrsec.grainguardian.data.model

enum class AssessmentMode(val title: String, val teluguTitle: String, val subtitle: String) {
    DRYING_READINESS(
        title = "DRYING READINESS",
        teluguTitle = "ఎండబెట్టడం సంసిద్ధత",
        subtitle = "Is the grain dry enough to store?"
    ),
    STORAGE_HEALTH(
        title = "STORAGE HEALTH",
        teluguTitle = "నిల్వ ఆరోగ్యం",
        subtitle = "Is my stored grain still safe?"
    )
}
