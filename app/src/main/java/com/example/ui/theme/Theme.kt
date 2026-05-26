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

private val DarkColorScheme = darkColorScheme(
    primary = ShopeeOrange,
    secondary = ShopeeDarkOrange,
    tertiary = AccentAmber,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = DarkCardBorder
)

private val LightColorScheme = lightColorScheme(
    primary = ShopeeOrange,
    secondary = ShopeeDarkOrange,
    tertiary = AccentAmber,
    background = Color(0xFFFAFAFC),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color(0xFF1C1C1F),
    onSurface = Color(0xFF1C1C1F),
    surfaceVariant = Color(0xFFF2F2F7),
    onSurfaceVariant = Color(0xFF5E5E66),
    outline = Color(0xFFE5E5EA)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to premium dark theme
    dynamicColor: Boolean = false, // Preserve original brand colors consistently
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
