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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vrsec.grainguardian.data.cloud.CloudSyncState
import com.vrsec.grainguardian.domain.localization.LocalizationManager
import com.vrsec.grainguardian.ui.components.GrainBottomBar
import com.vrsec.grainguardian.ui.components.GrainTopBar
import com.vrsec.grainguardian.ui.navigation.Screen
import com.vrsec.grainguardian.ui.theme.*
import com.vrsec.grainguardian.viewmodel.GrainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncStatusScreen(
    viewModel: GrainViewModel,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val isTelugu = LocalizationManager.isTelugu()
    val allInspections by viewModel.allInspections.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()
    val confirmedSyncCount by viewModel.confirmedSyncCount.collectAsState()
    val cloudSyncState by viewModel.cloudSyncState.collectAsState()

    var serverEndpoint by remember { mutableStateOf("http://10.0.2.2:8000/api/v1/telemetry/sync") }

    Scaffold(
        topBar = {
            GrainTopBar(
                title = if (isTelugu) "క్లౌడ్ సమకాలీకరణ" else "Cloud Synchronization",
                showBackButton = true,
                onBackClick = onBack
            )
        },
        bottomBar = {
            GrainBottomBar(
                currentRoute = Screen.SyncStatus.route,
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
            // Header Explanation Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = GrainSurface)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = GrainPrimaryGreen,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isTelugu) "ఆఫ్‌లైన్-ఫస్ట్ డేటా సమకాలీకరణ" else "Offline-First Cloud Sync",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = GrainTextPrimary
                        )
                        Text(
                            text = if (isTelugu) "నెట్‌వర్క్ లేకపోయినా రికార్డులు ఫోన్‌లో భద్రంగా ఉంటాయి. నెట్‌వర్క్ ఉన్నప్పుడు కేంద్ర సర్వర్‌కు పంపవచ్చు." 
                            else "Readings are stored safely in local Room DB. Upload to warehouse server whenever network is available.",
                            fontSize = 12.sp,
                            color = GrainTextSecondary
                        )
                    }
                }
            }

            // Sync Stats Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Pending Sync
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (pendingSyncCount > 0) Color(0xFFFEF3C7) else GrainSurface
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "$pendingSyncCount",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (pendingSyncCount > 0) Color(0xFFB45309) else GrainTextPrimary
                        )
                        Text(
                            text = if (isTelugu) "అప్‌లోడ్ పెండింగ్" else "Pending Upload",
                            fontSize = 11.sp,
                            color = GrainTextSecondary
                        )
                    }
                }

                // Confirmed Synced
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = GrainSurface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "$confirmedSyncCount",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = GrainPrimaryGreen
                        )
                        Text(
                            text = if (isTelugu) "సింక్ పూర్తయినవి" else "Confirmed Synced",
                            fontSize = 11.sp,
                            color = GrainTextSecondary
                        )
                    }
                }

                // Total Stored
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = GrainSurface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "${allInspections.size}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = GrainTextPrimary
                        )
                        Text(
                            text = if (isTelugu) "మొత్తం రికార్డులు" else "Total Stored",
                            fontSize = 11.sp,
                            color = GrainTextSecondary
                        )
                    }
                }
            }

            // Sync Action & Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GrainSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isTelugu) "కేంద్ర సర్వర్ అమరిక & అప్‌లోడ్" else "Warehouse Server Endpoint & Upload",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = GrainTextPrimary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = serverEndpoint,
                        onValueChange = { serverEndpoint = it },
                        label = { Text(if (isTelugu) "సర్వర్ URL" else "Server API Endpoint", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sync State display
                    when (val s = cloudSyncState) {
                        is CloudSyncState.Idle -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = GrainTextSecondary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isTelugu) "సమకాలీకరణకు సిద్ధంగా ఉంది." else "Ready to synchronize local batches.",
                                    fontSize = 12.sp,
                                    color = GrainTextSecondary
                                )
                            }
                        }
                        is CloudSyncState.Syncing -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = GrainPrimaryGreen)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isTelugu) "క్లౌడ్‌కు రికార్డులను అప్‌లోడ్ చేస్తోంది..." else "Uploading batches to warehouse server...",
                                    fontSize = 12.sp,
                                    color = GrainPrimaryGreen,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        is CloudSyncState.Success -> {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFDCFCE7),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GrainDarkGreen, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isTelugu) "విజయవంతమైంది: ${s.inserted} రికార్డులు అప్‌లోడ్ అయ్యాయి (${s.skipped} మునుపే ఉన్నవి)"
                                        else "Sync Successful: ${s.inserted} uploaded, ${s.skipped} already present",
                                        fontSize = 12.sp,
                                        color = GrainDarkGreen,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        is CloudSyncState.Offline -> {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFEE2E2),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CloudOff, contentDescription = null, tint = GrainDangerRed, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isTelugu) "ఆఫ్‌లైన్: ${s.reason}. మీ డేటా ఫోన్‌లో సురక్షితంగా ఉంది."
                                        else "Offline: ${s.reason}. Local records remain safe on your device.",
                                        fontSize = 12.sp,
                                        color = GrainDangerRed,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.syncToCloud(serverEndpoint) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GrainPrimaryGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = cloudSyncState !is CloudSyncState.Syncing
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isTelugu) "ఇప్పుడే అన్ని రికార్డులను సింక్ చేయండి" else "Sync All Local Records Now",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // What is synced audit note
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = GrainSurface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = GrainPrimaryGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isTelugu) "డేటా భద్రత మరియు నియమాలు" else "Payload & Data Integrity",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = GrainTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isTelugu)
                            "• ప్రతి రికార్డుకు ఒక యూనిక్ UUID కేటాయించబడుతుంది, తద్వారా డూప్లికేట్లు రావు.\n• ఉష్ణోగ్రత, తేమ రీడింగ్‌లు మరియు నిర్ణయ స్థితి మాత్రమే పంపబడతాయి.\n• ఫోన్ వ్యక్తిగత సమాచారం లేదా GPS లొకేషన్ ఎప్పుడూ పంపబడదు."
                        else
                            "• Each inspection payload has a unique UUID to prevent duplicate server ingestion.\n• Only physical sensor readings (moisture %, temperatures) and status codes are uploaded.\n• Personal phone data and GPS locations are never transmitted.",
                        fontSize = 11.sp,
                        color = GrainTextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
