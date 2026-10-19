package com.vrsec.grainguardian.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.vrsec.grainguardian.ui.navigation.Screen
import com.vrsec.grainguardian.ui.theme.*
import com.vrsec.grainguardian.viewmodel.GrainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    viewModel: GrainViewModel,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val isTelugu = LocalizationManager.isTelugu()
    val allInspections by viewModel.allInspections.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()
    val confirmedSyncCount by viewModel.confirmedSyncCount.collectAsState()

    var farmerName by remember { mutableStateOf("K. Rama Rao") }
    var farmerPhone by remember { mutableStateOf("+91 98480 23456") }
    var farmerVillage by remember { mutableStateOf("Kankipadu, Krishna District") }
    var showEditDialog by remember { mutableStateOf(false) }

    if (showEditDialog) {
        var tempName by remember { mutableStateOf(farmerName) }
        var tempPhone by remember { mutableStateOf(farmerPhone) }
        var tempVillage by remember { mutableStateOf(farmerVillage) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    text = if (isTelugu) "రైతు వివరాలను సవరించండి" else "Edit Farmer Details",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text(if (isTelugu) "రైతు పేరు" else "Farmer Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempPhone,
                        onValueChange = { tempPhone = it },
                        label = { Text(if (isTelugu) "ఫోన్ నంబర్" else "Phone Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempVillage,
                        onValueChange = { tempVillage = it },
                        label = { Text(if (isTelugu) "గ్రామం / మండలం" else "Village / Mandal") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        farmerName = tempName
                        farmerPhone = tempPhone
                        farmerVillage = tempVillage
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GrainPrimaryGreen)
                ) {
                    Text(if (isTelugu) "సేవ్ చేయండి" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text(if (isTelugu) "రద్దు" else "Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            GrainTopBar(
                title = if (isTelugu) "రైతు ప్రొఫైల్ & ఖాతా" else "Farmer Profile & Account",
                showBackButton = true,
                onBackClick = onBack
            )
        },
        bottomBar = {
            GrainBottomBar(
                currentRoute = Screen.Account.route,
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
            // Profile Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GrainSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(GrainLightGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = GrainDarkGreen,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = farmerName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = GrainTextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = farmerPhone,
                            fontSize = 13.sp,
                            color = GrainTextSecondary
                        )
                        Text(
                            text = farmerVillage,
                            fontSize = 12.sp,
                            color = GrainPrimaryGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = GrainPrimaryGreen
                        )
                    }
                }
            }

            // Offline Mode Guarantee Badge
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.OfflinePin,
                        contentDescription = null,
                        tint = GrainDarkGreen,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isTelugu) "ఆఫ్‌లైన్-ఫస్ట్ మోడ్ సక్రియంగా ఉంది" else "Offline-First Mode Active",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = GrainDarkGreen
                        )
                        Text(
                            text = if (isTelugu) "ఇంటర్నెట్ లేకుండా స్థానికంగా అన్ని తనిఖీలు భద్రపరచబడతాయి." 
                            else "All measurements and decisions operate 100% locally on your device.",
                            fontSize = 11.sp,
                            color = GrainTextSecondary
                        )
                    }
                }
            }

            // Quick Stats Section
            Text(
                text = if (isTelugu) "వ్యవసాయ గణాంకాలు & స్థలాలు" else "Farm Statistics & Storage",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = GrainTextPrimary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate(Screen.StorageLocations.route) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = GrainSurface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Icon(
                            imageVector = Icons.Default.Warehouse,
                            contentDescription = null,
                            tint = GrainPrimaryGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "3",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = GrainTextPrimary
                        )
                        Text(
                            text = if (isTelugu) "నిల్వ ప్రదేశాలు" else "Storage Sites",
                            fontSize = 11.sp,
                            color = GrainTextSecondary
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate(Screen.InspectionHistory.route) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = GrainSurface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Icon(
                            imageVector = Icons.Default.Assignment,
                            contentDescription = null,
                            tint = GrainPrimaryGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${allInspections.size}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = GrainTextPrimary
                        )
                        Text(
                            text = if (isTelugu) "స్థానిక రికార్డులు" else "Local Records",
                            fontSize = 11.sp,
                            color = GrainTextSecondary
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate(Screen.SyncStatus.route) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = GrainSurface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = if (pendingSyncCount > 0) GrainAccentYellow else GrainPrimaryGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "$pendingSyncCount",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (pendingSyncCount > 0) Color(0xFFD97706) else GrainTextPrimary
                        )
                        Text(
                            text = if (isTelugu) "సింక్ పెండింగ్" else "Pending Sync",
                            fontSize = 11.sp,
                            color = GrainTextSecondary
                        )
                    }
                }
            }

            // Quick Actions & Tools
            Text(
                text = if (isTelugu) "సిస్టమ్ & నిర్వహణ" else "System & Management",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = GrainTextPrimary
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GrainSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Storage Locations
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate(Screen.StorageLocations.route) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Storefront, contentDescription = null, tint = GrainPrimaryGreen, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isTelugu) "ధాన్యం నిల్వ ప్రదేశాలు" else "Storage Granaries & Sheds",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = GrainTextPrimary
                            )
                            Text(
                                text = if (isTelugu) "గోదాములు మరియు గన్నీ సంచుల దొంతరలను నిర్వహించండి" else "Manage granaries, stacks, and drying yards",
                                fontSize = 11.sp,
                                color = GrainTextSecondary
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = GrainTextSecondary)
                    }

                    Divider(color = GrainDivider, modifier = Modifier.padding(vertical = 4.dp))

                    // Cloud Sync
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate(Screen.SyncStatus.route) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = GrainPrimaryGreen, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isTelugu) "క్లౌడ్ సమకాలీకరణ స్థితి" else "Cloud Sync Status",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = GrainTextPrimary
                            )
                            Text(
                                text = if (isTelugu) "స్థానిక రికార్డుల అప్‌లోడ్ మరియు స్థితి సమీక్ష" else "Review pending uploads and warehouse server sync",
                                fontSize = 11.sp,
                                color = GrainTextSecondary
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = GrainTextSecondary)
                    }

                    Divider(color = GrainDivider, modifier = Modifier.padding(vertical = 4.dp))

                    // Permissions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate(Screen.PermissionExplainer.route) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = GrainPrimaryGreen, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isTelugu) "యాప్ అనుమతుల వివరాలు" else "Device Permissions Explainer",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = GrainTextPrimary
                            )
                            Text(
                                text = if (isTelugu) "బ్లూటూత్ మరియు లొకేషన్ అనుమతులు ఎందుకు అవసరం" else "Understand BLE, Nearby Devices & Android permissions",
                                fontSize = 11.sp,
                                color = GrainTextSecondary
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = GrainTextSecondary)
                    }

                    Divider(color = GrainDivider, modifier = Modifier.padding(vertical = 4.dp))

                    // Privacy
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate(Screen.PrivacyData.route) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PrivacyTip, contentDescription = null, tint = GrainPrimaryGreen, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isTelugu) "గోప్యత & స్థానిక డేటా హామీ" else "Privacy & Local Data Policy",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = GrainTextPrimary
                            )
                            Text(
                                text = if (isTelugu) "మీ ధాన్యం డేటా మీ పరికరంలోనే ఉంటుంది" else "Zero third-party tracking, 100% farmer data ownership",
                                fontSize = 11.sp,
                                color = GrainTextSecondary
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = GrainTextSecondary)
                    }

                    Divider(color = GrainDivider, modifier = Modifier.padding(vertical = 4.dp))

                    // Technician
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate(Screen.TechnicianSignIn.route) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Build, contentDescription = null, tint = GrainAccentYellow, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isTelugu) "హార్డ్‌వేర్ టెక్నీషియన్ గేట్" else "Hardware Technician Access",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = GrainTextPrimary
                            )
                            Text(
                                text = if (isTelugu) "ల్యాబ్ క్యాలిబ్రేషన్ & రా సెన్సార్ డయాగ్నోస్టిక్స్" else "Probe sensor bus diagnostics & calibration tools",
                                fontSize = 11.sp,
                                color = GrainTextSecondary
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = GrainTextSecondary)
                    }
                }
            }
        }
    }
}
