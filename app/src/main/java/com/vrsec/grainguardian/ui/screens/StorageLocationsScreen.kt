package com.vrsec.grainguardian.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

data class StorageLocationItem(
    val id: String,
    val nameEn: String,
    val nameTe: String,
    val typeEn: String,
    val typeTe: String,
    val capacityQuintals: Int,
    val bagsCount: Int,
    val lastInspectionDate: String,
    val isActive: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageLocationsScreen(
    viewModel: GrainViewModel,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val isTelugu = LocalizationManager.isTelugu()

    var locations by remember {
        mutableStateOf(
            listOf(
                StorageLocationItem(
                    id = "loc_1",
                    nameEn = "Main Concrete Shed 1",
                    nameTe = "ప్రధాన కాంక్రీట్ గోదాము 1",
                    typeEn = "Concrete Warehouse",
                    typeTe = "కాంక్రీట్ గోదాము",
                    capacityQuintals = 250,
                    bagsCount = 350,
                    lastInspectionDate = "Today, 10:15 AM",
                    isActive = true
                ),
                StorageLocationItem(
                    id = "loc_2",
                    nameEn = "North Corner Gunny Stack",
                    nameTe = "ఉత్తర మూల గన్నీ సంచుల దొంతర",
                    typeEn = "Gunny Bag Stack",
                    typeTe = "గన్నీ సంచుల దొంతర",
                    capacityQuintals = 120,
                    bagsCount = 160,
                    lastInspectionDate = "Yesterday, 04:30 PM",
                    isActive = false
                ),
                StorageLocationItem(
                    id = "loc_3",
                    nameEn = "Village Yard Drying Platform",
                    nameTe = "కళ్ళం ఎండబెట్టే ప్రదేశం",
                    typeEn = "Open Tarpaulin Yard",
                    typeTe = "కళ్ళం ఆరుబయలు టార్పాలిన్",
                    capacityQuintals = 80,
                    bagsCount = 100,
                    lastInspectionDate = "18 Oct 2026",
                    isActive = false
                )
            )
        )
    }

    var selectedLocationId by remember { mutableStateOf("loc_1") }
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        var newName by remember { mutableStateOf("") }
        var newType by remember { mutableStateOf("Gunny Bag Stack") }
        var newCapacity by remember { mutableStateOf("100") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = if (isTelugu) "కొత్త నిల్వ ప్రదేశాన్ని జోడించండి" else "Add New Storage Location",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text(if (isTelugu) "ప్రదేశం పేరు (ఉదా: షెడ్డు 2)" else "Location Name (e.g. Shed 2)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newType,
                        onValueChange = { newType = it },
                        label = { Text(if (isTelugu) "రకం (గోదాము / సంచులు / సైలో)" else "Type (Warehouse / Bags / Silo)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newCapacity,
                        onValueChange = { newCapacity = it },
                        label = { Text(if (isTelugu) "సామర్థ్యం (క్వింటాళ్ళు)" else "Capacity (Quintals)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            val cap = newCapacity.toIntOrNull() ?: 100
                            val newItem = StorageLocationItem(
                                id = "loc_${System.currentTimeMillis()}",
                                nameEn = newName,
                                nameTe = newName,
                                typeEn = newType,
                                typeTe = newType,
                                capacityQuintals = cap,
                                bagsCount = (cap * 1.3).toInt(),
                                lastInspectionDate = "Just now",
                                isActive = false
                            )
                            locations = locations + newItem
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GrainPrimaryGreen)
                ) {
                    Text(if (isTelugu) "చేర్చండి" else "Add Site")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(if (isTelugu) "రద్దు" else "Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            GrainTopBar(
                title = if (isTelugu) "ధాన్యం నిల్వ ప్రదేశాలు" else "Storage Locations",
                showBackButton = true,
                onBackClick = onBack
            )
        },
        bottomBar = {
            GrainBottomBar(
                currentRoute = Screen.StorageLocations.route,
                onNavigate = onNavigate
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = GrainPrimaryGreen,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Location")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(GrainBackground)
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Header card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = GrainSurface)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = GrainPrimaryGreen,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isTelugu) "ధాన్యం నిల్వ & కళ్ళం ప్రదేశాలు" else "Granaries & Bag Stacks",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = GrainTextPrimary
                        )
                        Text(
                            text = if (isTelugu) "తనిఖీ చేసే సమయంలో సంబంధిత నిల్వ ప్రదేశాన్ని ఎంచుకోండి." 
                            else "Select active storage site to associate with probe measurements.",
                            fontSize = 12.sp,
                            color = GrainTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (isTelugu) "నమోదైన ప్రదేశాలు (${locations.size})" else "Registered Locations (${locations.size})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = GrainTextPrimary
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(locations) { loc ->
                    val isSelected = loc.id == selectedLocationId

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedLocationId = loc.id },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFFF0FDF4) else GrainSurface
                        ),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, GrainPrimaryGreen) else null,
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (loc.capacityQuintals > 200) Icons.Default.Warehouse else Icons.Default.Layers,
                                        contentDescription = null,
                                        tint = if (isSelected) GrainPrimaryGreen else GrainTextSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isTelugu) loc.nameTe else loc.nameEn,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = GrainTextPrimary
                                    )
                                }

                                if (isSelected) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = GrainLightGreen
                                    ) {
                                        Text(
                                            text = if (isTelugu) "ప్రస్తుతం ఎంచుకున్నది" else "Active",
                                            color = GrainDarkGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "${if (isTelugu) "రకం" else "Type"}: ${if (isTelugu) loc.typeTe else loc.typeEn}",
                                    fontSize = 12.sp,
                                    color = GrainTextSecondary
                                )
                                Text(
                                    text = "${if (isTelugu) "సామర్థ్యం" else "Capacity"}: ${loc.capacityQuintals} Q",
                                    fontSize = 12.sp,
                                    color = GrainTextSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "${if (isTelugu) "చివరి తనిఖీ" else "Last Inspection"}: ${loc.lastInspectionDate}",
                                fontSize = 11.sp,
                                color = GrainTextMuted
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        selectedLocationId = loc.id
                                        viewModel.startStorageCheckFlow()
                                        onNavigate(Screen.CropSelection.route)
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GrainPrimaryGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isTelugu) "ఈ ప్రదేశాన్ని తనిఖీ చేయండి" else "Inspect This Site",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
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
