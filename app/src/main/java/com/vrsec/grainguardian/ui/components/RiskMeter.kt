package com.vrsec.grainguardian.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vrsec.grainguardian.domain.localization.LocalizationManager
import com.vrsec.grainguardian.ui.theme.GrainDangerRed
import com.vrsec.grainguardian.ui.theme.GrainPrimaryGreen
import com.vrsec.grainguardian.ui.theme.GrainTextSecondary
import com.vrsec.grainguardian.ui.theme.GrainWarningOrange

@Composable
fun RiskMeter(
    riskScore: Int, // 0 to 100
    modifier: Modifier = Modifier
) {
    val isTelugu = LocalizationManager.isTelugu()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (isTelugu) "ప్రమాద తీవ్రత" else "Risk Level",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = GrainTextSecondary
            )
            Text(
                text = "$riskScore / 100",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    riskScore < 35 -> GrainPrimaryGreen
                    riskScore < 70 -> GrainWarningOrange
                    else -> GrainDangerRed
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            GrainPrimaryGreen,
                            GrainWarningOrange,
                            GrainDangerRed
                        )
                    )
                )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (isTelugu) "తక్కువ" else "LOW",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = GrainPrimaryGreen
            )
            Text(
                text = if (isTelugu) "మధ్యస్థం" else "MODERATE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = GrainWarningOrange
            )
            Text(
                text = if (isTelugu) "అత్యధికం" else "HIGH",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = GrainDangerRed
            )
        }
    }
}
