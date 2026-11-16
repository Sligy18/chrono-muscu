package fr.gillesray.chronomuscu.ui

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

private val Amber = Color(0xFFFFB300)
private val Navy = Color(0xFF1B2A3A)

private val LightColors = lightColorScheme(
    primary = Color(0xFF7A5900),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDEA6),
    onPrimaryContainer = Color(0xFF261900),
    secondary = Navy,
    onSecondary = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Amber,
    onPrimary = Color(0xFF402D00),
    primaryContainer = Color(0xFF5C4200),
    onPrimaryContainer = Color(0xFFFFDEA6),
    secondary = Color(0xFFB9C8DA),
    background = Color(0xFF10161D),
    surface = Color(0xFF10161D),
)

@Composable
fun ChronoTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
