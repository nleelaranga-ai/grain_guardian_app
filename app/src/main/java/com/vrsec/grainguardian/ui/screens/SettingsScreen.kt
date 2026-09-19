package com.vrsec.grainguardian.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vrsec.grainguardian.domain.localization.AppLanguage
import com.vrsec.grainguardian.domain.localization.LocalizationManager
import com.vrsec.grainguardian.ui.components.GrainBottomBar
import com.vrsec.grainguardian.ui.components.GrainTopBar
import com.vrsec.grainguardian.ui.navigation.Screen
import com.vrsec.grainguardian.ui.theme.*
import com.vrsec.grainguardian.viewmodel.GrainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: GrainViewModel,
    onNavigate: (String) -> Unit,
    onOpenDeviceInfo: () -> Unit
) {
    val isTelugu = LocalizationManager.isTelugu()
    val currentLang by viewModel.currentLanguage.collectAsState()
    val deviceStatus by viewModel.deviceStatus.collectAsState()
    val isConnected by viewModel.isProbeConnected.collectAsState()
    val isSandbox by viewModel.isDeveloperSandboxEnabled.collectAsState()
    var notificationsEnabled by remember { mutableStateOf(true) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showBenchmarkLoadedDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(text = if (isTelugu) "రికార్డులను తొలగించాలా?" else "Clear All Records?") },
            text = { Text(text = if (isTelugu) "మీ అన్ని సేవ్ చేయబడిన తనిఖీ రికార్డులు తొలగించబడతాయి." else "All stored local inspection records will be cleared.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllHistory()
                    showClearDialog = false
                }) {
                    Text(text = if (isTelugu) "తొలగించు" else "Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(text = if (isTelugu) "రద్దు" else "Cancel")
                }
            }
        )
    }

    if (showBenchmarkLoadedDialog) {
        AlertDialog(
            onDismissRequest = { showBenchmarkLoadedDialog = false },
            title = { Text(text = if (isTelugu) "నమూనా డేటా లోడ్ అయింది" else "Benchmark Data Loaded") },
            text = { Text(text = if (isTelugu) "2 నమూనా తనిఖీ రికార్డులు హిస్టరీకి చేర్చబడ్డాయి." else "2 realistic benchmark inspection records have been added to your History.") },
            confirmButton = {
                TextButton(onClick = { showBenchmarkLoadedDialog = false }) {
                    Text(text = "OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            GrainTopBar(
                title = if (isTelugu) "సెట్టింగ్స్" else "Settings",
                showBackButton = false,
                showMenuButton = true,
                onMenuClick = {}
            )
        },
        bottomBar = {
            GrainBottomBar(
                currentRoute = Screen.Settings.route,
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
            // Language Setting
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GrainSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isTelugu) "భాష (Language)" else "Language",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = GrainTextPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.setLanguage(AppLanguage.ENGLISH) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentLang == AppLanguage.ENGLISH) GrainPrimaryGreen else Color(0xFFF1F5F0),
                                contentColor = if (currentLang == AppLanguage.ENGLISH) Color.White else GrainTextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("English")
                        }

                        Button(
                            onClick = { viewModel.setLanguage(AppLanguage.TELUGU) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentLang == AppLanguage.TELUGU) GrainPrimaryGreen else Color(0xFFF1F5F0),
                                contentColor = if (currentLang == AppLanguage.TELUGU) Color.White else GrainTextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("తెలుగు")
                        }
                    }
                }
            }

            // Hardware Probe Status
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenDeviceInfo() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GrainSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isConnected) GrainPrimaryGreen else Color.Gray)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isConnected) deviceStatus.deviceName else (if (isTelugu) "ప్రోబ్ కనెక్ట్ కాలేదు" else "Probe Disconnected"),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = GrainTextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = if (isConnected) "${deviceStatus.batteryPercent}% ${if (isTelugu) "బ్యాటరీ" else "Battery"} • ${if (isTelugu) "హార్డ్‌వేర్ వివరాలు చూడండి" else "Tap for device specs"}"
                            else (if (isTelugu) "కనెక్ట్ చేయడానికి ఇక్కడ నొక్కండి" else "Tap to connect probe"),
                            fontSize = 12.sp,
                            color = GrainTextSecondary
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = GrainTextSecondary
                    )
                }
            }

            // Notification Toggle
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GrainSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isTelugu) "నోటిఫికేషన్లు" else "Notifications & Alerts",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = GrainTextPrimary
                        )
                        Text(
                            text = if (isTelugu) "తేమ & ఉష్ణోగ్రత హెచ్చరికలు" else "Alert on high moisture or hotspot",
                            fontSize = 12.sp,
                            color = GrainTextSecondary
                        )
                    }

                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { notificationsEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = GrainPrimaryGreen
                        )
                    )
                }
            }

            // Developer Sandbox Mode (Probe Emulation)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (isSandbox) Color(0xFFFEF3C7) else GrainSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = null,
                                tint = if (isSandbox) Color(0xFF92400E) else GrainTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isTelugu) "డెవలపర్ సాండ్‌బాక్స్ (అనుకరణ)" else "Developer Sandbox (Emulation)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSandbox) Color(0xFF92400E) else GrainTextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isTelugu) "హార్డ్‌వేర్ ప్రోబ్ లేనప్పుడు స్క్రీన్లను టెస్ట్ చేయడానికి అనుకరణ." 
                            else "Simulate probe packets when physical probe is away for lab testing.",
                            fontSize = 11.sp,
                            color = GrainTextSecondary
                        )
                    }

                    Switch(
                        checked = isSandbox,
                        onCheckedChange = { viewModel.setDeveloperSandboxMode(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = GrainAccentYellow
                        )
                    )
                }
            }

            // Data Management
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GrainSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isTelugu) "డేటా నిర్వహణ" else "Data Management",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = GrainTextPrimary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.loadBenchmarkData()
                                showBenchmarkLoadedDialog = true
                            }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isTelugu) "నమూనా బెంచ్‌మార్క్ డేటాను లోడ్ చేయండి" else "Load Benchmark Sample Data",
                            fontSize = 13.sp,
                            color = GrainPrimaryGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = GrainPrimaryGreen, modifier = Modifier.size(16.dp))
                    }

                    Divider(color = GrainDivider, modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showClearDialog = true }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isTelugu) "అన్ని తనిఖీ రికార్డులను తొలగించండి" else "Clear All Inspection Records",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // About App
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GrainSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "GrainGuardian",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = GrainTextPrimary
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (isTelugu) "స్మార్ట్ వరి నిల్వ & ఎండబెట్టడం ఇంటెలిజెన్స్ సిస్టమ్" else "Smart Paddy Post-Harvest Decision Support System",
                        fontSize = 12.sp,
                        color = GrainDarkGreen,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Version 1.0.0",
                        fontSize = 12.sp,
                        color = GrainTextSecondary
                    )
                }
            }
        }
    }
}
