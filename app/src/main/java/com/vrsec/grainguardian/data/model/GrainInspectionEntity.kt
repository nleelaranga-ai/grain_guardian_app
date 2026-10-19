package com.vrsec.grainguardian.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "inspections",
    indices = [Index(value = ["uuid"], unique = true)]
)
data class GrainInspectionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val deviceId: String? = null,
    val sessionId: String? = null,
    val syncState: String = "LOCAL", // "LOCAL", "PENDING", "CONFIRMED"
    val source: String = "REAL",     // "REAL", "SIMULATED"
    val ruleVersion: String? = "0.1.0-unvalidated",
    val statusCode: String? = "NORMAL",
    val reasonCode: String? = null,
    val cropName: String = "Paddy",
    val mode: String = "Storage Check", // "Drying Check" or "Storage Check"
    val moisture: Float = 0.0f,
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
