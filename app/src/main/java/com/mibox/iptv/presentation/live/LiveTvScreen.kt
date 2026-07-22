package com.mibox.iptv.presentation.live

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Card
import androidx.tv.material3.Text
import com.mibox.iptv.domain.model.Channel

/**
 * Główny ekran Live TV. Lista kanałów sterowana D-Padem (fokus pionowy).
 * Każdy [Card] jest fokusowalny — wyraźny stan fokusa czytelny z kanapy.
 */
@Composable
fun LiveTvScreen(
    onChannelSelected: (sourceId: Long, categoryId: String, index: Int) -> Unit,
    viewModel: LiveViewModel = hiltViewModel(),
) {
    val sourceId by viewModel.sourceId.collectAsStateWithLifecycle()
    val channels by viewModel.channels.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
    ) {
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
