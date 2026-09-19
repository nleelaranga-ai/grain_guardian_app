package com.vrsec.grainguardian.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vrsec.grainguardian.domain.localization.LocalizationManager
import com.vrsec.grainguardian.ui.navigation.Screen
import com.vrsec.grainguardian.ui.theme.GrainDivider
import com.vrsec.grainguardian.ui.theme.GrainPrimaryGreen
import com.vrsec.grainguardian.ui.theme.GrainTextMuted
import com.vrsec.grainguardian.ui.theme.GrainTextSecondary

@Composable
fun GrainBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val isTelugu = LocalizationManager.isTelugu()

    val navItems = listOf(
        NavItem(
            route = Screen.Dashboard.route,
            label = if (isTelugu) "హోమ్" else "Home",
            icon = Icons.Default.Home
        ),
        NavItem(
            route = Screen.InspectionHistory.route,
            label = if (isTelugu) "చరిత్ర" else "History",
            icon = Icons.Default.History
        ),
        NavItem(
            route = Screen.Help.route,
            label = if (isTelugu) "సహాయం" else "Help",
            icon = Icons.Default.HelpOutline
        ),
        NavItem(
            route = Screen.Settings.route,
            label = if (isTelugu) "సెట్టింగ్స్" else "Settings",
            icon = Icons.Default.Settings
        )
    )

    Column(modifier = Modifier.fillMaxWidth().background(Color.White)) {
        Divider(color = GrainDivider, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEach { item ->
                val isSelected = currentRoute == item.route

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate(item.route) }
                        .padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (isSelected) GrainPrimaryGreen else GrainTextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = item.label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) GrainPrimaryGreen else GrainTextSecondary
                    )
                }
            }
        }
    }
}

private data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)
