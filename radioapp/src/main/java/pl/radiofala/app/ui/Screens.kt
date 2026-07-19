package pl.radiofala.app.ui

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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pl.radiofala.app.data.RadioCategory
import pl.radiofala.app.data.RadioStation
import pl.radiofala.app.RadioViewModel
import pl.radiofala.app.SearchUiState
import pl.radiofala.app.StationsUiState

@Composable
fun CategoryTabs(
    categories: List<RadioCategory>,
    selected: RadioCategory,
    onSelect: (RadioCategory) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories, key = { it.id }) { category ->
            val isSelected = category.id == selected.id
            val accent = Color(category.accent)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) accent else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onSelect(category) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    category.label,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
fun StationListScreen(
    state: StationsUiState,
    favoriteIds: Set<String>,
    currentMediaId: String?,
    isPlaying: Boolean,
    onPlay: (RadioStation) -> Unit,
    onToggleFavorite: (RadioStation) -> Unit,
    padding: PaddingValues
) {
    when {
        state.loading -> Box(
            Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator() }

        state.error != null -> EmptyInfo(padding, Icons.Filled.Radio, state.error)

        state.stations.isEmpty() -> EmptyInfo(
            padding,
            Icons.Filled.Radio,
            "Brak stacji w tej kategorii."
        )

        else -> LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.stations, key = { it.stationUuid }) { station ->
                StationRow(
                    station = station,
                    isFavorite = station.stationUuid in favoriteIds,
                    isCurrent = station.stationUuid == currentMediaId,
                    isPlaying = isPlaying && station.stationUuid == currentMediaId,
                    onPlay = { onPlay(station) },
                    onToggleFavorite = { onToggleFavorite(station) }
                )
            }
        }
    }
}

@Composable
fun StationRow(
    station: RadioStation,
    isFavorite: Boolean,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onPlay),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StationLogo(station.name, station.faviconUrl)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    station.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (station.tags.isNotBlank()) {
                    Text(
                        station.tags,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (isCurrent) {
                EqualizerBars(isPlaying, modifier = Modifier.padding(end = 10.dp))
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (isFavorite) "Usuń z ulubionych" else "Dodaj do ulubionych",
                    tint = if (isFavorite) Color(0xFFE0A548) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun RadioSearchScreen(
    viewModel: RadioViewModel,
    state: SearchUiState,
    favoriteIds: Set<String>,
    currentMediaId: String?,
    isPlaying: Boolean,
    padding: PaddingValues
) {
    Column(Modifier.fillMaxSize().padding(padding)) {
        SearchField(
            value = state.query,
            onValueChange = viewModel::onSearchQueryChange,
            onSearch = viewModel::search
        )
        when {
            state.searching -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.searched && state.results.isEmpty() -> EmptyInfo(
                PaddingValues(0.dp),
                Icons.Filled.SearchOff,
                "Nie znaleziono stacji „${state.query.trim()}”."
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.results, key = { it.stationUuid }) { station ->
                    StationRow(
                        station = station,
                        isFavorite = station.stationUuid in favoriteIds,
                        isCurrent = station.stationUuid == currentMediaId,
                        isPlaying = isPlaying && station.stationUuid == currentMediaId,
                        onPlay = { viewModel.play(station, state.results) },
                        onToggleFavorite = { viewModel.toggleFavorite(station) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit, onSearch: () -> Unit) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        placeholder = { Text("Szukaj stacji po nazwie…") },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = onSearch) {
                Icon(Icons.Filled.Search, contentDescription = "Szukaj")
            }
        },
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { onSearch() }),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            imeAction = androidx.compose.ui.text.input.ImeAction.Search
        )
    )
}

@Composable
fun EmptyInfo(padding: PaddingValues, icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp)),
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
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
