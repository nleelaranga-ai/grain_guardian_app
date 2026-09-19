package com.vrsec.grainguardian.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vrsec.grainguardian.data.model.RiskStatus
import com.vrsec.grainguardian.domain.localization.LocalizationManager
import com.vrsec.grainguardian.ui.theme.GrainDangerBorder
import com.vrsec.grainguardian.ui.theme.GrainDangerRed
import com.vrsec.grainguardian.ui.theme.GrainDarkGreen
import com.vrsec.grainguardian.ui.theme.GrainLightDanger
import com.vrsec.grainguardian.ui.theme.GrainLightGreen
import com.vrsec.grainguardian.ui.theme.GrainLightGreenBorder
import com.vrsec.grainguardian.ui.theme.GrainLightWarning
import com.vrsec.grainguardian.ui.theme.GrainPrimaryGreen
import com.vrsec.grainguardian.ui.theme.GrainWarningBorder
import com.vrsec.grainguardian.ui.theme.GrainWarningOrange

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val isTelugu = LocalizationManager.isTelugu()

    val (bgColor, textColor, borderColor, text) = when (status.uppercase()) {
        "SAFE", "సురక్షితం" -> Quadruple(
            GrainLightGreen,
            GrainDarkGreen,
            GrainLightGreenBorder,
            if (isTelugu) "సురక్షితం" else "SAFE"
        )
        "WARNING", "హెచ్చరిక" -> Quadruple(
            GrainLightWarning,
            GrainWarningOrange,
            GrainWarningBorder,
            if (isTelugu) "హెచ్చరిక" else "WARNING"
        )
        else -> Quadruple(
            GrainLightDanger,
            GrainDangerRed,
            GrainDangerBorder,
            if (isTelugu) "అత్యధిక ప్రమాదం" else "HIGH RISK"
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
