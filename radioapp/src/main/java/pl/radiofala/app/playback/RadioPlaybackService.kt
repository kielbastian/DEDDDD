package pl.radiofala.app.playback

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import pl.radiofala.app.R

/**
 * Usługa pierwszoplanowa: odtwarza radio w tle i przy zgaszonym ekranie,
 * pokazuje powiadomienie z sterowaniem na ekranie blokady (MediaSession).
 */
class RadioPlaybackService : MediaSessionService() {

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private val handler = Handler(Looper.getMainLooper())
    private var sleepTimerRunnable: Runnable? = null

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(SleepTimerCallback())
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = mediaSession

    override fun onDestroy() {
        clearSleepTimer()
        mediaSession.run {
            player.release()
            release()
        }
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        // Użytkownik zamknął aplikację z listy – jeśli nic nie gra, zatrzymaj usługę.
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    private fun scheduleSleepTimer(minutes: Int) {
        clearSleepTimer()
        val runnable = Runnable {
            player.pause()
            sleepTimerRunnable = null
        }
        sleepTimerRunnable = runnable
        handler.postDelayed(runnable, minutes * 60_000L)
    }

    private fun clearSleepTimer() {
        sleepTimerRunnable?.let { handler.removeCallbacks(it) }
        sleepTimerRunnable = null
    }

    // Radio na żywo to zawsze jeden MediaItem – ExoPlayer nigdy nie zgłasza dostępnych
    // komend seekToNext/seekToPrevious. Dlatego "poprzednia/następna stacja" na ekranie
    // blokady są realizowane jako własne komendy sesji, pokazane przez custom layout.
    private val previousNextLayout: List<CommandButton> by lazy {
        listOf(
            CommandButton.Builder()
                .setDisplayName("Poprzednia stacja")
                .setSessionCommand(SessionCommand(CMD_PREVIOUS_STATION, Bundle.EMPTY))
                .setIconResId(R.drawable.ic_notif_skip_previous)
                .build(),
            CommandButton.Builder()
                .setDisplayName("Następna stacja")
                .setSessionCommand(SessionCommand(CMD_NEXT_STATION, Bundle.EMPTY))
                .setIconResId(R.drawable.ic_notif_skip_next)
                .build()
        )
    }

    private inner class SleepTimerCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val availableCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                .buildUpon()
                .add(SessionCommand(CMD_SET_SLEEP_TIMER, Bundle.EMPTY))
                .add(SessionCommand(CMD_CANCEL_SLEEP_TIMER, Bundle.EMPTY))
                .add(SessionCommand(CMD_PREVIOUS_STATION, Bundle.EMPTY))
                .add(SessionCommand(CMD_NEXT_STATION, Bundle.EMPTY))
                .build()
            return MediaSession.ConnectionResult.accept(
                availableCommands,
                MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS
            )
        }

        override fun onPostConnect(session: MediaSession, controller: MediaSession.ControllerInfo) {
            session.setCustomLayout(controller, previousNextLayout)
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                CMD_SET_SLEEP_TIMER -> {
                    val minutes = args.getInt(EXTRA_MINUTES, 0)
                    if (minutes > 0) scheduleSleepTimer(minutes)
                }
                CMD_CANCEL_SLEEP_TIMER -> clearSleepTimer()
                CMD_PREVIOUS_STATION -> queueNavigator?.onPreviousRequested()
                CMD_NEXT_STATION -> queueNavigator?.onNextRequested()
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    /** Most do warstwy ViewModel – tam żyje kolejka stacji, usługa tylko przekazuje żądanie. */
    interface QueueNavigator {
        fun onPreviousRequested()
        fun onNextRequested()
    }

    companion object {
        const val CMD_SET_SLEEP_TIMER = "pl.radiofala.app.SET_SLEEP_TIMER"
        const val CMD_CANCEL_SLEEP_TIMER = "pl.radiofala.app.CANCEL_SLEEP_TIMER"
        const val CMD_PREVIOUS_STATION = "pl.radiofala.app.PREVIOUS_STATION"
        const val CMD_NEXT_STATION = "pl.radiofala.app.NEXT_STATION"
        const val EXTRA_MINUTES = "minutes"

        @Volatile
        var queueNavigator: QueueNavigator? = null

        fun buildMediaItem(
            mediaId: String,
            title: String,
            subtitle: String,
            streamUrl: String,
            artworkUri: String?
        ): MediaItem {
            val metadata = androidx.media3.common.MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(subtitle)
                .setAlbumTitle("Marek Kulczycki")
                .apply {
                    if (!artworkUri.isNullOrBlank()) {
                        setArtworkUri(android.net.Uri.parse(artworkUri))
                    }
                }
                .build()
            return MediaItem.Builder()
                .setMediaId(mediaId)
                .setUri(streamUrl)
                .setMediaMetadata(metadata)
                .build()
        }
    }
}
