package com.theaterphone.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF0A84FF),       // iOS blue
    onPrimary = Color.White,
    secondary = Color(0xFF30D158),     // iOS green
    background = Color.Black,
    surface = Color(0xFF1C1C1E),       // iOS dark gray
    onBackground = Color.White,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2C2C2E),
    outline = Color(0xFF48484A),
)

@Composable
fun TheaterPhoneTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
