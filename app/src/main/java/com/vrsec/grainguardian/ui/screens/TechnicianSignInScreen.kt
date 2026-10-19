package com.vrsec.grainguardian.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
fun TechnicianSignInScreen(
    viewModel: GrainViewModel,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val isTelugu = LocalizationManager.isTelugu()
    val isSandbox by viewModel.isDeveloperSandboxEnabled.collectAsState()

    var pinCode by remember { mutableStateOf("") }
    var showRestrictedDialog by remember { mutableStateOf(false) }

    if (showRestrictedDialog) {
        AlertDialog(
            onDismissRequest = { showRestrictedDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = GrainAccentYellow,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = if (isTelugu) "ఫీల్డ్ మోడ్‌లో పరిమితం చేయబడింది" else "Restricted in Production",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isTelugu)
                        "సెన్సార్ క్యాలిబ్రేషన్ మరియు ఫర్మ్‌వేర్ పారామితుల మార్పు అధీకృత హార్డ్‌వేర్ ల్యాబ్‌లకు మాత్రమే పరిమితం. డెవలపర్ సాండ్‌బాక్స్ మోడ్‌ను ప్రారంభించడం ద్వారా మీరు డయాగ్నోస్టిక్స్ డెమోను చూడవచ్చు."
                    else
                        "Sensor calibration writing is locked in field production builds to guarantee legal agricultural safety standards. Enable Developer Sandbox in Settings to preview diagnostics.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestrictedDialog = false
                        if (isSandbox) {
                            onNavigate(Screen.TechnicianDiagnostics.route)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GrainPrimaryGreen)
                ) {
                    Text(if (isSandbox) (if (isTelugu) "డయాగ్నోస్టిక్స్‌కు వెళ్ళండి" else "Proceed to Diagnostics") else "OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestrictedDialog = false }) {
                    Text(if (isTelugu) "ముగించు" else "Dismiss")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            GrainTopBar(
                title = if (isTelugu) "టెక్నీషియన్ యాక్సెస్" else "Technician Access",
                showBackButton = true,
                onBackClick = onBack
            )
        },
        bottomBar = {
            GrainBottomBar(
                currentRoute = Screen.TechnicianSignIn.route,
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
            // Mandatory Prominent Restriction Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = Color(0xFFB45309),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isTelugu) "క్యాలిబ్రేషన్ లాక్ చేయబడింది (పరిమితం)" else "Calibration Mode Restricted",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF92400E)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isTelugu)
                                "హార్డ్‌వేర్ సెన్సార్ క్యాలిబ్రేషన్ మరియు ఫర్మ్‌వేర్ రైట్ మోడ్ అధీకృత టెక్నీషియన్లకు మాత్రమే అనుమతించబడింది. క్షేత్రస్థాయి రైతులు ధ్రువీకరించబడిన ఆఫ్‌లైన్ తనిఖీ మోడ్‌లోనే పని చేస్తారు."
                            else
                                "Field calibration mode requires authorized hardware technician access. Field users operate in validated offline inspection mode to preserve measurement integrity.",
                            fontSize = 12.sp,
                            color = Color(0xFF78350F),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // PIN Gate Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GrainSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = if (isTelugu) "అధీకృత టెక్నీషియన్ పిన్ (PIN)" else "Authorized Technician PIN",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = GrainTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isTelugu) "హార్డ్‌వేర్ ప్రోబ్ క్యాలిబ్రేషన్ టూల్స్ తెరవడానికి పిన్ నమోదు చేయండి." 
                        else "Enter 6-digit laboratory calibration key to unlock raw hardware telemetry.",
                        fontSize = 12.sp,
                        color = GrainTextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = pinCode,
                        onValueChange = { if (it.length <= 6) pinCode = it },
                        label = { Text(if (isTelugu) "టెక్నీషియన్ పిన్" else "Technician PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (pinCode == "123456" || isSandbox) {
                                onNavigate(Screen.TechnicianDiagnostics.route)
                            } else {
                                showRestrictedDialog = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GrainPrimaryGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isTelugu) "డయాగ్నోస్టిక్స్ అన్‌లాక్ చేయండి" else "Unlock Diagnostics",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Sandbox bypass option for dev/testing
                    OutlinedButton(
                        onClick = {
                            if (isSandbox) {
                                onNavigate(Screen.TechnicianDiagnostics.route)
                            } else {
                                showRestrictedDialog = true
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GrainAccentYellow),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isTelugu) "సాండ్‌బాక్స్ డయాగ్నోస్టిక్స్ డెమో" else "View Sandbox Diagnostics Demo",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Explanatory note
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = GrainSurface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (isTelugu) "క్యాలిబ్రేషన్ రక్షణ ఉద్దేశం" else "Purpose of Calibration Lock",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = GrainTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isTelugu)
                            "ప్రామాణిక కొలత ఫలితాలు మారకుండా ఉండేందుకు క్యాలిబ్రేషన్ రైట్ ఆపరేషన్లు లాక్ చేయబడ్డాయి. ఇది తప్పుడు తేమ లెక్కలను నిరోధిస్తుంది."
                        else
                            "To comply with post-harvest engineering validation protocols, sensor polynomial parameters cannot be modified in normal user operation to prevent adulteration of storage readiness certificates.",
                        fontSize = 11.sp,
                        color = GrainTextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
