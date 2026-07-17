package pl.programtv.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import pl.programtv.app.AppViewModel
import pl.programtv.app.data.ProgrammeWithChannel

/** Ekran "Teraz w TV" – co aktualnie leci na wybranych kanałach. */
@Composable
fun NowScreen(viewModel: AppViewModel, padding: PaddingValues) {
    val nowPlaying by viewModel.nowPlaying.collectAsState()
    val selectedChannels by viewModel.selectedChannels.collectAsState()

    when {
        selectedChannels.isEmpty() -> EmptyInfo(
            padding,
            Icons.Filled.Tune,
            "Nie wybrano jeszcze żadnych stacji.\n" +
                "Przejdź do zakładki „Kanały” i zaznacz interesujące Cię stacje."
        )
        nowPlaying.isEmpty() -> EmptyInfo(
            padding,
            Icons.Filled.LiveTv,
            "Brak danych o bieżącym programie.\n" +
                "Odśwież program TV przyciskiem w górnym pasku."
        )
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                SectionHeader("Teraz na antenie", "${nowPlaying.size} kanałów")
            }
            items(nowPlaying, key = { it.programme.id }) { item ->
                NowCard(item)
            }
        }
    }
}

@Composable
private fun NowCard(item: ProgrammeWithChannel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ChannelLogo(item.channelName, item.channelIconUrl)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        item.channelName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        item.programme.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                LivePill()
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progressOf(item.programme.startMillis, item.programme.stopMillis) },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "${formatTime(item.programme.startMillis)} – ${formatTime(item.programme.stopMillis)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            item.programme.description?.let { desc ->
                Spacer(Modifier.height(8.dp))
                Text(
                    desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun LivePill() {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            "NA ŻYWO",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

/** Ekran "Program" – ramówka jednego z wybranych kanałów. */
@Composable
fun ScheduleScreen(viewModel: AppViewModel, padding: PaddingValues) {
    val selectedChannels by viewModel.selectedChannels.collectAsState()
    val schedule by viewModel.schedule.collectAsState()
    val currentChannelId by viewModel.scheduleChannelId.collectAsState()

    LaunchedEffect(selectedChannels) {
        if (selectedChannels.none { it.id == currentChannelId }) {
            viewModel.scheduleChannelId.value = selectedChannels.firstOrNull()?.id
        }
    }

    if (selectedChannels.isEmpty()) {
        EmptyInfo(
            padding,
            Icons.Filled.Tune,
            "Nie wybrano jeszcze żadnych stacji.\n" +
                "Przejdź do zakładki „Kanały” i zaznacz interesujące Cię stacje."
        )
        return
    }

    Column(Modifier.fillMaxSize().padding(padding)) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(selectedChannels, key = { it.id }) { channel ->
                FilterChip(
                    selected = channel.id == currentChannelId,
                    onClick = { viewModel.scheduleChannelId.value = channel.id },
                    label = { Text(channel.displayName) },
                    leadingIcon = {
                        ChannelLogo(channel.displayName, channel.iconUrl, size = 22.dp)
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        if (schedule.isEmpty()) {
            EmptyInfo(
                PaddingValues(0.dp),
                Icons.Filled.LiveTv,
                "Brak ramówki dla tego kanału.\nOdśwież program TV przyciskiem w górnym pasku."
            )
        } else {
            val grouped = schedule.groupBy { localDateOf(it.programme.startMillis) }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                grouped.forEach { (day, items) ->
                    item(key = "day-$day") {
                        Text(
                            formatDayHeader(day),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(items, key = { it.programme.id }) { item ->
                        ScheduleRow(item)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleRow(item: ProgrammeWithChannel) {
    val now = System.currentTimeMillis()
    val isNow = item.programme.startMillis <= now && now < item.programme.stopMillis
    val bg = if (isNow) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surface

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                formatTime(item.programme.startMillis),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isNow) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(52.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(
                    item.programme.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isNow) FontWeight.Bold else FontWeight.Normal
                )
                item.programme.category?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isNow) LivePill()
        }
    }
}

/** Ekran wyszukiwarki filmów i programów po tytule. */
@Composable
fun SearchScreen(viewModel: AppViewModel, padding: PaddingValues) {
    val state by viewModel.searchState.collectAsState()

    Column(Modifier.fillMaxSize().padding(padding)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            label = { Text("Tytuł filmu lub programu") },
            placeholder = { Text("np. Wiadomości, Shrek…") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { viewModel.search() }),
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { viewModel.search() }) {
                    Icon(Icons.Filled.Search, contentDescription = "Szukaj")
                }
            }
        )
        Text(
            "Wyszukiwanie obejmuje wszystkie kanały z pobranego programu TV.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(4.dp))

        when {
            state.searching -> Row(
                Modifier.fillMaxWidth().padding(24.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
            state.searched && state.results.isEmpty() -> EmptyInfo(
                PaddingValues(0.dp),
                Icons.Filled.SearchOff,
                "Nie znaleziono „${state.query.trim()}” w nadchodzącym programie TV."
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (state.searched) {
                    item {
                        SectionHeader("Wyniki", "${state.results.size} emisji")
                    }
                }
                items(state.results, key = { it.programme.id }) { item ->
                    SearchResultCard(item)
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(item: ProgrammeWithChannel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            ChannelLogo(item.channelName, item.channelIconUrl)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.programme.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    item.channelName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${formatShortDateTime(item.programme.startMillis)} – " +
                        formatTime(item.programme.stopMillis),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                item.programme.category?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** Ekran wyboru stacji – z opcją „Zaznacz wszystkie”. */
@Composable
fun ChannelsScreen(viewModel: AppViewModel, padding: PaddingValues) {
    val channels by viewModel.channels.collectAsState()
    var filter by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(padding)) {
        OutlinedTextField(
            value = filter,
            onValueChange = { filter = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            label = { Text("Filtruj kanały") },
            placeholder = { Text("np. TVP, Polsat…") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) }
        )

        if (channels.isEmpty()) {
            EmptyInfo(
                PaddingValues(0.dp),
                Icons.Filled.Tune,
                "Lista kanałów jest pusta.\nOdśwież program TV przyciskiem w górnym pasku."
            )
            return
        }

        val visible = channels.filter {
            filter.isBlank() || it.displayName.contains(filter.trim(), ignoreCase = true)
        }

        // Stan „Zaznacz wszystkie” liczony dla aktualnie widocznych kanałów.
        val selectedCount = visible.count { it.selected }
        val allState = when {
            visible.isEmpty() || selectedCount == 0 -> ToggleableState.Off
            selectedCount == visible.size -> ToggleableState.On
            else -> ToggleableState.Indeterminate
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        val target = allState != ToggleableState.On
                        viewModel.setChannelsSelected(visible.map { it.id }, target)
                    }
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TriStateCheckbox(
                    state = allState,
                    onClick = {
                        val target = allState != ToggleableState.On
                        viewModel.setChannelsSelected(visible.map { it.id }, target)
                    }
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        "Zaznacz wszystkie",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "Wybrano $selectedCount z ${visible.size}" +
                            if (filter.isBlank()) "" else " (wg filtra)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(visible, key = { it.id }) { channel ->
                val bg by animateColorAsState(
                    if (channel.selected) MaterialTheme.colorScheme.surfaceVariant
                    else MaterialTheme.colorScheme.surface,
                    label = "channelBg"
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = bg),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setChannelSelected(channel.id, !channel.selected)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ChannelLogo(channel.displayName, channel.iconUrl, size = 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            channel.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (channel.selected) FontWeight.SemiBold
                            else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        Checkbox(
                            checked = channel.selected,
                            onCheckedChange = {
                                viewModel.setChannelSelected(channel.id, it)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, trailing: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Text(
            trailing,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun EmptyInfo(padding: PaddingValues, icon: ImageVector, text: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
