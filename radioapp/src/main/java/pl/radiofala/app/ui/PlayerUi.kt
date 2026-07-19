package pl.radiofala.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pl.radiofala.app.playback.PlaybackUiState

/** Pasek mini-odtwarzacza przyklejony u dołu ekranu – dotknięcie otwiera pełny widok. */
@Composable
fun MiniPlayerBar(
    playback: PlaybackUiState,
    faviconUrl: String?,
    onExpand: () -> Unit,
    onTogglePlayPause: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onExpand),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StationLogo(playback.title ?: "?", faviconUrl, size = 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    playback.title ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (playback.isBuffering) "Buforowanie…" else playback.subtitle ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (playback.isBuffering) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = onTogglePlayPause) {
                    Icon(
                        if (playback.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (playback.isPlaying) "Pauza" else "Odtwórz",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            IconButton(onClick = onExpand) {
                Icon(
                    Icons.Filled.KeyboardArrowUp,
                    contentDescription = "Maksymalizuj odtwarzacz",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Pełnoekranowy odtwarzacz z dużym logo, korektorem i timerem snu. */
@Composable
fun FullPlayerScreen(
    playback: PlaybackUiState,
    faviconUrl: String?,
    isFavorite: Boolean,
    sleepMinutesActive: Int?,
    canSkip: Boolean,
    onToggleFavorite: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onStop: () -> Unit,
    onSetSleepTimer: (Int) -> Unit,
    onCancelSleepTimer: () -> Unit,
    onCollapse: () -> Unit
) {
    var showSleepDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.background)
                )
            )
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = onCollapse) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Zwiń")
                }
                IconButton(onClick = { showSleepDialog = true }) {
                    Icon(
                        Icons.Filled.Timer,
                        contentDescription = "Timer snu",
                        tint = if (sleepMinutesActive != null) Color(0xFFE0A548)
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(Modifier.weight(0.6f))

            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier.size(300.dp).padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    StationLogo(playback.title ?: "Radio", faviconUrl, size = 300.dp)
                }
            }

            Spacer(Modifier.height(32.dp))

            Text(
                playback.title ?: "Wybierz stację",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (playback.isBuffering) "Buforowanie…" else playback.subtitle.orEmpty(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                EqualizerBars(
                    playback.isPlaying,
                    barColor = MaterialTheme.colorScheme.primary,
                    barWidth = 6.dp,
                    barMaxHeight = 28.dp
                )
            }

            if (sleepMinutesActive != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Wyłączenie za $sleepMinutesActive min",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFE0A548),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.weight(1f))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(52.dp)) {
                    Icon(
                        if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Ulubione",
                        tint = if (isFavorite) Color(0xFFE0A548) else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(26.dp)
                    )
                }

                IconButton(
                    onClick = onPrevious,
                    enabled = canSkip,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        Icons.Filled.SkipPrevious,
                        contentDescription = "Poprzednia stacja",
                        modifier = Modifier.size(32.dp),
                        tint = if (canSkip) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .clickable(onClick = onTogglePlayPause),
                    contentAlignment = Alignment.Center
                ) {
                    if (playback.isBuffering) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    } else {
                        Icon(
                            if (playback.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (playback.isPlaying) "Pauza" else "Odtwórz",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onNext,
                    enabled = canSkip,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = "Następna stacja",
                        modifier = Modifier.size(32.dp),
                        tint = if (canSkip) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }

                IconButton(onClick = onStop, modifier = Modifier.size(52.dp)) {
                    Icon(
                        Icons.Filled.Stop,
                        contentDescription = "Zatrzymaj",
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }

    if (showSleepDialog) {
        SleepTimerDialog(
            active = sleepMinutesActive,
            onDismiss = { showSleepDialog = false },
            onSelect = { minutes ->
                onSetSleepTimer(minutes)
                showSleepDialog = false
            },
            onCancelTimer = {
                onCancelSleepTimer()
                showSleepDialog = false
            }
        )
    }
}

@Composable
private fun SleepTimerDialog(
    active: Int?,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
    onCancelTimer: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Timer snu") },
        text = {
            Column {
                Text(
                    "Radio wyłączy się automatycznie po wybranym czasie.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                listOf(15, 30, 45, 60).forEach { minutes ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(minutes) }
                            .padding(vertical = 10.dp)
                    ) {
                        Text(
                            "$minutes minut",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (active == minutes) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (active != null) {
                TextButton(onClick = onCancelTimer) { Text("Wyłącz timer") }
            } else {
                TextButton(onClick = onDismiss) { Text("Anuluj") }
            }
        },
        dismissButton = {
            if (active != null) {
                TextButton(onClick = onDismiss) { Text("Zamknij") }
            }
        }
    )
}
