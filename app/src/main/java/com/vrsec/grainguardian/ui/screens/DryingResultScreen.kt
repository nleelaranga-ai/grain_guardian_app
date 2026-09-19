package com.vrsec.grainguardian.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
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
import com.vrsec.grainguardian.data.model.RiskStatus
import com.vrsec.grainguardian.domain.localization.LocalizationManager
import com.vrsec.grainguardian.ui.components.GrainTopBar
import com.vrsec.grainguardian.ui.components.PrimaryActionButton
import com.vrsec.grainguardian.ui.components.SecondaryActionButton
import com.vrsec.grainguardian.ui.theme.GrainBackground
import com.vrsec.grainguardian.ui.theme.GrainDarkGreen
import com.vrsec.grainguardian.ui.theme.GrainLightGreen
import com.vrsec.grainguardian.ui.theme.GrainLightWarning
import com.vrsec.grainguardian.ui.theme.GrainPrimaryGreen
import com.vrsec.grainguardian.ui.theme.GrainSurface
import com.vrsec.grainguardian.ui.theme.GrainTextPrimary
import com.vrsec.grainguardian.ui.theme.GrainTextSecondary
import com.vrsec.grainguardian.ui.theme.GrainWarningOrange
import com.vrsec.grainguardian.viewmodel.GrainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DryingResultScreen(
    viewModel: GrainViewModel,
    onBack: () -> Unit,
    onViewRecommendation: () -> Unit,
    onSaveAndHome: () -> Unit
) {
    val isTelugu = LocalizationManager.isTelugu()
    val decision by viewModel.currentDecision.collectAsState()

    val isSafe = decision?.isMoistureSafe ?: true
    val iconBg = if (isSafe) GrainLightGreen else GrainLightWarning
    val iconTint = if (isSafe) GrainPrimaryGreen else GrainWarningOrange

    Scaffold(
        topBar = {
            GrainTopBar(
                title = if (isTelugu) "ఎండబెట్టడం ఫలితం" else "Drying Readiness Result",
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

                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSafe) Icons.Default.Check else Icons.Default.Warning,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(52.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = if (isTelugu) {
                        if (isSafe) "నిల్వకు సిద్ధంగా ఉంది" else "సిద్ధంగా లేదు\nఎండబెట్టడం కొనసాగించండి"
                    } else {
                        if (isSafe) "READY FOR STORAGE" else "NOT READY\nCONTINUE DRYING"
                    },
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSafe) GrainDarkGreen else GrainWarningOrange,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                androidx.compose.material3.Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = GrainLightGreen,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text(
                        text = if (isTelugu) "✓ ధృవీకరించబడిన హార్డ్‌వేర్ కొలత" else "✓ Validated Sensor Stream",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GrainDarkGreen,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = GrainSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isTelugu) "కొలిచిన తేమ శాతం" else "Moisture Content",
                            fontSize = 14.sp,
                            color = GrainTextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = decision?.let { "${it.moisture}%" } ?: "--.-%",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = GrainTextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isTelugu) (decision?.subtitleTelugu ?: "") else (decision?.subtitle ?: ""),
                            fontSize = 13.sp,
                            color = GrainTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PrimaryActionButton(
                    text = if (isTelugu) "సూచనలు చూడండి" else "View Recommendation",
                    onClick = onViewRecommendation
                )

                SecondaryActionButton(
                    text = if (isTelugu) "తనిఖీని భద్రపరచండి" else "Save Inspection",
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