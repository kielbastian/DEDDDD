package pl.radiofala.app.playback

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class PlaybackUiState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentMediaId: String? = null,
    val title: String? = null,
    val subtitle: String? = null
)

/** Łączy warstwę UI z usługą odtwarzania poprzez MediaController (jedna instancja na aplikację). */
class PlayerConnection(private val context: Context) {

    private var controller: MediaController? = null

    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state

    fun connect(onReady: () -> Unit = {}) {
        if (controller != null) {
            onReady()
            return
        }
        val token = SessionToken(context, ComponentName(context, RadioPlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            controller = future.get().also { attachListener(it) }
            onReady()
        }, MoreExecutors.directExecutor())
    }

    private fun attachListener(controller: MediaController) {
        _state.value = _state.value.copy(
            isPlaying = controller.isPlaying,
            isBuffering = controller.playbackState == Player.STATE_BUFFERING
        )
        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.value = _state.value.copy(isPlaying = isPlaying)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _state.value = _state.value.copy(isBuffering = playbackState == Player.STATE_BUFFERING)
            }

            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                _state.value = _state.value.copy(
                    title = mediaMetadata.title?.toString(),
                    subtitle = mediaMetadata.artist?.toString()
                )
            }

            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                _state.value = _state.value.copy(currentMediaId = mediaItem?.mediaId)
            }
        })
    }

    fun play(mediaId: String, title: String, subtitle: String, streamUrl: String, artworkUri: String?) {
        val item = RadioPlaybackService.buildMediaItem(mediaId, title, subtitle, streamUrl, artworkUri)
        controller?.apply {
            setMediaItem(item)
            prepare()
            play()
        }
        _state.value = _state.value.copy(currentMediaId = mediaId, title = title, subtitle = subtitle)
    }

    fun togglePlayPause() {
        controller?.let { c ->
            if (c.isPlaying) c.pause() else c.play()
        }
    }

    fun stop() {
        controller?.stop()
        _state.value = _state.value.copy(currentMediaId = null, isPlaying = false)
    }

    fun setSleepTimer(minutes: Int) {
        val args = Bundle().apply { putInt(RadioPlaybackService.EXTRA_MINUTES, minutes) }
        controller?.sendCustomCommand(SessionCommand(RadioPlaybackService.CMD_SET_SLEEP_TIMER, Bundle.EMPTY), args)
    }

    fun cancelSleepTimer() {
        controller?.sendCustomCommand(
            SessionCommand(RadioPlaybackService.CMD_CANCEL_SLEEP_TIMER, Bundle.EMPTY),
            Bundle.EMPTY
        )
    }

    fun release() {
        controller?.release()
        controller = null
    }
}
