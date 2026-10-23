package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = Navy900,
    onPrimary = Color.White,
    primaryContainer = Navy100,
    onPrimaryContainer = Navy900,
    secondary = Teal700,
    onSecondary = Color.White,
    secondaryContainer = Teal100,
    onSecondaryContainer = Color(0xFF002024),
    tertiary = Amber700,
    onTertiary = Color.White,
    tertiaryContainer = Amber100,
    onTertiaryContainer = Color(0xFF451A03),
    background = Color(0xFFF8FAFC),
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Slate700,
    outline = Slate300,
    outlineVariant = Color(0xFFE2E8F0),
    error = Red700,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Navy900,
    primaryContainer = Navy800,
    onPrimaryContainer = Color(0xFFDCE6F2),
    secondary = Color(0xFF4DD0E1),
    onSecondary = Color(0xFF00363D),
    secondaryContainer = Teal700,
    onSecondaryContainer = Teal100,
    tertiary = Color(0xFFFFB74D),
    onTertiary = Color(0xFF4E2C00),
    background = Color(0xFF0B1320),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF131F33),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1B2A44),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF475569)
)

@Composable
fun BharatBillTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our crisp branded FinTech palette by default
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
