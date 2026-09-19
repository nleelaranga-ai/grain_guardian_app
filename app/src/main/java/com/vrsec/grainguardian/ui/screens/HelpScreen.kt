package com.vrsec.grainguardian.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vrsec.grainguardian.R
import com.vrsec.grainguardian.domain.localization.LocalizationManager
import com.vrsec.grainguardian.ui.components.GrainBottomBar
import com.vrsec.grainguardian.ui.components.GrainTopBar
import com.vrsec.grainguardian.ui.navigation.Screen
import com.vrsec.grainguardian.ui.theme.GrainBackground
import com.vrsec.grainguardian.ui.theme.GrainDarkGreen
import com.vrsec.grainguardian.ui.theme.GrainSurface
import com.vrsec.grainguardian.ui.theme.GrainTextPrimary
import com.vrsec.grainguardian.ui.theme.GrainTextSecondary
import com.vrsec.grainguardian.viewmodel.GrainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    viewModel: GrainViewModel,
    onNavigate: (String) -> Unit
) {
    val isTelugu = LocalizationManager.isTelugu()

    val faqs = if (isTelugu) listOf(
        FaqItem(
            "సురక్షితం (SAFE) అంటే ఏమిటి?",
            "ధాన్యం తేమ శాతం సురక్షిత పరిమితి (13.5% లోపు) మరియు ఉష్ణోగ్రత సాధారణంగా ఉందని అర్థం. ధాన్యాన్ని ఎలాంటి నష్టం లేకుండా నిల్వ చేయవచ్చు."
        ),
        FaqItem(
            "హెచ్చరిక (WARNING) అంటే ఏమిటి?",
            "తేమ కొద్దిగా ఎక్కువగా ఉండటం లేదా ఉష్ణోగ్రత పెరుగుదల గమనించబడింది. ధాన్యాన్ని తిరగేయడం లేదా పగటిపూట ఎండబెట్టడం అవసరం."
        ),
        FaqItem(
            "అత్యధిక ప్రమాదం (HIGH RISK) అంటే ఏమిటి?",
            "అధిక తేమ లేదా లోపలి వేడి వల్ల బూజు, పురుగులు పట్టే తీవ్ర ముప్పు ఉంది. వెంటనే ధాన్యాన్ని బయటకు తీసి ఎండబెట్టాలి."
        ),
        FaqItem(
            "ప్రోబ్‌ను ఎలా ఉపయోగించాలి?",
            "ప్రోబ్‌ను ధాన్యపు కుప్ప మధ్యలోకి సమాంతరంగా దింపండి. రీడింగ్ పూర్తయ్యే వరకు 5-6 సెకన్ల పాటు కదల్చకుండా ఉంచండి."
        ),
        FaqItem(
            "బ్లూటూత్ ఎలా కనెక్ట్ చేయాలి?",
            "ఫోన్‌లో బ్లూటూత్ ఆన్ చేసి, గ్రెయిన్ గార్డియన్ పరికరాన్ని ఆన్ చేయండి. యాప్‌లో 'కనెక్ట్' నొక్కి సులభంగా జత చేయండి."
        )
    ) else listOf(
        FaqItem(
            "What does SAFE mean?",
            "SAFE indicates that the grain moisture content is within optimal storage limits (<=13.5% for Paddy) and temperature gradients are stable. The grain is safe for storage."
        ),
        FaqItem(
            "What does WARNING mean?",
            "WARNING means slight moisture elevation or localized heat accumulation was detected. Aeration or turning the grain is advised to prevent mold."
        ),
        FaqItem(
            "What does HIGH RISK mean?",
            "HIGH RISK indicates severe biological degradation risk (high moisture >15.5% or hotspot >35°C). Immediate sun drying and grain spreading is critical."
        ),
        FaqItem(
            "How do I use the probe?",
            "Insert the probe vertically or diagonally into the core of the grain bag or pile. Hold firmly for 5-6 seconds while readings stabilize."
        ),
        FaqItem(
            "How do I connect Bluetooth?",
            "Enable Bluetooth on your phone, power on the GrainGuardian probe, and tap 'Connect Device' on the probe screen."
        )
    )

    Scaffold(
        topBar = {
            GrainTopBar(
                title = if (isTelugu) "సహాయం & తరచుగా అడిగే ప్రశ్నలు" else "Help & FAQs",
                showBackButton = false,
                showMenuButton = true,
                onMenuClick = { onNavigate(Screen.Settings.route) }
            )
        },
        bottomBar = {
            GrainBottomBar(
                currentRoute = Screen.Help.route,
                onNavigate = onNavigate
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(GrainBackground)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            faqs.forEach { faq ->
                FaqCard(faq = faq)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Farmer support illustration card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8F3)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_farmer),
                        contentDescription = "Farmer avatar",
                        modifier = Modifier.size(60.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = if (isTelugu) "రైతు సహాయ కేంద్రం" else "Farmer Support Portal",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = GrainDarkGreen
                        )
                        Text(
                            text = if (isTelugu) "గ్రెయిన్‌గార్డియన్ అగ్రిటెక్ సపోర్ట్" else "GrainGuardian Agritech Support",
                            fontSize = 12.sp,
                            color = GrainTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FaqCard(faq: FaqItem) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = GrainSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = faq.question,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = GrainTextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = GrainTextSecondary
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = faq.answer,
                        fontSize = 13.sp,
                        color = GrainTextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

private data class FaqItem(val question: String, val answer: String)