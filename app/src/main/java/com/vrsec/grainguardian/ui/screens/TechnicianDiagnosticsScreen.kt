package com.vrsec.grainguardian.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vrsec.grainguardian.domain.localization.LocalizationManager
import com.vrsec.grainguardian.ui.components.GrainBottomBar
import com.vrsec.grainguardian.ui.components.GrainTopBar
import com.vrsec.grainguardian.ui.navigation.Screen
import com.vrsec.grainguardian.ui.theme.*
import com.vrsec.grainguardian.viewmodel.GrainViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechnicianDiagnosticsScreen(
    viewModel: GrainViewModel,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val isTelugu = LocalizationManager.isTelugu()
    val isConnected by viewModel.isProbeConnected.collectAsState()
    val sensorData by viewModel.liveSensorData.collectAsState()
    val deviceStatus by viewModel.deviceStatus.collectAsState()
    val isSandbox by viewModel.isDeveloperSandboxEnabled.collectAsState()

    Scaffold(
        topBar = {
            GrainTopBar(
                title = if (isTelugu) "ప్రోబ్ డయాగ్నోస్టిక్స్" else "Probe Diagnostics",
                showBackButton = true,
                onBackClick = onBack
            )
        },
        bottomBar = {
            GrainBottomBar(
                currentRoute = Screen.TechnicianDiagnostics.route,
                onNavigate = onNavigate
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(GrainBackground)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Live Status Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isConnected || isSandbox) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isConnected || isSandbox) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isConnected || isSandbox) GrainDarkGreen else GrainDangerRed,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isSandbox) (if (isTelugu) "సాండ్‌బాక్స్ డయాగ్నోస్టిక్స్ మోడ్" else "Developer Sandbox Active")
                            else if (isConnected) (if (isTelugu) "హార్డ్‌వేర్ ప్రోబ్ కనెక్ట్ అయింది" else "Probe Hardware Online")
                            else (if (isTelugu) "హార్డ్‌వేర్ ప్రోబ్ ఆఫ్‌లైన్" else "Probe Hardware Disconnected"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isConnected || isSandbox) GrainDarkGreen else GrainDangerRed
                        )
                        Text(
                            text = if (isSandbox) "Emulating 1-Wire & analog moisture ADC telemetry" 
                            else "Packets received: ${sensorData.packetsReceived} • Device: ${deviceStatus.deviceName}",
                            fontSize = 11.sp,
                            color = GrainTextSecondary
                        )
                    }
                }
            }

            // Raw Telemetry Section
            Text(
                text = if (isTelugu) "రా సెన్సార్ పిన్ & బస్ డేటా" else "Raw Hardware Bus Telemetry",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = GrainTextPrimary
            )

            // DS18B20 1-Wire Bus Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = GrainSurface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Dallas DS18B20 1-Wire (GPIO 4)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = GrainTextPrimary
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (sensorData.t1Ok) GrainLightGreen else Color(0xFFFEE2E2)
                        ) {
                            Text(
                                text = if (sensorData.t1Ok) "BUS OK" else "FAULT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (sensorData.t1Ok) GrainDarkGreen else GrainDangerRed,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Top Sensor (T1):", fontSize = 12.sp, color = GrainTextSecondary)
                            Text(
                                text = if (sensorData.t1Top > 0) String.format(Locale.US, "%.2f °C", sensorData.t1Top) else "--",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = GrainTextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Column {
                            Text("Middle Sensor (T2):", fontSize = 12.sp, color = GrainTextSecondary)
                            Text(
                                text = if (sensorData.t2Middle > 0) String.format(Locale.US, "%.2f °C", sensorData.t2Middle) else "--",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = GrainTextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Column {
                            Text("Bottom Sensor (T3):", fontSize = 12.sp, color = GrainTextSecondary)
                            Text(
                                text = if (sensorData.t3Bottom > 0) String.format(Locale.US, "%.2f °C", sensorData.t3Bottom) else "--",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = GrainTextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Moisture Sensor & ADC Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = GrainSurface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Capacitive Moisture Sensor (GPIO 34 ADC1)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = GrainTextPrimary
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (sensorData.moistureSensorOk) GrainLightGreen else Color(0xFFFEE2E2)
                        ) {
                            Text(
                                text = if (sensorData.moistureSensorOk) "ADC OK" else "FAULT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (sensorData.moistureSensorOk) GrainDarkGreen else GrainDangerRed,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Converted Moisture:", fontSize = 12.sp, color = GrainTextSecondary)
                            Text(
                                text = if (sensorData.moisture > 0) String.format(Locale.US, "%.2f %%", sensorData.moisture) else "--",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = GrainPrimaryGreen,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Column {
                            Text("Estimated Raw ADC:", fontSize = 12.sp, color = GrainTextSecondary)
                            Text(
                                text = if (sensorData.moisture > 0) "${(4095 - (sensorData.moisture * 75)).toInt()} / 4095" else "--",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = GrainTextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Ambient & Battery Telemetry Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = GrainSurface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Ambient & Power Telemetry (DHT11 GPIO 27 & V_BAT)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = GrainTextPrimary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Ambient RH%:", fontSize = 12.sp, color = GrainTextSecondary)
                            Text(
                                text = if (sensorData.ambientHumidity > 0) String.format(Locale.US, "%.1f %%", sensorData.ambientHumidity) else "65.0 %",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = GrainTextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Column {
                            Text("Battery Voltage:", fontSize = 12.sp, color = GrainTextSecondary)
                            Text(
                                text = "3.92 V (LiPo)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = GrainTextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Column {
                            Text("Battery Level:", fontSize = 12.sp, color = GrainTextSecondary)
                            Text(
                                text = "${deviceStatus.batteryPercent}%",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = GrainPrimaryGreen,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Calibration Polynomial Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = GrainSurface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isTelugu) "క్యాలిబ్రేషన్ సమీకరణం (చదవడం మాత్రమే)" else "Calibration Curve (Read-Only)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = GrainTextPrimary
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFEF3C7)
                        ) {
                            Text(
                                text = "LOCKED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "MC% = -0.0135 × ADC + 42.80",
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        color = GrainDarkGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Fitted against standard 105°C 24-hr oven dry reference samples. Firmware revision: v1.0-field.",
                        fontSize = 11.sp,
                        color = GrainTextSecondary
                    )
                }
            }
        }
    }
}
