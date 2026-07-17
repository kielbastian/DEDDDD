package pl.programtv.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import kotlin.math.abs

/** Kolory tła "kafelka" kanału, gdy nie ma loga – dobierane deterministycznie z nazwy. */
private val avatarColors = listOf(
    Color(0xFF3B82F6), Color(0xFF8B5CF6), Color(0xFFEC4899),
    Color(0xFFF97316), Color(0xFF14B8A6), Color(0xFF22C55E),
    Color(0xFFEAB308), Color(0xFFEF4444)
)

/**
 * Logo kanału – ładuje obrazek z URL, a w razie jego braku pokazuje
 * kolorowy kafelek z inicjałami nazwy.
 */
@Composable
fun ChannelLogo(
    name: String,
    iconUrl: String?,
    size: Dp = 44.dp
) {
    val shape = RoundedCornerShape(12.dp)
    if (!iconUrl.isNullOrBlank()) {
        SubcomposeAsyncImage(
            model = iconUrl,
            contentDescription = name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(size)
                .clip(shape)
                .background(Color.White),
            loading = { InitialsAvatar(name, size, shape) },
            error = { InitialsAvatar(name, size, shape) }
        )
    } else {
        InitialsAvatar(name, size, shape)
    }
}

@Composable
private fun InitialsAvatar(name: String, size: Dp, shape: RoundedCornerShape) {
    val color = avatarColors[abs(name.hashCode()) % avatarColors.size]
    val initials = name.trim()
        .split(" ", "-", "/")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifEmpty { "?" }

    Box(
        modifier = Modifier.size(size).clip(shape).background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            initials,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value / 2.6f).sp,
            style = MaterialTheme.typography.labelLarge
        )
    }
}
