package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = WhatsAppTeal,
    onPrimary = Color.Black,
    primaryContainer = WhatsAppDarkOutgoingBubble,
    onPrimaryContainer = WhatsAppDarkTextPrimary,
    secondary = WhatsAppAccentGreen,
    onSecondary = Color.Black,
    background = WhatsAppDarkBackground,
    onBackground = WhatsAppDarkTextPrimary,
    surface = WhatsAppDarkSurface,
    onSurface = WhatsAppDarkTextPrimary,
    surfaceVariant = WhatsAppDarkSurfaceVariant,
    onSurfaceVariant = WhatsAppDarkTextSecondary
)

private val LightColorScheme = lightColorScheme(
    primary = WhatsAppDarkTeal,
    onPrimary = Color.White,
    primaryContainer = WhatsAppLightOutgoingBubble,
    onPrimaryContainer = WhatsAppLightTextPrimary,
    secondary = WhatsAppHeaderGreen,
    onSecondary = Color.White,
    background = WhatsAppLightBackground,
    onBackground = WhatsAppLightTextPrimary,
    surface = WhatsAppLightSurface,
    onSurface = WhatsAppLightTextPrimary,
    surfaceVariant = WhatsAppLightSurfaceVariant,
    onSurfaceVariant = WhatsAppLightTextSecondary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
