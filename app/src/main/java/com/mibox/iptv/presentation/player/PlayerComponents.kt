package com.mibox.iptv.presentation.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.mibox.iptv.domain.model.Channel
import com.mibox.iptv.domain.model.NowNext
import dagger.hilt.android.EntryPointAccessors
import com.mibox.iptv.di.PlayerEntryPoint

/** Pobiera singleton [PlayerManager] z grafu Hilt bez wstrzykiwania do Composable. */
@Composable
fun rememberPlayerManager(): PlayerManager {
    val context = LocalContext.current
    return remember(context) {
        EntryPointAccessors
            .fromApplication(context.applicationContext, PlayerEntryPoint::class.java)
            .playerManager()
    }
}

/** Nakładka z nazwą kanału i EPG (now/next) — pojawia się po naciśnięciu OK. */
@Composable
fun ChannelInfoOverlay(
    channel: Channel?,
    nowNext: NowNext,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xCC000000))
            .padding(24.dp),
    ) {
        Text(text = channel?.name ?: "—", color = Color.White)
        nowNext.now?.let { Text(text = "Teraz: ${it.title}", color = Color(0xFFDDDDDD)) }
        nowNext.next?.let { Text(text = "Następnie: ${it.title}", color = Color(0xFF999999)) }
    }
}
