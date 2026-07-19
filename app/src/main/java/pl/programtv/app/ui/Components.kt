package pl.programtv.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import pl.programtv.app.data.ProgrammeWithChannel

/** Wspólny pasek wyszukiwania z ikoną lupy i przyciskiem czyszczenia. */
@Composable
fun SearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        placeholder = { Text(placeholder) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "Wyczyść")
                }
            }
        }
    )
}

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

/** Okno ze szczegółami filmu/programu – pokazywane po dotknięciu pozycji. */
@Composable
fun ProgrammeDetailDialog(
    item: ProgrammeWithChannel,
    isReminderSet: Boolean,
    canRemind: Boolean,
    onToggleReminder: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ChannelLogo(item.channelName, item.channelIconUrl, size = 36.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        item.programme.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        item.channelName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (canRemind || isReminderSet) {
                    IconButton(onClick = onToggleReminder) {
                        Icon(
                            if (isReminderSet) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = if (isReminderSet)
                                "Usuń przypomnienie" else "Ustaw przypomnienie",
                            tint = if (isReminderSet) Color(0xFFE0A548)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        text = {
            Column {
                Text(
                    "${formatShortDateTime(item.programme.startMillis)} – " +
                        formatTime(item.programme.stopMillis),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                item.programme.category?.let {
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                if (isReminderSet) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Otrzymasz powiadomienie na 2 minuty przed startem.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFE0A548)
                    )
                } else if (!canRemind) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Przypomnienia dostępne tylko dla nadchodzących pozycji.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    item.programme.description?.takeIf { it.isNotBlank() }
                        ?: "Brak opisu dla tej pozycji.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Zamknij") }
        }
    )
}
