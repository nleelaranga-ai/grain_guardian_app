package com.vrsec.grainguardian.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vrsec.grainguardian.domain.localization.LocalizationManager
import com.vrsec.grainguardian.ui.components.GrainTopBar
import com.vrsec.grainguardian.ui.components.PrimaryActionButton
import com.vrsec.grainguardian.ui.components.SecondaryActionButton
import com.vrsec.grainguardian.ui.theme.GrainAccentYellow
import com.vrsec.grainguardian.ui.theme.GrainBackground
import com.vrsec.grainguardian.ui.theme.GrainLightGreen
import com.vrsec.grainguardian.ui.theme.GrainPrimaryGreen
import com.vrsec.grainguardian.ui.theme.GrainSurface
import com.vrsec.grainguardian.ui.theme.GrainTextPrimary
import com.vrsec.grainguardian.ui.theme.GrainTextSecondary
import com.vrsec.grainguardian.viewmodel.GrainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrepareMeasurementScreen(
    viewModel: GrainViewModel,
    onBack: () -> Unit,
    onConnectProbeClick: () -> Unit,
    onStartMeasurement: () -> Unit
) {
    val isTelugu = LocalizationManager.isTelugu()
    val isConnected by viewModel.isProbeConnected.collectAsState()
    val deviceStatus by viewModel.deviceStatus.collectAsState()

    Scaffold(
        topBar = {
            GrainTopBar(
                title = if (isTelugu) "కొలతకు సిద్ధం చేయండి" else "Prepare Measurement",
                onBackClick = onBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(GrainBackground)
                .padding(paddingValues)
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                if (!isConnected) {
                    // Blocked state card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2))
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = if (isTelugu) "ప్రోబ్ కనెక్ట్ కాలేదు!" else "Probe Not Connected!",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF991B1B)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isTelugu) "కొలత చేయడానికి హార్డ్‌వేర్ ప్రోబ్ అవసరం." else "Physical probe is required before measuring.",
                                    fontSize = 12.sp,
                                    color = Color(0xFFB91C1C)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 3 Farmer Steps
                Text(
                    text = if (isTelugu) "సరిగ్గా కొలవడానికి 3 ముఖ్యమైన దశలు:" else "3 Steps for Accurate Measurement:",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = GrainTextPrimary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                InstructionStepCard(
                    stepNum = "1",
                    title = if (isTelugu) "ప్రోబ్‌ను ధాన్యంలోకి దూర్చండి" else "Insert Probe Deeply",
                    subtitle = if (isTelugu) "అన్ని పొరల సెన్సార్లు ధాన్యంతో పూర్తిగా కప్పబడాలి." else "Ensure top, middle, and bottom sensors are submerged in grain."
                )

                Spacer(modifier = Modifier.height(12.dp))

                InstructionStepCard(
                    stepNum = "2",
                    title = if (isTelugu) "ప్రోబ్‌ను కదల్చకుండా స్థిరంగా ఉంచండి" else "Keep the Probe Steady",
                    subtitle = if (isTelugu) "కొలత జరుగుతున్నంత సేపు కదిలించవద్దు." else "Do not move or shake the probe while receiving data."
                )

                Spacer(modifier = Modifier.height(12.dp))

                InstructionStepCard(
                    stepNum = "3",
                    title = if (isTelugu) "కొలతలు స్థిరపడేవరకు వేచి ఉండండి" else "Wait for Reading Stability",
                    subtitle = if (isTelugu) "సిస్టమ్ సెన్సార్ స్థిరత్వాన్ని ధృవీకరించి ఫలితాన్ని లెక్కిస్తుంది." else "System will validate physical stability before computing risk."
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Hardware status chip
                if (isConnected) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(GrainLightGreen)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = GrainPrimaryGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isTelugu) "ప్రోబ్ సిద్ధంగా ఉంది (${deviceStatus.batteryPercent}% బ్యాటరీ)" 
                            else "Hardware Connected & Ready (${deviceStatus.batteryPercent}% battery)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = GrainPrimaryGreen
                        )
                    }
                }
            }

            if (isConnected) {
                PrimaryActionButton(
                    text = if (isTelugu) "కొలత ప్రారంభించండి (Start Measurement)" else "Start Measurement",
                    onClick = onStartMeasurement,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            } else {
                PrimaryActionButton(
                    text = if (isTelugu) "ప్రోబ్‌ను కనెక్ట్ చేయండి" else "Connect Probe First",
                    onClick = onConnectProbeClick,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun InstructionStepCard(
    stepNum: String,
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = GrainSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(GrainPrimaryGreen),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stepNum,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = GrainTextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = GrainTextSecondary
                )
            }
        }
    }
}
