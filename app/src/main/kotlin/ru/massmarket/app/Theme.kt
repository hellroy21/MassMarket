package ru.massmarket.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8E6C9),
    secondary = Color(0xFFFF8F00),
    background = Color(0xFFF7F8F4),
    surface = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF81C784),
    primaryContainer = Color(0xFF1B5E20),
    secondary = Color(0xFFFFB74D),
    background = Color(0xFF101510),
    surface = Color(0xFF192019)
)

@Composable
fun MassMarketTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}

