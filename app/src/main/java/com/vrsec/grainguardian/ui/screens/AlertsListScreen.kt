package com.vrsec.grainguardian.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.vrsec.grainguardian.data.model.GrainInspectionEntity
import com.vrsec.grainguardian.domain.localization.LocalizationManager
import com.vrsec.grainguardian.ui.components.GrainBottomBar
import com.vrsec.grainguardian.ui.components.GrainTopBar
import com.vrsec.grainguardian.ui.navigation.Screen
import com.vrsec.grainguardian.ui.theme.*
import com.vrsec.grainguardian.viewmodel.GrainViewModel

enum class AlertFilter {
    ALL,
    HIGH_MOISTURE,
    HOTSPOT,
    SENSOR_FAULT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsListScreen(
    viewModel: GrainViewModel,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    onAlertClick: (Long) -> Unit
) {
    val isTelugu = LocalizationManager.isTelugu()
    val warnings by viewModel.warningInspections.collectAsState()
    val deviceProblems by viewModel.deviceProblemInspections.collectAsState()

    var selectedFilter by remember { mutableStateOf(AlertFilter.ALL) }

    // Combine distinct alert inspections
    val allAlerts = remember(warnings, deviceProblems) {
        val map = linkedMapOf<Long, GrainInspectionEntity>()
        warnings.forEach { map[it.id] = it }
        deviceProblems.forEach { map[it.id] = it }
        map.values.sortedByDescending { it.timestamp }
    }

    val filteredAlerts = remember(allAlerts, selectedFilter) {
        when (selectedFilter) {
            AlertFilter.ALL -> allAlerts
            AlertFilter.HIGH_MOISTURE -> allAlerts.filter { it.moisture > 14.0f }
            AlertFilter.HOTSPOT -> allAlerts.filter { it.t1Top > 35.0f || it.t2Middle > 35.0f || it.t3Bottom > 35.0f }
            AlertFilter.SENSOR_FAULT -> allAlerts.filter {
                it.statusCode == "SENSOR_FAULT" || it.reasonCode == "SENSOR_ERROR" || it.status == "HIGH_RISK"
            }
        }
    }

    Scaffold(
        topBar = {
            GrainTopBar(
                title = if (isTelugu) "హెచ్చరికలు & అలర్ట్‌లు" else "Warnings & Alerts",
                showBackButton = true,
                onBackClick = onBack
            )
        },
        bottomBar = {
            GrainBottomBar(
                currentRoute = Screen.AlertsList.route,
                onNavigate = onNavigate
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(GrainBackground)
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Filter chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == AlertFilter.ALL,
                    onClick = { selectedFilter = AlertFilter.ALL },
                    label = { Text(if (isTelugu) "అన్నీ" else "All (${allAlerts.size})", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GrainPrimaryGreen,
                        selectedLabelColor = Color.White
                    )
                )

                FilterChip(
                    selected = selectedFilter == AlertFilter.HIGH_MOISTURE,
                    onClick = { selectedFilter = AlertFilter.HIGH_MOISTURE },
                    label = { Text(if (isTelugu) "తేమ" else "Moisture", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GrainPrimaryGreen,
                        selectedLabelColor = Color.White
                    )
                )

                FilterChip(
                    selected = selectedFilter == AlertFilter.HOTSPOT,
                    onClick = { selectedFilter = AlertFilter.HOTSPOT },
                    label = { Text(if (isTelugu) "వేడి మచ్చలు" else "Hotspots", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GrainPrimaryGreen,
                        selectedLabelColor = Color.White
                    )
                )

                FilterChip(
                    selected = selectedFilter == AlertFilter.SENSOR_FAULT,
                    onClick = { selectedFilter = AlertFilter.SENSOR_FAULT },
                    label = { Text(if (isTelugu) "సెన్సార్ లోపం" else "Faults", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GrainPrimaryGreen,
                        selectedLabelColor = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (filteredAlerts.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = GrainPrimaryGreen,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isTelugu) "ప్రస్తుతం ఎటువంటి ప్రమాద హెచ్చరికలు లేవు" else "No Active Warnings Found",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = GrainTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isTelugu) "అన్ని ధాన్యం నిల్వ బ్యాచ్‌లు సురక్షిత సాధారణ పరిమితిలో ఉన్నాయి." 
                            else "All grain batches are within safe storage thresholds.",
                            fontSize = 12.sp,
                            color = GrainTextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredAlerts) { item ->
                        val isFault = item.statusCode == "SENSOR_FAULT" || item.reasonCode == "SENSOR_ERROR" || item.status == "HIGH_RISK"
                        val isHighMoisture = item.moisture > 14.0f
                        val isHotspot = item.t1Top > 35.0f || item.t2Middle > 35.0f || item.t3Bottom > 35.0f

                        val badgeColor = if (isFault) GrainDangerRed else if (isHotspot) Color(0xFFEA580C) else GrainAccentYellow
                        val badgeText = when {
                            isFault -> if (isTelugu) "సెన్సార్ లోపం" else "Sensor Fault"
                            isHotspot -> if (isTelugu) "వేడి మచ్చ" else "Hotspot Risk"
                            isHighMoisture -> if (isTelugu) "అధిక తేమ" else "High Moisture"
                            else -> if (isTelugu) "శ్రద్ధ అవసరం" else "Attention"
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAlertClick(item.id) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = GrainSurface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = badgeColor.copy(alpha = 0.15f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (isFault) Icons.Default.ErrorOutline else Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = badgeColor,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = badgeText,
                                                color = badgeColor,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    if (item.source == "SIMULATED") {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFFEF3C7)
                                        ) {
                                            Text(
                                                text = if (isTelugu) "డెమో" else "Demo data",
                                                color = Color(0xFF92400E),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "${item.cropName} • ${item.mode}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = GrainTextPrimary
                                        )
                                        Text(
                                            text = item.formattedDate,
                                            fontSize = 11.sp,
                                            color = GrainTextMuted
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = if (item.moisture > 0) String.format(java.util.Locale.US, "%.1f%%", item.moisture) else "--",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = if (isHighMoisture) GrainDangerRed else GrainTextPrimary
                                        )
                                        Text(
                                            text = if (item.t2Middle > 0) String.format(java.util.Locale.US, "%.1f°C", item.t2Middle) else "--",
                                            fontSize = 12.sp,
                                            color = if (isHotspot) Color(0xFFEA580C) else GrainTextSecondary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = item.recommendationsJson.replace("|", " • "),
                                    fontSize = 12.sp,
                                    color = GrainTextSecondary,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
