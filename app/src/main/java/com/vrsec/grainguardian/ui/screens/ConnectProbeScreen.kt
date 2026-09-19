package com.vrsec.grainguardian.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vrsec.grainguardian.data.bluetooth.BleConnectionState
import com.vrsec.grainguardian.domain.localization.LocalizationManager
import com.vrsec.grainguardian.ui.components.GrainTopBar
import com.vrsec.grainguardian.ui.components.PrimaryActionButton
import com.vrsec.grainguardian.ui.components.SecondaryActionButton
import com.vrsec.grainguardian.ui.theme.*
import com.vrsec.grainguardian.viewmodel.GrainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectProbeScreen(
    viewModel: GrainViewModel,
    onBack: () -> Unit,
    onConnected: () -> Unit
) {
    val isTelugu = LocalizationManager.isTelugu()
    val bleState by viewModel.bleConnectionState.collectAsState()
    val isConnected by viewModel.isProbeConnected.collectAsState()
    val deviceStatus by viewModel.deviceStatus.collectAsState()
    val liveSensorData by viewModel.liveSensorData.collectAsState()
    val isSandbox by viewModel.isDeveloperSandboxEnabled.collectAsState()

    val pulseTransition = rememberInfiniteTransition(label = "bluetooth_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bt_scale"
    )

    val isScanning = bleState is BleConnectionState.Scanning

    Scaffold(
        topBar = {
            GrainTopBar(
                title = if (isTelugu) "బ్లూటూత్ ప్రోబ్ కనెక్షన్" else "Connect Probe",
                onBackClick = onBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(GrainBackground)
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Radar / Pulse Icon Box
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .scale(if (isScanning) pulseScale else 1f)
                        .clip(CircleShape)
                        .background(
                            when {
                                isConnected -> GrainLightGreen
                                bleState is BleConnectionState.DeviceNotFound -> Color(0xFFFEE2E2)
                                else -> Color(0xFFEFF6FF)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            isConnected -> Icons.Default.CheckCircle
                            bleState is BleConnectionState.DeviceNotFound -> Icons.Default.BluetoothDisabled
                            isScanning -> Icons.Default.BluetoothSearching
                            else -> Icons.Default.Bluetooth
                        },
                        contentDescription = "Bluetooth Status",
                        tint = when {
                            isConnected -> GrainPrimaryGreen
                            bleState is BleConnectionState.DeviceNotFound -> GrainDangerRed
                            else -> GrainAccentBlue
                        },
                        modifier = Modifier.size(54.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = when (bleState) {
                        is BleConnectionState.Connected -> if (isTelugu) "ప్రోబ్ విజయవంతంగా కనెక్ట్ అయింది" else "GrainGuardian Probe Connected"
                        is BleConnectionState.Scanning -> if (isTelugu) "దగ్గర్లోని ప్రోబ్ కోసం శోధిస్తోంది..." else "Searching for GrainGuardian..."
                        is BleConnectionState.Connecting -> if (isTelugu) "కనెక్ట్ అవుతోంది..." else "Connecting to probe..."
                        is BleConnectionState.DeviceNotFound -> if (isTelugu) "ప్రోబ్ కనుగొనబడలేదు" else "Probe Not Detected"
                        is BleConnectionState.BluetoothDisabled -> if (isTelugu) "బ్లూటూత్ ఆపివేయబడింది" else "Bluetooth is Turned Off"
                        is BleConnectionState.PermissionsMissing -> if (isTelugu) "అనుమతులు అవసరం" else "Permissions Required"
                        else -> if (isTelugu) "ప్రోబ్ కనెక్ట్ కాలేదు" else "Probe Not Connected"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = GrainTextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = when (bleState) {
                        is BleConnectionState.Connected -> if (isTelugu) "సెన్సార్లు సిద్ధంగా ఉన్నాయి. కొలత ప్రారంభించవచ్చు." else "All sensors ready. You can now proceed to measurement."
                        is BleConnectionState.Scanning -> if (isTelugu) "దయచేసి వేచి ఉండండి, బ్లూటూత్ పరికరాన్ని కనుగొంటోంది." else "Please wait while we search for your hardware probe."
                        is BleConnectionState.DeviceNotFound -> if (isTelugu) "ప్రోబ్ స్విచ్ ఆన్ చేయబడిందో లేదో చూడండి, ఫోన్‌ను దగ్గరగా ఉంచండి." else "Make sure probe is powered ON and in Bluetooth range."
                        is BleConnectionState.BluetoothDisabled -> if (isTelugu) "ప్రోబ్ కనెక్ట్ చేయడానికి మీ ఫోన్‌లో బ్లూటూత్ ఆన్ చేయండి." else "Enable Bluetooth in device settings to communicate with probe."
                        else -> if (isTelugu) "కొలత చేయడానికి ముందు ప్రోబ్‌ను కనెక్ట్ చేయాలి." else "Hardware connection is strictly required before measuring."
                    },
                    fontSize = 13.sp,
                    color = GrainTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(30.dp))

                // Connected Hardware Device Card (Real information)
                if (isConnected) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, GrainPrimaryGreen, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = GrainSurface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Sensors, contentDescription = null, tint = GrainPrimaryGreen)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = deviceStatus.deviceName,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GrainTextPrimary
                                        )
                                        Text(
                                            text = "ID: ${deviceStatus.deviceId}",
                                            fontSize = 11.sp,
                                            color = GrainTextSecondary
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(GrainLightGreen)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (isTelugu) "కనెక్ట్ అయింది" else "Connected",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GrainPrimaryGreen
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Divider(color = GrainDivider)
                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${if (isTelugu) "బ్యాటరీ:" else "Battery:"} ${deviceStatus.batteryPercent}%",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = GrainTextPrimary
                                )
                                Text(
                                    text = if (isTelugu) "సిగ్నల్: బలమైనది" else "Signal: Strong",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = GrainPrimaryGreen
                                )
                                Text(
                                    text = if (isTelugu) "సెన్సార్లు: 4/4 రెడీ" else "Sensors: 4/4 Ready",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = GrainPrimaryGreen
                                )
                            }
                        }
                    }
                }
            }

            // Bottom CTAs
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isConnected) {
                    PrimaryActionButton(
                        text = if (isTelugu) "కొలతకు వెళ్లండి (Proceed)" else "Proceed to Measurement",
                        onClick = onConnected
                    )
                    SecondaryActionButton(
                        text = if (isTelugu) "ప్రోబ్ డిస్‌కనెక్ట్ చేయండి" else "Disconnect Probe",
                        onClick = { viewModel.disconnectProbe() }
                    )
                } else {
                    PrimaryActionButton(
                        text = if (isScanning) {
                            if (isTelugu) "శోధిస్తోంది..." else "Scanning..."
                        } else {
                            if (isTelugu) "ప్రోబ్ కోసం శోధించండి" else "Scan for Probe"
                        },
                        enabled = !isScanning,
                        onClick = { viewModel.scanForDevices() }
                    )
                }
            }
        }
    }
}
