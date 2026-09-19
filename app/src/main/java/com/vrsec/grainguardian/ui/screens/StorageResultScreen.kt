package com.vrsec.grainguardian.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vrsec.grainguardian.domain.localization.LocalizationManager
import com.vrsec.grainguardian.ui.components.GrainTopBar
import com.vrsec.grainguardian.ui.components.PrimaryActionButton
import com.vrsec.grainguardian.ui.components.SecondaryActionButton
import com.vrsec.grainguardian.ui.theme.GrainBackground
import com.vrsec.grainguardian.ui.theme.GrainDarkGreen
import com.vrsec.grainguardian.ui.theme.GrainLightGreen
import com.vrsec.grainguardian.ui.theme.GrainPrimaryGreen
import com.vrsec.grainguardian.ui.theme.GrainSurface
import com.vrsec.grainguardian.ui.theme.GrainTextPrimary
import com.vrsec.grainguardian.ui.theme.GrainTextSecondary
import com.vrsec.grainguardian.viewmodel.GrainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageResultScreen(
    viewModel: GrainViewModel,
    onBack: () -> Unit,
    onViewAnalysis: () -> Unit,
    onSaveAndHome: () -> Unit
) {
    val isTelugu = LocalizationManager.isTelugu()
    val decision by viewModel.currentDecision.collectAsState()

    Scaffold(
        topBar = {
            GrainTopBar(
                title = if (isTelugu) "నిల్వ ఆరోగ్య ఫలితం" else "Storage Health Result",
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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(GrainLightGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = GrainPrimaryGreen,
                        modifier = Modifier.size(46.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                androidx.compose.material3.Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = GrainLightGreen,
                    modifier = Modifier.padding(bottom = 14.dp)
                ) {
                    Text(
                        text = if (isTelugu) "✓ ధృవీకరించబడిన హార్డ్‌వేర్ సెన్సార్ డేటా" else "✓ Validated Hardware Sensor Stream",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GrainDarkGreen,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Text(
                    text = if (isTelugu) (decision?.headlineTelugu ?: "నిల్వ పరిస్థితి ఫలితం") else (decision?.headline ?: "STORAGE CONDITION RESULT"),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = GrainDarkGreen,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = GrainSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isTelugu) "తేమ శాతం (Moisture)" else "Moisture Content",
                                fontSize = 14.sp,
                                color = GrainTextSecondary
                            )
                            Text(
                                text = decision?.let { "${it.moisture}%" } ?: "--.-%",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = GrainTextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isTelugu) "పై పొర (T1) ఉష్ణోగ్రత" else "Top (T1) Temperature",
                                fontSize = 14.sp,
                                color = GrainTextSecondary
                            )
                            Text(
                                text = decision?.let { "${it.t1Top}°C" } ?: "--.-°C",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = GrainTextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isTelugu) "మధ్య పొర (T2) ఉష్ణోగ్రత" else "Middle (T2) Temperature",
                                fontSize = 14.sp,
                                color = GrainTextSecondary
                            )
                            Text(
                                text = decision?.let { "${it.t2Middle}°C" } ?: "--.-°C",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = GrainTextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isTelugu) "దిగువ పొర (T3) ఉష్ణోగ్రత" else "Bottom (T3) Temperature",
                                fontSize = 14.sp,
                                color = GrainTextSecondary
                            )
                            Text(
                                text = decision?.let { "${it.t3Bottom}°C" } ?: "--.-°C",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = GrainTextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PrimaryActionButton(
                    text = if (isTelugu) "రిస్క్ విశ్లేషణ చూడండి (View Analysis)" else "View Analysis",
                    onClick = onViewAnalysis
                )

                SecondaryActionButton(
                    text = if (isTelugu) "భద్రపరచి హోమ్‌కు వెళ్లండి" else "Save Inspection",
                    onClick = {
                        viewModel.saveCurrentInspection {
                            onSaveAndHome()
                        }
                    }
                )
            }
        }
    }
}