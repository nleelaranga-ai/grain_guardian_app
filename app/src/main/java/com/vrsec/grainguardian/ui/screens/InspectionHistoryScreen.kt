package com.vrsec.grainguardian.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vrsec.grainguardian.domain.localization.LocalizationManager
import com.vrsec.grainguardian.ui.components.GrainBottomBar
import com.vrsec.grainguardian.ui.components.GrainTopBar
import com.vrsec.grainguardian.ui.components.StatusBadge
import com.vrsec.grainguardian.ui.navigation.Screen
import com.vrsec.grainguardian.ui.theme.GrainBackground
import com.vrsec.grainguardian.ui.theme.GrainSurface
import com.vrsec.grainguardian.ui.theme.GrainTextPrimary
import com.vrsec.grainguardian.ui.theme.GrainTextSecondary
import com.vrsec.grainguardian.viewmodel.GrainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectionHistoryScreen(
    viewModel: GrainViewModel,
    onNavigate: (String) -> Unit,
    onInspectionClick: (Long) -> Unit
) {
    val isTelugu = LocalizationManager.isTelugu()
    val inspections by viewModel.allInspections.collectAsState()

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isTelugu) "గత తనిఖీల జాబితా (${inspections.size})" else "Past Records (${inspections.size})",
                    fontSize = 14.sp,
                    color = GrainTextSecondary
                )
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter",
                        tint = GrainTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (inspections.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isTelugu) "ఎలాంటి తనిఖీ రికార్డులు లేవు" else "No inspection history found",
                        color = GrainTextSecondary
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(inspections) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onInspectionClick(item.id) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = GrainSurface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
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
                        }
                    }
                }
            }
        }
    }
}