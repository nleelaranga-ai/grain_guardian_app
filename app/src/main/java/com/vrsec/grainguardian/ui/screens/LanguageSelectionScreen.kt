package com.vrsec.grainguardian.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import com.vrsec.grainguardian.domain.localization.AppLanguage
import com.vrsec.grainguardian.ui.components.PrimaryActionButton
import com.vrsec.grainguardian.ui.theme.GrainBackground
import com.vrsec.grainguardian.ui.theme.GrainDarkGreen
import com.vrsec.grainguardian.ui.theme.GrainDivider
import com.vrsec.grainguardian.ui.theme.GrainSurface
import com.vrsec.grainguardian.ui.theme.GrainTextPrimary
import com.vrsec.grainguardian.ui.theme.GrainTextSecondary
import com.vrsec.grainguardian.viewmodel.GrainViewModel

@Composable
fun LanguageSelectionScreen(
    viewModel: GrainViewModel,
    onContinue: () -> Unit
) {
    val currentLang by viewModel.currentLanguage.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GrainBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Select Language",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = GrainTextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "భాషను ఎంచుకోండి",
                fontSize = 18.sp,
                color = GrainTextSecondary
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Telugu Option Card
            val isTelugu = currentLang == AppLanguage.TELUGU
            LanguageCard(
                mainTitle = "తెలుగు",
                subTitle = "Telugu",
                isSelected = isTelugu,
                onClick = { viewModel.setLanguage(AppLanguage.TELUGU) }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // English Option Card
            val isEnglish = currentLang == AppLanguage.ENGLISH
            LanguageCard(
                mainTitle = "English",
                subTitle = "ఆంగ్లం",
                isSelected = isEnglish,
                onClick = { viewModel.setLanguage(AppLanguage.ENGLISH) }
            )
        }

        PrimaryActionButton(
            text = if (currentLang == AppLanguage.TELUGU) "కొనసాగించండి (Continue)" else "Continue",
            onClick = onContinue,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun LanguageCard(
    mainTitle: String,
    subTitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val bgColor = if (isSelected) GrainDarkGreen else GrainSurface
    val textColor = if (isSelected) Color.White else GrainTextPrimary
    val subTextColor = if (isSelected) Color(0xFFC3EBD0) else GrainTextSecondary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clickable { onClick() }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) GrainDarkGreen else GrainDivider,
                shape = shape
            ),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = mainTitle,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = subTitle,
                    fontSize = 13.sp,
                    color = subTextColor
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
