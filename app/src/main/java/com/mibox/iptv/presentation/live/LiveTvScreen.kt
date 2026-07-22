package com.mibox.iptv.presentation.live

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.Text
import com.mibox.iptv.domain.model.Category
import com.mibox.iptv.domain.model.Channel

/**
 * Główny ekran Live TV. Rail kategorii (LazyRow) + lista kanałów (LazyColumn),
 * w całości sterowane D-Padem. Każdy element jest fokusowalny.
 */
@Composable
fun LiveTvScreen(
    onChannelSelected: (sourceId: Long, categoryId: String, index: Int) -> Unit,
    onManageSources: () -> Unit,
    viewModel: LiveViewModel = hiltViewModel(),
) {
    val sourceId by viewModel.sourceId.collectAsStateWithLifecycle()
    val channels by viewModel.channels.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    if (sourceId == 0L && channels.isEmpty()) {
        EmptyState(onManageSources = onManageSources)
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "Live TV", color = Color.White)
            Button(onClick = onManageSources) { Text("Źródła") }
        }
        Spacer(Modifier.height(12.dp))

        CategoryRail(
            categories = categories,
            onSelect = { viewModel.selectCategory(it?.id) },
        )
        Spacer(Modifier.height(12.dp))

        LazyColumn(modifier = Modifier.fillMaxSize().weight(1f)) {
            items(channels, key = { it.id }) { channel ->
                val index = channels.indexOf(channel)
                ChannelRow(
                    channel = channel,
                    onClick = {
                        onChannelSelected(sourceId, channel.categoryId ?: "all", index)
                    },
                )
            }
        }
    }
}

@Composable
private fun CategoryRail(categories: List<Category>, onSelect: (Category?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Card(onClick = { onSelect(null) }) {
                Text(text = "Wszystkie", modifier = Modifier.padding(12.dp))
            }
        }
        items(categories, key = { it.id }) { category ->
            Card(onClick = { onSelect(category) }) {
                Text(text = category.name, modifier = Modifier.padding(12.dp))
            }
        }
    }
}

@Composable
private fun ChannelRow(channel: Channel, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.padding(vertical = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = channel.name)
        }
    }
}

@Composable
private fun EmptyState(onManageSources: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Brak skonfigurowanego źródła", color = Color.White)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onManageSources) { Text("Dodaj playlistę M3U / Xtream") }
        }
    }
}
