package com.vrsec.grainguardian.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = GrainPrimaryGreen,
    onPrimary = GrainSurface,
    primaryContainer = GrainLightGreen,
    onPrimaryContainer = GrainDarkGreen,
    secondary = GrainDarkGreen,
    onSecondary = GrainSurface,
    background = GrainBackground,
    onBackground = GrainTextPrimary,
    surface = GrainSurface,
    onSurface = GrainTextPrimary,
    surfaceVariant = GrainSurfaceVariant,
    onSurfaceVariant = GrainTextSecondary,
    error = GrainDangerRed,
    onError = GrainSurface
)

@Composable
fun GrainGuardianTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = GrainDarkGreen.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
