package com.vrsec.grainguardian.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vrsec.grainguardian.R
import com.vrsec.grainguardian.domain.localization.LocalizationManager
import com.vrsec.grainguardian.ui.components.GrainTopBar
import com.vrsec.grainguardian.ui.components.PrimaryActionButton
import com.vrsec.grainguardian.ui.theme.*
import com.vrsec.grainguardian.viewmodel.GrainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceInfoScreen(
    viewModel: GrainViewModel,
    onBack: () -> Unit
) {
    val isTelugu = LocalizationManager.isTelugu()
    val deviceStatus by viewModel.deviceStatus.collectAsState()
    val isConnected by viewModel.isProbeConnected.collectAsState()

    Scaffold(
        topBar = {
            GrainTopBar(
                title = if (isTelugu) "పరికరం సమాచారం" else "GrainGuardian Probe Info",
                onBackClick = onBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(GrainBackground)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Device graphic card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8F3)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_probe),
                                contentDescription = "Probe",
                                modifier = Modifier.size(70.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isConnected) deviceStatus.deviceName else (if (isTelugu) "గ్రెయిన్‌గార్డియన్ ప్రోబ్" else "GrainGuardian Hardware Probe"),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = GrainDarkGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Hardware stats
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = GrainSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = if (isTelugu) "స్థితి" else "Connection Status", fontSize = 14.sp, color = GrainTextSecondary)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isConnected) GrainLightGreen else Color(0xFFF1F5F9))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isConnected) (if (isTelugu) "కనెక్ట్ అయింది" else "Connected") 
                                    else (if (isTelugu) "డిస్‌కనెక్ట్ అయింది" else "Disconnected"),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isConnected) GrainPrimaryGreen else GrainTextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = if (isTelugu) "పరికరం ID" else "Device ID", fontSize = 14.sp, color = GrainTextSecondary)
                            Text(text = if (isConnected) deviceStatus.deviceId else "--", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = if (isTelugu) "బ్యాటరీ" else "Battery", fontSize = 14.sp, color = GrainTextSecondary)
                            Text(text = if (isConnected) "${deviceStatus.batteryPercent}%" else "--", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = if (isTelugu) "ఫర్మ్‌వేర్" else "Firmware", fontSize = 14.sp, color = GrainTextSecondary)
                            Text(text = "v${deviceStatus.firmwareVersion}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sensor checks
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = GrainSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (isTelugu) "సెన్సార్ల స్థితి (Sensors Check)" else "Sensors Self-Check",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = GrainTextPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        SensorStatusRow(label = if (isTelugu) "కెపాసిటివ్ తేమ సెన్సార్ (SEN0193)" else "Capacitive Moisture Sensor (SEN0193)", isOk = deviceStatus.moistureSensorOk)
                        Spacer(modifier = Modifier.height(8.dp))
                        SensorStatusRow(label = if (isTelugu) "పై పొర ఉష్ణోగ్రత సెన్సార్ (T1)" else "Top Temp Sensor (T1 - DS18B20)", isOk = deviceStatus.tempSensor1Ok)
                        Spacer(modifier = Modifier.height(8.dp))
                        SensorStatusRow(label = if (isTelugu) "మధ్య పొర ఉష్ణోగ్రత సెన్సార్ (T2)" else "Middle Temp Sensor (T2 - DS18B20)", isOk = deviceStatus.tempSensor2Ok)
                        Spacer(modifier = Modifier.height(8.dp))
                        SensorStatusRow(label = if (isTelugu) "దిగువ పొర ఉష్ణోగ్రత సెన్సార్ (T3)" else "Bottom Temp Sensor (T3 - DS18B20)", isOk = deviceStatus.tempSensor3Ok)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isConnected) {
                    Button(
                        onClick = {
                            viewModel.disconnectProbe()
                            onBack()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFEE2E2),
                            contentColor = GrainDangerRed
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = if (isTelugu) "ప్రోబ్ డిస్‌కనెక్ట్ చేయండి" else "Disconnect Probe",
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    PrimaryActionButton(
                        text = if (isTelugu) "ప్రోబ్‌ను కనెక్ట్ చేయండి" else "Connect Probe",
                        onClick = {
                            viewModel.scanForDevices()
                            onBack()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SensorStatusRow(label: String, isOk: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = GrainTextPrimary, modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isOk) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = if (isOk) "OK" else "Offline",
                tint = if (isOk) GrainPrimaryGreen else Color.Gray,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isOk) "OK" else "Offline",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isOk) GrainPrimaryGreen else Color.Gray
            )
        }
    }
}
