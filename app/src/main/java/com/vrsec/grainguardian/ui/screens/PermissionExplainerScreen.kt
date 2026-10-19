package com.vrsec.grainguardian.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.ui.platform.LocalContext
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
fun PermissionExplainerScreen(
    viewModel: GrainViewModel,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isTelugu = LocalizationManager.isTelugu()
    val hasBle = remember { viewModel.hasBlePermissions() }

    Scaffold(
        topBar = {
            GrainTopBar(
                title = if (isTelugu) "యాప్ అనుమతులు" else "Device Permissions",
                showBackButton = true,
                onBackClick = onBack
            )
        },
        bottomBar = {
            GrainBottomBar(
                currentRoute = Screen.PermissionExplainer.route,
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
            // Explainer Header Card
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
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = GrainPrimaryGreen,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isTelugu) "అనుమతులు ఎందుకు అవసరం?" else "Why Permissions Are Needed",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = GrainTextPrimary
                        )
                        Text(
                            text = if (isTelugu) "హార్డ్‌వేర్ ప్రోబ్‌తో కమ్యూనికేషన్ కోసం అవసరమైన సిస్టమ్ అనుమతుల వివరాలు." 
                            else "Transparent explanation of Android system capabilities used by GrainGuardian.",
                            fontSize = 12.sp,
                            color = GrainTextSecondary
                        )
                    }
                }
            }

            // Permission 1: Bluetooth Low Energy
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = GrainSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bluetooth, contentDescription = null, tint = GrainPrimaryGreen, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isTelugu) "బ్లూటూత్ & సమీప పరికరాలు" else "Bluetooth & Nearby Devices",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = GrainTextPrimary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (hasBle) GrainLightGreen else Color(0xFFFEF3C7)
                        ) {
                            Text(
                                text = if (hasBle) (if (isTelugu) "మంజూరు చేయబడింది" else "GRANTED") else (if (isTelugu) "అవసరం" else "REQUIRED"),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hasBle) GrainDarkGreen else Color(0xFF92400E),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isTelugu)
                            "గ్రెయిన్ గార్డియన్ ESP32 హార్డ్‌వేర్ ప్రోబ్‌ను కనుగొని, రియల్-టైమ్ సెన్సార్ రీడింగ్‌లను స్వీకరించడానికి బ్లూటూత్ లో ఎనర్జీ (BLE) అవసరం."
                        else
                            "Used exclusively to scan for and maintain a secure local BLE connection with your GrainGuardian ESP32 moisture and temperature sensing probe.",
                        fontSize = 12.sp,
                        color = GrainTextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }

            // Permission 2: Location (Android OS requirement for BLE)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = GrainSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = GrainPrimaryGreen, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isTelugu) "లొకేషన్ (Android OS నియమం)" else "Location (Android OS Requirement)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = GrainTextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isTelugu)
                            "Android 11 మరియు అంతకంటే పాత వెర్షన్లలో బ్లూటూత్ స్కానింగ్ చేయడానికి Android OS లొకేషన్ అనుమతిని కోరుతుంది. గ్రెయిన్ గార్డియన్ మీ GPS స్థానాన్ని ఎప్పుడూ రికార్డ్ చేయదు లేదా ట్రాక్ చేయదు."
                        else
                            "Android OS requires Location permission to discover nearby Bluetooth Low Energy devices on Android 11 and earlier. GrainGuardian never accesses, records, or shares your actual GPS coordinates.",
                        fontSize = 12.sp,
                        color = GrainTextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }

            // Permission 3: Notifications
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = GrainSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = GrainPrimaryGreen, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isTelugu) "హెచ్చరిక నోటిఫికేషన్లు" else "Safety Notifications",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = GrainTextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isTelugu)
                            "ధాన్యం కుప్పలో ఉష్ణోగ్రత 35°C దాటినప్పుడు (హాట్‌స్పాట్ ప్రమాదం) లేదా తేమ అధికంగా ఉన్నప్పుడు తక్షణ హెచ్చరికలను పంపడానికి అవసరం."
                        else
                            "Enables instant alerts if stored paddy exhibits internal temperature hotspots (> 35°C) or critical spoilage-level moisture contents.",
                        fontSize = 12.sp,
                        color = GrainTextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }

            // Open App Settings Action
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = GrainSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isTelugu) "అనుమతులను సవరించండి" else "Manage Permissions",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = GrainTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isTelugu) "ఆండ్రాయిడ్ సిస్టమ్ సెట్టింగ్స్‌లో అనుమతులను మార్చడానికి ఇక్కడ నొక్కండి." 
                        else "Jump directly to Android system app settings to grant or revoke hardware permissions.",
                        fontSize = 12.sp,
                        color = GrainTextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GrainPrimaryGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isTelugu) "సిస్టమ్ సెట్టింగ్స్ తెరవండి" else "Open Android System Settings",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
