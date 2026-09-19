package com.vrsec.grainguardian.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vrsec.grainguardian.R
import com.vrsec.grainguardian.domain.localization.LocalizationManager
import com.vrsec.grainguardian.ui.components.PrimaryActionButton
import com.vrsec.grainguardian.ui.theme.GrainBackground
import com.vrsec.grainguardian.ui.theme.GrainDarkGreen
import com.vrsec.grainguardian.ui.theme.GrainLightGreen
import com.vrsec.grainguardian.ui.theme.GrainPrimaryGreen
import com.vrsec.grainguardian.ui.theme.GrainTextPrimary
import com.vrsec.grainguardian.ui.theme.GrainTextSecondary

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit
) {
    val isTelugu = LocalizationManager.isTelugu()

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
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8F3)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_farmer),
                            contentDescription = "Farmer Avatar",
                            modifier = Modifier.size(90.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isTelugu) "రైతు మిత్రుడు" else "Farmer First Intelligence",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = GrainDarkGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isTelugu) "గ్రెయిన్ గార్డియన్‌కు స్వాగతం" else "Welcome to GrainGuardian",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = GrainDarkGreen
            )

            Spacer(modifier = Modifier.height(20.dp))

            val bulletPoints = if (isTelugu) listOf(
                "మీ ధాన్యం నాణ్యతను సులభంగా తనిఖీ చేయండి",
                "నిల్వ పరిస్థితి మరియు తేమను అర్థం చేసుకోండి",
                "సరైన సమయంలో తగిన చర్యలు తీసుకోండి",
                "కోత అనంతర నష్టాలను పూర్తిగా నివారించండి"
            ) else listOf(
                "Check your grain readiness accurately",
                "Understand moisture and storage condition",
                "Take actionable, localized recommendations",
                "Reduce post-harvest losses & save revenue"
            )

            bulletPoints.forEach { point ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(GrainLightGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = GrainPrimaryGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = point,
                        fontSize = 14.sp,
                        color = GrainTextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        PrimaryActionButton(
            text = if (isTelugu) "ప్రారంభించండి (Get Started)" else "Get Started",
            onClick = onGetStarted,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}
