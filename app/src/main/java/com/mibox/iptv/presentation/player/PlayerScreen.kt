package com.mibox.iptv.presentation.player

import android.view.KeyEvent
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.PlayerView
import com.mibox.iptv.LocalPipController

/**
 * Ekran odtwarzacza sterowany pilotem (D-Pad):
 *  - ↑ / ↓  → Quick Zap (poprzedni / następny kanał)
 *  - OK     → overlay z informacją o kanale + EPG now/next
 *  - Back   → wejście w tryb PiP, ponowny Back kończy odtwarzanie
 */
@Composable
fun PlayerScreen(
    sourceId: Long,
    categoryId: String?,
    startIndex: Int,
    onExit: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
    playerManager: PlayerManager = rememberPlayerManager(),
) {
    val context = LocalContext.current
    val pip = LocalPipController.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nowNext by viewModel.nowNext.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { viewModel.start(sourceId, categoryId, startIndex) }

    // Utworzenie i zwolnienie playera powiązane z cyklem życia kompozycji.
    val activity = remember(context) { context as android.app.Activity }
    DisposableEffect(Unit) {
        playerManager.getOrCreate(activity)
        onDispose { playerManager.release() }
    }

    // Zmiana kanału (start / Quick Zap) → nowy strumień w istniejącym playerze.
    LaunchedEffect(state.channel?.id) {
        state.channel?.let { playerManager.play(it.streamUrl) }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key.nativeKeyCode) {
                    KeyEvent.KEYCODE_DPAD_UP -> { viewModel.zap(+1); true }
                    KeyEvent.KEYCODE_DPAD_DOWN -> { viewModel.zap(-1); true }
                    KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_ENTER -> { viewModel.toggleOverlay(); true }
                    KeyEvent.KEYCODE_BACK -> {
                        // Pierwszy Back → PiP; system zajmie się kolejnym.
                        pip.enterPip(16, 9)
                        true
                    }
                    else -> false
                }
            }
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false            // sterujemy pilotem sami
                    setBackgroundColor(android.graphics.Color.BLACK)
                    player = playerManager.player
                }
            },
            update = { it.player = playerManager.player },
        )

        if (state.overlayVisible) {
            ChannelInfoOverlay(channel = state.channel, nowNext = nowNext)
        }
    }
}
