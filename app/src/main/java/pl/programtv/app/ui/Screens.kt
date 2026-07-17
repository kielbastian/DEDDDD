package pl.programtv.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
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
            "Nie wybrano jeszcze żadnych stacji.\n" +
                "Przejdź do zakładki „Kanały” i zaznacz interesujące Cię stacje."
        )
        nowPlaying.isEmpty() -> EmptyInfo(
            padding,
            "Brak danych o bieżącym programie.\n" +
                "Odśwież program TV przyciskiem w górnym pasku."
        )
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(nowPlaying, key = { it.programme.id }) { item ->
                NowCard(item)
            }
        }
    }
}

@Composable
private fun NowCard(item: ProgrammeWithChannel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(
                item.channelName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                item.programme.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "${formatTime(item.programme.startMillis)} – ${formatTime(item.programme.stopMillis)}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progressOf(item.programme.startMillis, item.programme.stopMillis) },
                modifier = Modifier.fillMaxWidth()
            )
            item.programme.description?.let { desc ->
                Spacer(Modifier.height(8.dp))
                Text(
                    desc,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Ekran "Program" – ramówka jednego z wybranych kanałów. */
@Composable
fun ScheduleScreen(viewModel: AppViewModel, padding: PaddingValues) {
    val selectedChannels by viewModel.selectedChannels.collectAsState()
    val schedule by viewModel.schedule.collectAsState()
    val currentChannelId by viewModel.scheduleChannelId.collectAsState()

    // Pilnuj, żeby wybrany kanał zawsze był jednym z zaznaczonych.
    LaunchedEffect(selectedChannels) {
        if (selectedChannels.none { it.id == currentChannelId }) {
            viewModel.scheduleChannelId.value = selectedChannels.firstOrNull()?.id
        }
    }

    if (selectedChannels.isEmpty()) {
        EmptyInfo(
            padding,
            "Nie wybrano jeszcze żadnych stacji.\n" +
                "Przejdź do zakładki „Kanały” i zaznacz interesujące Cię stacje."
        )
        return
    }

    Column(Modifier.fillMaxSize().padding(padding)) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(selectedChannels, key = { it.id }) { channel ->
                FilterChip(
                    selected = channel.id == currentChannelId,
                    onClick = { viewModel.scheduleChannelId.value = channel.id },
                    label = { Text(channel.displayName) }
                )
            }
        }
        HorizontalDivider()

        if (schedule.isEmpty()) {
            EmptyInfo(
                PaddingValues(0.dp),
                "Brak ramówki dla tego kanału.\nOdśwież program TV przyciskiem w górnym pasku."
            )
        } else {
            val grouped = schedule.groupBy { localDateOf(it.programme.startMillis) }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                grouped.forEach { (day, items) ->
                    item(key = "day-$day") {
                        Text(
                            formatDayHeader(day),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
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
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                formatTime(item.programme.startMillis),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isNow) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(56.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(
                    item.programme.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isNow) FontWeight.SemiBold else FontWeight.Normal
                )
                item.programme.category?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isNow) {
                Text(
                    "TERAZ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
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
                .padding(horizontal = 12.dp, vertical = 8.dp),
            label = { Text("Tytuł filmu lub programu") },
            placeholder = { Text("np. Wiadomości, Shrek…") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { viewModel.search() }),
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
                "Nie znaleziono „${state.query.trim()}” w nadchodzącym programie TV."
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.results, key = { it.programme.id }) { item ->
                    SearchResultCard(item)
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(item: ProgrammeWithChannel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(
                item.programme.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                item.channelName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "${formatShortDateTime(item.programme.startMillis)} – " +
                    formatTime(item.programme.stopMillis),
                style = MaterialTheme.typography.bodyMedium
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

/** Ekran wyboru stacji. */
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
                .padding(horizontal = 12.dp, vertical = 8.dp),
            label = { Text("Filtruj kanały") },
            placeholder = { Text("np. TVP, Polsat…") },
            singleLine = true
        )

        if (channels.isEmpty()) {
            EmptyInfo(
                PaddingValues(0.dp),
                "Lista kanałów jest pusta.\nOdśwież program TV przyciskiem w górnym pasku."
            )
            return
        }

        val visible = channels.filter {
            filter.isBlank() || it.displayName.contains(filter.trim(), ignoreCase = true)
        }

        LazyColumn(Modifier.fillMaxSize()) {
            items(visible, key = { it.id }) { channel ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = channel.selected,
                        onCheckedChange = {
                            viewModel.setChannelSelected(channel.id, it)
                        }
                    )
                    Text(channel.displayName, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
fun EmptyInfo(padding: PaddingValues, text: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
