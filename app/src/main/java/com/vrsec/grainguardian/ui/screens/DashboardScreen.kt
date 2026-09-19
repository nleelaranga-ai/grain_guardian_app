package com.vrsec.grainguardian.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.painterResource
import com.vrsec.grainguardian.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
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
import com.vrsec.grainguardian.ui.components.GrainBottomBar
import com.vrsec.grainguardian.ui.components.GrainTopBar
import com.vrsec.grainguardian.ui.components.StatusBadge
import com.vrsec.grainguardian.ui.navigation.Screen
import com.vrsec.grainguardian.ui.theme.*
import com.vrsec.grainguardian.viewmodel.GrainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: GrainViewModel,
    onNavigate: (String) -> Unit,
    onStartDryingCheck: () -> Unit,
    onStartStorageCheck: () -> Unit
) {
    val isTelugu = LocalizationManager.isTelugu()
    val latestInspection by viewModel.latestInspection.collectAsState()
    val isConnected by viewModel.isProbeConnected.collectAsState()
    val deviceStatus by viewModel.deviceStatus.collectAsState()
    val isSandbox by viewModel.isDeveloperSandboxEnabled.collectAsState()

    Scaffold(
        topBar = {
            GrainTopBar(
                title = "GrainGuardian",
                showBackButton = false,
                showMenuButton = true,
                showNotification = true,
                onMenuClick = { onNavigate(Screen.Settings.route) },
                onNotificationClick = { onNavigate(Screen.InspectionHistory.route) }
            )
        },
        bottomBar = {
            GrainBottomBar(
                currentRoute = Screen.Dashboard.route,
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
                .padding(16.dp)
        ) {
            // Developer Sandbox Banner (if active)
            if (isSandbox) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = GrainAccentYellow, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isTelugu) "డెవలపర్ సాండ్‌బాక్స్ మోడ్ యాక్టివ్" else "Developer Sandbox Mode Active",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E)
                        )
                    }
                }
            }

            // Header Greeting & Probe Status Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_grain_logo),
                        contentDescription = "GrainGuardian Brand Logo",
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isTelugu) "శుభోదయం, రైతు సోదరా!" else "Good Morning, Farmer!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = GrainTextPrimary
                        )
                        Text(
                            text = if (isTelugu) "వరి నిల్వ ఆరోగ్య సమాచారం" else "Paddy Storage Health Intelligence",
                            fontSize = 12.sp,
                            color = GrainTextSecondary
                        )
                    }
                }

                // Probe Connectivity Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isConnected) GrainLightGreen else Color(0xFFF1F5F9))
                        .clickable { onNavigate(Screen.ConnectProbe.route) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isConnected) GrainPrimaryGreen else Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isConnected) {
                                if (isTelugu) "ప్రోబ్ సిద్ధం" else "Probe Ready"
                            } else {
                                if (isTelugu) "ప్రోబ్ లేదు" else "No Probe"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isConnected) GrainPrimaryGreen else GrainTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Main Hero Card: Grain Storage Health Score
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = GrainDarkGreen),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isTelugu) "ధాన్యం ఆరోగ్య సూచిక" else "GRAIN HEALTH SCORE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.8f),
                            letterSpacing = 1.sp
                        )

                        if (latestInspection != null) {
                            StatusBadge(status = latestInspection!!.status)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (latestInspection != null) {
                        val score = 100 - latestInspection!!.riskScore
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$score",
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = " / 100",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${if (isTelugu) "చివరిగా తనిఖీ చేసినది:" else "Last inspected:"} ${latestInspection!!.formattedDate}",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    } else {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                text = if (isTelugu) "ఇంకా తనిఖీలు నమోదు కాలేదు" else "No Inspections Yet",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isTelugu) "ప్రోబ్ కనెక్ట్ చేసి మొదటి తనిఖీని ప్రారంభించండి" else "Connect GrainGuardian probe to take your first reading",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4 Compact Metric Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Moisture Metric
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = GrainSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (isTelugu) "తేమ" else "Moisture",
                            fontSize = 11.sp,
                            color = GrainTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = latestInspection?.let { "${it.moisture}%" } ?: "--",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = GrainTextPrimary
                        )
                    }
                }

                // Temp Metric
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = GrainSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (isTelugu) "ఉష్ణోగ్రత" else "Temp",
                            fontSize = 11.sp,
                            color = GrainTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = latestInspection?.let { "${it.t1Top}°C" } ?: "--",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = GrainTextPrimary
                        )
                    }
                }

                // Risk Metric
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = GrainSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (isTelugu) "ప్రమాదం" else "Risk",
                            fontSize = 11.sp,
                            color = GrainTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = latestInspection?.let {
                                when (it.status) {
                                    "SAFE" -> if (isTelugu) "తక్కువ" else "Low"
                                    "WARNING" -> if (isTelugu) "మధ్యస్థం" else "Moderate"
                                    else -> if (isTelugu) "అధికం" else "High"
                                }
                            } ?: "--",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = GrainTextPrimary
                        )
                    }
                }

                // Probe Battery
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = GrainSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (isTelugu) "బ్యాటరీ" else "Probe",
                            fontSize = 11.sp,
                            color = GrainTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isConnected) "${deviceStatus.batteryPercent}%" else "--",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isConnected) GrainPrimaryGreen else GrainTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Two Huge Primary Action Cards
            Text(
                text = if (isTelugu) "తనిఖీని ప్రారంభించండి" else "Start Assessment",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = GrainTextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action 1: CHECK BEFORE STORAGE
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onStartDryingCheck() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GrainSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEF3C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WbSunny,
                            contentDescription = "Drying Check",
                            tint = GrainAccentYellow,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isTelugu) "నిల్వకు ముందు తనిఖీ (ఎండబెట్టడం)" else "CHECK BEFORE STORAGE",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = GrainTextPrimary
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = if (isTelugu) "వరి బస్తాల్లో నింపడానికి సరిపడా ఎండిందా?" else "Drying readiness & safe moisture check",
                            fontSize = 12.sp,
                            color = GrainTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action 2: CHECK STORED PADDY
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onStartStorageCheck() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GrainSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDBEAFE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = "Storage Check",
                            tint = GrainAccentBlue,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isTelugu) "నిల్వ ధాన్యం తనిఖీ (ఆరోగ్యం)" else "CHECK STORED PADDY",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = GrainTextPrimary
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = if (isTelugu) "బస్తాల లోపల వేడి, తేమ & హాట్‌స్పాట్ తనిఖీ" else "3-Depth heat gradient & hotspot detection",
                            fontSize = 12.sp,
                            color = GrainTextSecondary
                        )
                    }
                }
            }
        }
    }
}
