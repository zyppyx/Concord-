package com.example.concordmobile_android.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ConcordPrimary = Color(0xFF5865F2)
val ConcordPrimaryDark = Color(0xFF34248F)
val ConcordSidebar = Color(0xFF0C0E16)
val ConcordPanel = Color(0xFF171923)
val ConcordSurface = Color(0xFF20232D)
val ConcordField = Color(0xFF2A2D3A)
val ConcordBubbleMine = Color(0xFF3A3F91)
val ConcordBubbleOther = Color(0xFF262936)

private val LightColors: ColorScheme = lightColorScheme(
    primary = ConcordPrimary,
    onPrimary = Color.White,
    secondary = ConcordPrimaryDark,
    surface = Color(0xFFF2F3F8),
    background = Color(0xFFE8EAF3),
    onSurface = Color(0xFF151721),
    onBackground = Color(0xFF151721)
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = ConcordPrimary,
    onPrimary = Color.White,
    secondary = Color(0xFF7C83FF),
    surface = ConcordSurface,
    background = ConcordPanel,
    onSurface = Color.White,
    onBackground = Color.White
)

@Composable
fun ConcordTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
