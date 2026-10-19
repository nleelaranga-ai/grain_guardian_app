package com.vrsec.grainguardian.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vrsec.grainguardian.data.model.GrainInspectionEntity
import com.vrsec.grainguardian.domain.localization.LocalizationManager
import com.vrsec.grainguardian.ui.components.GrainBottomBar
import com.vrsec.grainguardian.ui.components.GrainTopBar
import com.vrsec.grainguardian.ui.components.StatusBadge
import com.vrsec.grainguardian.ui.navigation.Screen
import com.vrsec.grainguardian.viewmodel.GrainViewModel
import com.vrsec.grainguardian.ui.theme.GrainBackground
import com.vrsec.grainguardian.ui.theme.GrainDarkGreen
import com.vrsec.grainguardian.ui.theme.GrainLightGreen
import com.vrsec.grainguardian.ui.theme.GrainPrimaryGreen
import com.vrsec.grainguardian.ui.theme.GrainSurface
import com.vrsec.grainguardian.ui.theme.GrainTextPrimary
import com.vrsec.grainguardian.ui.theme.GrainTextSecondary

enum class HistoryFilter { ALL, WARNINGS, DEVICE_PROBLEMS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectionHistoryScreen(
    viewModel: GrainViewModel,
    onNavigate: (String) -> Unit,
    onInspectionClick: (Long) -> Unit
) {
    val isTelugu = LocalizationManager.isTelugu()
    val allInspections by viewModel.allInspections.collectAsState()
    val warningInspections by viewModel.warningInspections.collectAsState()
    val deviceProblemInspections by viewModel.deviceProblemInspections.collectAsState()

    var selectedFilter by remember { mutableStateOf(HistoryFilter.ALL) }

    val currentList = when (selectedFilter) {
        HistoryFilter.ALL -> allInspections
        HistoryFilter.WARNINGS -> warningInspections
        HistoryFilter.DEVICE_PROBLEMS -> deviceProblemInspections
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var exportMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            GrainTopBar(
                title = if (isTelugu) "తనిఖీ చరిత్ర" else "Inspection History",
                showBackButton = false,
                showMenuButton = true,
                onMenuClick = { onNavigate(Screen.Settings.route) }
            )
        },
        bottomBar = {
            GrainBottomBar(
                currentRoute = Screen.InspectionHistory.route,
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
            // Header Stats & Export Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isTelugu) "మొత్తం: ${currentList.size} రికార్డులు" else "Total: ${currentList.size} records",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GrainTextSecondary
                )
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                val file = viewModel.exportInspectionsToCsv(context)
                                exportMessage = if (isTelugu) "CSV గా ఎగుమతి చేయబడింది (${file.name})" else "Exported to ${file.name}"
                            } catch (e: Exception) {
                                exportMessage = "Export failed: ${e.message}"
                            }
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    enabled = currentList.isNotEmpty()
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = GrainPrimaryGreen
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isTelugu) "CSV ఎగుమతి" else "Export CSV",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GrainPrimaryGreen
                    )
                }
            }

            exportMessage?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF0FDF4),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                ) {
                    Text(
                        text = msg,
                        fontSize = 11.sp,
                        color = GrainDarkGreen,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == HistoryFilter.ALL,
                        onClick = { selectedFilter = HistoryFilter.ALL },
                        label = {
                            Text(if (isTelugu) "అన్నీ (${allInspections.size})" else "All (${allInspections.size})")
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GrainPrimaryGreen,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == HistoryFilter.WARNINGS,
                        onClick = { selectedFilter = HistoryFilter.WARNINGS },
                        label = {
                            Text(if (isTelugu) "హెచ్చరికలు (${warningInspections.size})" else "Warnings (${warningInspections.size})")
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFD97706),
                            selectedLabelColor = Color.White
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == HistoryFilter.DEVICE_PROBLEMS,
                        onClick = { selectedFilter = HistoryFilter.DEVICE_PROBLEMS },
                        label = {
                            Text(if (isTelugu) "పరికర సమస్యలు (${deviceProblemInspections.size})" else "Device Problems (${deviceProblemInspections.size})")
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFDC2626),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            if (currentList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isTelugu) "ఎలాంటి తనిఖీ రికార్డులు లేవు" else "No inspection records found",
                        color = GrainTextSecondary
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(currentList) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onInspectionClick(item.id) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = GrainSurface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = item.formattedDate,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = GrainTextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${item.cropName} • ${item.mode}",
                                            fontSize = 12.sp,
                                            color = GrainTextSecondary
                                        )
                                    }

                                    StatusBadge(status = item.status)
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Sync State Badge
                                    val isSynced = item.syncState == "CONFIRMED"
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSynced) GrainLightGreen else Color(0xFFF1F5F9)
                                    ) {
                                        Text(
                                            text = if (isSynced) {
                                                if (isTelugu) "✓ సింక్ చేయబడింది" else "✓ Synced"
                                            } else {
                                                if (isTelugu) "📱 ఈ ఫోన్‌లో భద్రపరచబడింది" else "📱 Saved on this phone"
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isSynced) GrainDarkGreen else Color(0xFF475569),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }

                                    // Simulation Badge if applicable
                                    if (item.source == "SIMULATED") {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFFEF3C7)
                                        ) {
                                            Text(
                                                text = if (isTelugu) "⚠️ డెమో డేటా" else "⚠️ Demo data",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF92400E),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}