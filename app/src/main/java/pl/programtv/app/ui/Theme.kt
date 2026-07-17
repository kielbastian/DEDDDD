package pl.programtv.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Ciemny motyw "panelu sterowania": granatowe tła + turkusowy akcent.
private val DarkColors = darkColorScheme(
    primary = Color(0xFF2DD4BF),
    onPrimary = Color(0xFF00201B),
    primaryContainer = Color(0xFF124A42),
    onPrimaryContainer = Color(0xFF8EF2E2),
    secondary = Color(0xFF67E8F9),
    onSecondary = Color(0xFF00252B),
    secondaryContainer = Color(0xFF0E3A44),
    onSecondaryContainer = Color(0xFFB8F1FC),
    background = Color(0xFF0B1220),
    onBackground = Color(0xFFE4EAF2),
    surface = Color(0xFF121B2B),
    onSurface = Color(0xFFE4EAF2),
    surfaceVariant = Color(0xFF1B2740),
    onSurfaceVariant = Color(0xFF9AA8BF),
    outline = Color(0xFF32405C),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF2B0000)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF00796B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB2F1E4),
    onPrimaryContainer = Color(0xFF00201B),
    secondary = Color(0xFF0288D1),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCBEFFF),
    onSecondaryContainer = Color(0xFF001F2A),
    background = Color(0xFFF4F7FB),
    onBackground = Color(0xFF16202E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF16202E),
    surfaceVariant = Color(0xFFE3EAF4),
    onSurfaceVariant = Color(0xFF51607A),
    outline = Color(0xFFB4C0D4)
)

@Composable
fun ProgramTvTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
