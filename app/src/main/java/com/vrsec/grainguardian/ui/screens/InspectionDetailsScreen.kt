package com.vrsec.grainguardian.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch
import java.io.File
import com.vrsec.grainguardian.ui.theme.GrainPrimaryGreen
import com.vrsec.grainguardian.ui.theme.GrainDarkGreen
import com.vrsec.grainguardian.data.model.GrainInspectionEntity
import com.vrsec.grainguardian.domain.localization.LocalizationManager
import com.vrsec.grainguardian.ui.components.GrainTopBar
import com.vrsec.grainguardian.ui.components.StatusBadge
import com.vrsec.grainguardian.ui.theme.GrainBackground
import com.vrsec.grainguardian.ui.theme.GrainSurface
import com.vrsec.grainguardian.ui.theme.GrainTextPrimary
import com.vrsec.grainguardian.ui.theme.GrainTextSecondary
import com.vrsec.grainguardian.viewmodel.GrainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectionDetailsScreen(
    inspectionId: Long,
    viewModel: GrainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isTelugu = LocalizationManager.isTelugu()
    var inspection by remember { mutableStateOf<GrainInspectionEntity?>(null) }
    var exportPdfMessage by remember { mutableStateOf<String?>(null) }
    var isExporting by remember { mutableStateOf(false) }

    LaunchedEffect(inspectionId) {
        inspection = viewModel.getInspectionById(inspectionId)
    }

    Scaffold(
        topBar = {
            GrainTopBar(
                title = if (isTelugu) "తనిఖీ వివరాలు" else "Inspection Details",
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
                .padding(20.dp)
        ) {
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
                            text = if (isTelugu) "పంట" else "Crop",
                            fontSize = 14.sp,
                            color = GrainTextSecondary
                        )
                        Text(
                            text = inspection?.cropName ?: "Paddy",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = GrainTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isTelugu) "తనిఖీ రకం" else "Mode",
                            fontSize = 14.sp,
                            color = GrainTextSecondary
                        )
                        Text(
                            text = inspection?.mode ?: "Storage Check",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = GrainTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isTelugu) "తేదీ & సమయం" else "Date & Time",
                            fontSize = 14.sp,
                            color = GrainTextSecondary
                        )
                        Text(
                            text = inspection?.formattedDate ?: "Today, 10:30 AM",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = GrainTextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Readings section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GrainSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = if (isTelugu) "రీడింగులు (Readings)" else "Readings",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = GrainTextPrimary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Moisture", fontSize = 14.sp, color = GrainTextSecondary)
                        Text(text = inspection?.moisture?.let { "${it}%" } ?: "--.-%", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Top (T1)", fontSize = 14.sp, color = GrainTextSecondary)
                        Text(text = "${inspection?.t1Top ?: 29.0f}°C", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Middle (T2)", fontSize = 14.sp, color = GrainTextSecondary)
                        Text(text = "${inspection?.t2Middle ?: 31.0f}°C", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Bottom (T3)", fontSize = 14.sp, color = GrainTextSecondary)
                        Text(text = "${inspection?.t3Bottom ?: 32.0f}°C", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Result Badge Card
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
                            text = if (isTelugu) "తుది ఫలితం" else "Overall Result",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = GrainTextPrimary
                        )
                        StatusBadge(status = inspection?.status ?: "SAFE")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isTelugu) "క్షీణత ప్రమాద స్కోరు (BDRS)" else "Biological Risk (BDRS)",
                            fontSize = 13.sp,
                            color = GrainTextSecondary
                        )
                        Text(
                            text = "${inspection?.riskScore ?: 0} / 100",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = GrainTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isTelugu) "ప్రధాన ప్రమాదం" else "Dominant Risk",
                            fontSize = 13.sp,
                            color = GrainTextSecondary
                        )
                        Text(
                            text = inspection?.dominantRisk ?: "None",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GrainTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isTelugu) "క్లౌడ్ సింక్ స్థితి" else "Cloud Sync",
                            fontSize = 13.sp,
                            color = GrainTextSecondary
                        )
                        Text(
                            text = inspection?.syncState ?: "LOCAL",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (inspection?.syncState == "CONFIRMED") GrainDarkGreen else GrainTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Actionable Recommendations Card
            val recList = inspection?.recommendationsJson?.split("|")?.filter { it.isNotBlank() } ?: emptyList()
            if (recList.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = GrainSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = if (isTelugu) "రైతు చర్యలు & సిఫార్సులు" else "Recommended Actions",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = GrainTextPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        recList.forEach { rec ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "• ",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GrainPrimaryGreen
                                )
                                Text(
                                    text = rec.trim(),
                                    fontSize = 13.sp,
                                    color = GrainTextPrimary,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            // PDF Certificate Export Section
            exportPdfMessage?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF0FDF4),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = msg,
                        fontSize = 12.sp,
                        color = GrainDarkGreen,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Button(
                onClick = {
                    val currentInsp = inspection ?: return@Button
                    isExporting = true
                    coroutineScope.launch {
                        try {
                            val pdfFile = viewModel.exportInspectionToPdf(context, currentInsp)
                            exportPdfMessage = if (isTelugu)
                                "✓ నాణ్యతా పత్రం PDF గా సేవ్ చేయబడింది: ${pdfFile.name}"
                                else "✓ Quality Certificate generated: ${pdfFile.name}"
                        } catch (e: Exception) {
                            exportPdfMessage = "PDF generation failed: ${e.message}"
                        } finally {
                            isExporting = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GrainPrimaryGreen),
                enabled = inspection != null && !isExporting
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isExporting) {
                        if (isTelugu) "పీడీఎఫ్ రూపొందిస్తోంది..." else "Generating Certificate..."
                    } else {
                        if (isTelugu) "నాణ్యతా పత్రం (PDF సర్టిఫికేట్)" else "Export Quality Certificate (PDF)"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}