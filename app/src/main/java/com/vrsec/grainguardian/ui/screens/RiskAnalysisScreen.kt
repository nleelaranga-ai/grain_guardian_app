package com.vrsec.grainguardian.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vrsec.grainguardian.domain.localization.LocalizationManager
import com.vrsec.grainguardian.ui.components.GrainTopBar
import com.vrsec.grainguardian.ui.components.PrimaryActionButton
import com.vrsec.grainguardian.ui.components.RiskMeter
import com.vrsec.grainguardian.ui.theme.GrainBackground
import com.vrsec.grainguardian.ui.theme.GrainLightWarning
import com.vrsec.grainguardian.ui.theme.GrainSurface
import com.vrsec.grainguardian.ui.theme.GrainTextPrimary
import com.vrsec.grainguardian.ui.theme.GrainTextSecondary
import com.vrsec.grainguardian.ui.theme.GrainWarningOrange
import com.vrsec.grainguardian.viewmodel.GrainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiskAnalysisScreen(
    viewModel: GrainViewModel,
    onBack: () -> Unit,
    onViewRecommendation: () -> Unit
) {
    val isTelugu = LocalizationManager.isTelugu()
    val decision by viewModel.currentDecision.collectAsState()

    Scaffold(
        topBar = {
            GrainTopBar(
                title = if (isTelugu) "రిస్క్ విశ్లేషణ" else "Risk Analysis",
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
            Column(modifier = Modifier.fillMaxWidth()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = GrainSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(GrainLightWarning),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = GrainWarningOrange,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isTelugu) "శ్రద్ధ అవసరం (Needs Attention)" else "WARNING - Needs Attention",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = GrainWarningOrange
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isTelugu) (decision?.dominantRiskTelugu ?: "") else (decision?.dominantRisk ?: ""),
                            fontSize = 13.sp,
                            color = GrainTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = GrainSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = if (isTelugu) "కారణాలు (Why?)" else "Risk Factors (Why?)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = GrainTextPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val reasons = if (isTelugu) {
                            decision?.whyReasonsTelugu ?: listOf("తేమ సాధారణం కంటే ఎక్కువగా ఉంది.")
                        } else {
                            decision?.whyReasons ?: listOf("Moisture is elevated.")
                        }

                        reasons.forEach { reason ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "• ",
                                    fontWeight = FontWeight.Bold,
                                    color = GrainWarningOrange,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = reason,
                                    fontSize = 13.sp,
                                    color = GrainTextPrimary,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = GrainSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(modifier = Modifier.padding(18.dp)) {
                        RiskMeter(riskScore = decision?.bdrs ?: 48)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            PrimaryActionButton(
                text = if (isTelugu) "రైతు సూచనలు చూడండి (View Recommendation)" else "View Recommendation",
                onClick = onViewRecommendation,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
    }
}