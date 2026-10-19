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
import com.vrsec.grainguardian.domain.localization.LocalizationManager
import com.vrsec.grainguardian.ui.components.GrainBottomBar
import com.vrsec.grainguardian.ui.components.GrainTopBar
import com.vrsec.grainguardian.ui.navigation.Screen
import com.vrsec.grainguardian.ui.theme.*
import com.vrsec.grainguardian.viewmodel.GrainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyDataScreen(
    viewModel: GrainViewModel,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val isTelugu = LocalizationManager.isTelugu()
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text(
                    text = if (isTelugu) "స్థానిక రికార్డులను తొలగించాలా?" else "Clear All Local Records?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isTelugu)
                        "ఈ చర్య మీ ఫోన్ నుండి అన్ని సేవ్ చేయబడిన తనిఖీ రికార్డులను శాశ్వతంగా తొలగిస్తుంది."
                    else
                        "This will permanently erase all saved moisture and temperature inspection records from this device's local database.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isTelugu) "తొలగించు" else "Delete All Records")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(if (isTelugu) "రద్దు" else "Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            GrainTopBar(
                title = if (isTelugu) "గోప్యత & డేటా పాలసీ" else "Privacy & Data Policy",
                showBackButton = true,
                onBackClick = onBack
            )
        },
        bottomBar = {
            GrainBottomBar(
                currentRoute = Screen.PrivacyData.route,
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
            // Header Card
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
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = GrainPrimaryGreen,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isTelugu) "రైతు డేటా సార్వభౌమత్వ హామీ" else "Farmer Data Sovereignty",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = GrainTextPrimary
                        )
                        Text(
                            text = if (isTelugu) "మీ వ్యవసాయ మరియు ధాన్యం సమాచారం మీ నియంత్రణలోనే ఉంటుంది." 
                            else "Your crop measurements belong to you, stored safely on your device.",
                            fontSize = 12.sp,
                            color = GrainTextSecondary
                        )
                    }
                }
            }

            // Pillar 1: Local-First Storage
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = GrainSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = GrainPrimaryGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isTelugu) "1. స్థానిక నిల్వ హామీ (Local-First)" else "1. Local-First Storage Architecture",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = GrainTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isTelugu)
                            "అన్ని ప్రోబ్ కొలతలు, నిర్ణయ ఫలితాలు మరియు బ్యాచ్ వివరాలు మీ మొబైల్ పరికరంలోని ఎంబెడెడ్ రూమ్ డేటాబేస్ (grainguardian.db) లో మాత్రమే స్థానికంగా నిల్వ చేయబడతాయి. యాప్ పనిచేయడానికి క్లౌడ్ అవసరం లేదు."
                        else
                            "All sensor samples, FES decision evaluations, and batch histories are written exclusively to your smartphone's internal Room SQLite database. The application operates with zero internet dependency.",
                        fontSize = 12.sp,
                        color = GrainTextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }

            // Pillar 2: Zero Third-Party Trackers
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = GrainSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = GrainPrimaryGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isTelugu) "2. ఎటువంటి ట్రాకర్లు లేదా ప్రకటనలు లేవు" else "2. Zero Third-Party Telemetry",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = GrainTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isTelugu)
                            "GrainGuardian లో ఎటువంటి ప్రకటనల SDKలు, Google/Meta అనలిటిక్స్ ట్రాకర్లు లేదా వ్యక్తిగత డేటా మానిటరింగ్ టూల్స్ చేర్చబడలేదు. మీ పరికరం నుండి రహస్యంగా ఏ డేటావూ బయటకు వెళ్ళదు."
                        else
                            "No third-party analytics libraries, advertising trackers, or behavioral profiling services are packaged in this APK. No background telemetry calls are executed without explicit user action.",
                        fontSize = 12.sp,
                        color = GrainTextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }

            // Pillar 3: Cloud Sync Transparency
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = GrainSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudQueue, contentDescription = null, tint = GrainPrimaryGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isTelugu) "3. సమకాలీకరణ పారదర్శకత" else "3. Cloud Synchronization Transparency",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = GrainTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isTelugu)
                            "మీరు 'Sync Now' నొక్కినప్పుడు మాత్రమే ఈ క్రింది డేటా అప్‌లోడ్ అవుతుంది:\n• తనిఖీ UUID మరియు టైమ్‌స్టాంప్\n• సగటు తేమ శాతం మరియు ఉష్ణోగ్రతలు\n• నిర్ణయ స్థితి మరియు రక్షణ సిఫార్సు\nవ్యక్తిగత పరిచయాలు, ఫోటోలు లేదా స్థాన సమాచారం ఎప్పటికీ పంపబడవు."
                        else
                            "Only when you explicitly initiate cloud synchronization is data transmitted to your designated warehouse server:\n• Cryptographic inspection UUID and timestamp\n• Physical moisture % and probe sensor temperatures\n• Storage decision code and rule evaluation\nPersonal identifiers, contacts, or GPS fixes are never captured or sent.",
                        fontSize = 12.sp,
                        color = GrainTextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }

            // Data Deletion Action
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = GrainSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isTelugu) "డేటా తొలగింపు హక్కు" else "Right to Erasure",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = GrainTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isTelugu) "మీ పరికరంలో ఉన్న అన్ని తనిఖీ రికార్డులను మీరు ఎప్పుడైనా పూర్తిగా తొలగించవచ్చు." 
                        else "You have full authority to purge all inspection history from your phone at any time.",
                        fontSize = 12.sp,
                        color = GrainTextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { showClearDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isTelugu) "అన్ని స్థానిక రికార్డులను తొలగించండి" else "Clear All Stored Records",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
