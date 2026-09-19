package com.vrsec.grainguardian.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inspections")
data class GrainInspectionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cropName: String = "Paddy",
    val mode: String = "Storage Check", // "Drying Check" or "Storage Check"
    val moisture: Float = 13.2f,
    val t1Top: Float = 29.0f,
    val t2Middle: Float = 31.0f,
    val t3Bottom: Float = 32.0f,
    val humidity: Float = 68.0f,
    val status: String = "SAFE", // SAFE, WARNING, HIGH_RISK
    val riskScore: Int = 18, // 0 - 100
    val dominantRisk: String = "Moisture Optimal",
    val recommendationsJson: String = "Spread grain evenly|Store in dry place",
    val timestamp: Long = System.currentTimeMillis(),
    val formattedDate: String = "Today, 10:30 AM"
)
