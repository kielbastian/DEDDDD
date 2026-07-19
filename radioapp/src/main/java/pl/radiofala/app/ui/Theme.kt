package pl.radiofala.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RadioColors = darkColorScheme(
    primary = Color(0xFFB79CFF),
    onPrimary = Color(0xFF1E1240),
    primaryContainer = Color(0xFF3A2A70),
    onPrimaryContainer = Color(0xFFE7DEFF),
    secondary = Color(0xFF46C7D8),
    onSecondary = Color(0xFF00272B),
    secondaryContainer = Color(0xFF0E3A40),
    onSecondaryContainer = Color(0xFFB8F1FC),
    background = Color(0xFF120B22),
    onBackground = Color(0xFFEDE8FF),
    surface = Color(0xFF1B1236),
    onSurface = Color(0xFFEDE8FF),
    surfaceVariant = Color(0xFF2A1F4D),
    onSurfaceVariant = Color(0xFFB1A6D4),
    outline = Color(0xFF463A6E),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF2B0000)
)

@Composable
fun RadioFalaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = RadioColors, content = content)
}
