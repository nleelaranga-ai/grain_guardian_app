package com.vrsec.grainguardian.data.model

enum class CropType(val displayName: String, val teluguName: String, val safeMoistureThreshold: Float) {
    PADDY("Paddy", "వరి (వరి ధాన్యం)", 13.5f),
    MAIZE("Maize", "మొక్కజొన్న", 14.0f),
    WHEAT("Wheat", "గోధుమ", 12.0f),
    PULSES("Pulses", "పప్పు ధాన్యాలు", 10.0f),
    OTHER("Other / Add Crop", "ఇతర పంటలు", 13.0f);

    companion object {
        fun fromName(name: String): CropType {
            return values().firstOrNull { it.name.equals(name, ignoreCase = true) || it.displayName.equals(name, ignoreCase = true) } ?: PADDY
        }
    }
}
