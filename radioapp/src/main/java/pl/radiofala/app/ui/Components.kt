package pl.radiofala.app.ui

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import kotlin.math.abs

private val avatarColors = listOf(
    Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF3B82F6),
    Color(0xFF22C55E), Color(0xFFF97316), Color(0xFF46C7D8),
    Color(0xFFEAB308), Color(0xFFEF4444)
)

/**
 * Logo stacji – favicon albo kolorowy kafelek z inicjałami nazwy.
 * Używamy prostego AsyncImage (bez subkompozycji) nałożonego na zawsze obecne
 * inicjały, bo SubcomposeAsyncImage w długiej przewijanej liście zauważalnie
 * spowalniało scrollowanie (dodatkowy przebieg pomiaru na każdy wiersz).
 */
@Composable
fun StationLogo(name: String, faviconUrl: String?, size: Dp = 48.dp) {
    val shape = RoundedCornerShape(14.dp)
    if (!faviconUrl.isNullOrBlank()) {
        var loaded by remember(faviconUrl) { mutableStateOf(false) }
        Box(modifier = Modifier.width(size).height(size).clip(shape).background(Color.White)) {
            InitialsAvatar(name, size, shape)
            AsyncImage(
                model = faviconUrl,
                contentDescription = name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().alpha(if (loaded) 1f else 0f),
                onState = { state -> loaded = state is AsyncImagePainter.State.Success }
            )
        }
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
        modifier = Modifier.width(size).height(size).clip(shape).background(color),
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

/**
 * Animowane paski korektora – widoczne, gdy stacja gra.
 * Wszystkie 4 paski jeżdżą na jednej wspólnej transition (zamiast osobnej na
 * pasek), więc podczas przewijania listy nie ma 4 niezależnych callbacków
 * animacji na klatkę, tylko jeden.
 */
@Composable
fun EqualizerBars(
    playing: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    barWidth: Dp = 4.dp,
    barMaxHeight: Dp = 18.dp
) {
    val transition = rememberInfiniteTransition(label = "equalizer")
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        repeat(4) { index ->
            EqualizerBar(transition, playing, index, barColor, barWidth, barMaxHeight)
            if (index != 3) Box(Modifier.width(barWidth * 0.75f))
        }
    }
}

@Composable
private fun EqualizerBar(
    transition: androidx.compose.animation.core.InfiniteTransition,
    playing: Boolean,
    index: Int,
    color: Color,
    barWidth: Dp,
    barMaxHeight: Dp
) {
    val height by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = if (playing) 1f else 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 420 + index * 90, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eqHeight$index"
    )
    Box(
        modifier = Modifier
            .width(barWidth)
            .height(barMaxHeight * height)
            .clip(RoundedCornerShape(2.dp))
            .background(color)
    )
}
