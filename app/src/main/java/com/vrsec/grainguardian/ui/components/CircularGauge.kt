package com.vrsec.grainguardian.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vrsec.grainguardian.ui.theme.GrainDivider
import com.vrsec.grainguardian.ui.theme.GrainPrimaryGreen
import com.vrsec.grainguardian.ui.theme.GrainTextPrimary
import com.vrsec.grainguardian.ui.theme.GrainTextSecondary

@Composable
fun CircularGauge(
    progressPercent: Int,
    size: Dp = 180.dp,
    strokeWidth: Dp = 12.dp,
    label: String = "Reading"
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progressPercent / 100f,
        animationSpec = tween(durationMillis = 600),
        label = "gauge_anim"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(size)
    ) {
        Canvas(modifier = Modifier.size(size)) {
            // Track
            drawArc(
                color = GrainDivider,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
            // Progress
            drawArc(
                color = GrainPrimaryGreen,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = GrainTextSecondary
            )
            Text(
                text = "$progressPercent%",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = GrainTextPrimary
            )
        }
    }
}
