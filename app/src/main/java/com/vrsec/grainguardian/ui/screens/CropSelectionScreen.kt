package com.vrsec.grainguardian.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vrsec.grainguardian.data.model.CropType
import com.vrsec.grainguardian.domain.localization.LocalizationManager
import com.vrsec.grainguardian.ui.components.GrainTopBar
import com.vrsec.grainguardian.ui.components.PrimaryActionButton
import com.vrsec.grainguardian.ui.theme.GrainBackground
import com.vrsec.grainguardian.ui.theme.GrainDivider
import com.vrsec.grainguardian.ui.theme.GrainLightGreen
import com.vrsec.grainguardian.ui.theme.GrainPrimaryGreen
import com.vrsec.grainguardian.ui.theme.GrainSurface
import com.vrsec.grainguardian.ui.theme.GrainTextPrimary
import com.vrsec.grainguardian.ui.theme.GrainTextSecondary
import com.vrsec.grainguardian.viewmodel.GrainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropSelectionScreen(
    viewModel: GrainViewModel,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val isTelugu = LocalizationManager.isTelugu()
    val selectedCrop by viewModel.selectedCrop.collectAsState()

    val cropItems = listOf(
        CropItem(CropType.PADDY, "Paddy", "వరి (వరి ధాన్యం)", Icons.Default.Grass, isRecommended = true),
        CropItem(CropType.MAIZE, "Maize", "మొక్కజొన్న", Icons.Default.Eco),
        CropItem(CropType.WHEAT, "Wheat", "గోధుమ", Icons.Default.Grain),
        CropItem(CropType.PULSES, "Pulses", "పప్పు ధాన్యాలు", Icons.Default.Spa),
        CropItem(CropType.OTHER, "Other / Add Crop", "ఇతర పంటలు", Icons.Default.Eco)
    )

    Scaffold(
        topBar = {
            GrainTopBar(
                title = if (isTelugu) "పంటను ఎంచుకోండి" else "Select Crop",
                onBackClick = onBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(GrainBackground)
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = if (isTelugu) "మీరు ఏ ధాన్యాన్ని తనిఖీ చేయాలనుకుంటున్నారు?" else "What grain are you checking?",
                    fontSize = 14.sp,
                    color = GrainTextSecondary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(cropItems) { item ->
                        val isSelected = selectedCrop == item.crop
                        val shape = RoundedCornerShape(14.dp)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectCrop(item.crop) }
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) GrainPrimaryGreen else GrainDivider,
                                    shape = shape
                                ),
                            shape = shape,
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) GrainLightGreen else GrainSurface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) GrainPrimaryGreen else Color(0xFFF1F5F0)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.name,
                                            tint = if (isSelected) Color.White else GrainPrimaryGreen,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = if (isTelugu) item.teluguName else item.name,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = GrainTextPrimary
                                            )
                                            if (item.isRecommended) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(GrainPrimaryGreen)
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = if (isTelugu) "సిఫార్సు చేయబడింది" else "Primary Crop",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "Safe limit: ${item.crop.safeMoistureThreshold}% moisture",
                                            fontSize = 12.sp,
                                            color = GrainTextSecondary
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = GrainPrimaryGreen,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = GrainTextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            PrimaryActionButton(
                text = if (isTelugu) "కొనసాగించండి (Continue)" else "Continue",
                onClick = onContinue,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}

private data class CropItem(
    val crop: CropType,
    val name: String,
    val teluguName: String,
    val icon: ImageVector,
    val isRecommended: Boolean = false
)