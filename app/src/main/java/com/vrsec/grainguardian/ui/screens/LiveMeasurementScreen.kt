package com.vrsec.grainguardian.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vrsec.grainguardian.data.model.AssessmentMode
import com.vrsec.grainguardian.domain.localization.LocalizationManager
import com.vrsec.grainguardian.ui.components.CircularGauge
import com.vrsec.grainguardian.ui.components.GrainTopBar
import com.vrsec.grainguardian.ui.components.PrimaryActionButton
import com.vrsec.grainguardian.ui.theme.*
import com.vrsec.grainguardian.viewmodel.GrainViewModel
import com.vrsec.grainguardian.viewmodel.MeasurementState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveMeasurementScreen(
    viewModel: GrainViewModel,
    onMeasurementComplete: (AssessmentMode) -> Unit,
    onConnectProbeClick: () -> Unit
) {
    val isTelugu = LocalizationManager.isTelugu()
    val isConnected by viewModel.isProbeConnected.collectAsState()
    val measurementState by viewModel.measurementState.collectAsState()
    val progress by viewModel.measurementProgress.collectAsState()
    val phaseText by viewModel.measurementPhase.collectAsState()
    val moistureDisplay by viewModel.liveMoistureDisplay.collectAsState()
    val tempDisplay by viewModel.liveTempDisplay.collectAsState()
    val sensorData by viewModel.liveSensorData.collectAsState()

    LaunchedEffect(isConnected) {
        if (isConnected) {
            viewModel.startLiveMeasurement { mode ->
                onMeasurementComplete(mode)
            }
        }
    }

    Scaffold(
        topBar = {
            GrainTopBar(
                title = if (isTelugu) "ప్రత్యక్ష సెన్సార్ డేటా" else "Live Sensor Stream",
                showBackButton = false
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(GrainBackground)
                .padding(paddingValues)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            if (!isConnected) {
                // Truthful Error State: Hardware Not Connected
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEE2E2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = GrainDangerRed,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = if (isTelugu) "హార్డ్‌వేర్ ప్రోబ్ కనెక్ట్ కాలేదు" else "Probe Not Connected",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = GrainTextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isTelugu) "నిజమైన సెన్సార్ హార్డ్‌వేర్ లేకుండా కొలత సాధ్యం కాదు. దయచేసి ప్రోబ్‌ను కనెక్ట్ చేయండి." 
                        else "Measurement requires real sensor hardware. Please connect your GrainGuardian probe.",
                        fontSize = 14.sp,
                        color = GrainTextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }

                PrimaryActionButton(
                    text = if (isTelugu) "ప్రోబ్‌ను కనెక్ట్ చేయండి" else "Connect Probe",
                    onClick = onConnectProbeClick,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            } else {
                // Live Stream View
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Stability Progress Gauge
                    CircularGauge(
                        progressPercent = progress,
                        size = 170.dp,
                        strokeWidth = 12.dp,
                        label = if (isTelugu) "స్థిరత్వం" else "Stability"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = phaseText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GrainPrimaryGreen,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Multi-Depth Live Readouts
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = GrainSurface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isTelugu) "సెన్సార్ ప్యాకెట్లు (లైవ్)" else "SENSOR PACKETS (LIVE)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GrainTextSecondary,
                                    letterSpacing = 1.sp
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(GrainPrimaryGreen)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${sensorData.packetsReceived} pkts",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GrainPrimaryGreen
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Divider(color = GrainDivider)
                            Spacer(modifier = Modifier.height(14.dp))

                            // Moisture & Temps
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = if (isTelugu) "తేమ శాతం" else "Moisture",
                                        fontSize = 12.sp,
                                        color = GrainTextSecondary
                                    )
                                    Text(
                                        text = if (sensorData.moisture > 0f) "${String.format("%.1f", sensorData.moisture)}%" else "--",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GrainPrimaryGreen
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (isTelugu) "ఉష్ణోగ్రత (3 పొరలు)" else "Multi-Depth Temp",
                                        fontSize = 12.sp,
                                        color = GrainTextSecondary
                                    )
                                    Text(
                                        text = if (sensorData.t1Top > 0f) 
                                            "T1: ${String.format("%.1f", sensorData.t1Top)}° • T2: ${String.format("%.1f", sensorData.t2Middle)}° • T3: ${String.format("%.1f", sensorData.t3Bottom)}°"
                                            else "--",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = GrainTextPrimary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Physical Stability Checklist Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = if (isTelugu) "డేటా ధృవీకరణ చెక్‌లిస్ట్:" else "Validation Status:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GrainTextSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            StabilityCheckRow(
                                label = if (isTelugu) "సెన్సార్ డేటా ప్రవాహం" else "Packet Stream Receiving",
                                isReady = sensorData.packetsReceived > 2
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            StabilityCheckRow(
                                label = if (isTelugu) "తేమ రీడింగ్ స్థిరత్వం (<0.25% మార్పు)" else "Moisture Reading Stability",
                                isReady = sensorData.isStable
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            StabilityCheckRow(
                                label = if (isTelugu) "ఉష్ణోగ్రత రీడింగ్ స్థిరత్వం (<0.35°C మార్పు)" else "Multi-Depth Temp Stability",
                                isReady = sensorData.isStable
                            )
                        }
                    }
                }

                Text(
                    text = if (isTelugu) "ప్రోబ్‌ను స్థిరంగా ఉంచండి — కదిలించవద్దు" else "Keep the probe steady — do not move",
                    fontSize = 12.sp,
                    color = GrainTextSecondary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun StabilityCheckRow(label: String, isReady: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = if (isReady) Icons.Default.CheckCircle else Icons.Default.HourglassEmpty,
            contentDescription = null,
            tint = if (isReady) GrainPrimaryGreen else GrainAccentYellow,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isReady) GrainTextPrimary else GrainTextSecondary,
            fontWeight = if (isReady) FontWeight.Medium else FontWeight.Normal
        )
    }
}
