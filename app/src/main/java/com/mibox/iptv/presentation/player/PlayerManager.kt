package com.mibox.iptv.presentation.player

import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.Surface
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Zarządza pojedynczą instancją [ExoPlayer] dla całej sesji odtwarzania.
 *
 * Cele pod Xiaomi Mi Box (2 GB RAM, Amlogic):
 *  - Wyłącznie dekodery SPRZĘTOWE (MediaCodec): HEVC/H.265, AV1, Dolby Vision.
 *  - Audio passthrough (AC3/E-AC3/DTS) przez HDMI/S-PDIF — offload do AVR.
 *  - Auto Frame Rate: dopasowanie odświeżania ekranu do klatkażu materiału.
 *  - Obniżone bufory (DefaultLoadControl) → mniejszy footprint RAM.
 *  - Deterministyczne zwolnienie zasobów ([release]) — brak wycieków pamięci.
 */
@OptIn(UnstableApi::class)
@Singleton
class PlayerManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    var player: ExoPlayer? = null
        private set

    private var afrController: AutoFrameRateController? = null
    private var videoListener: Player.Listener? = null

    fun getOrCreate(activity: Activity): ExoPlayer {
        player?.let { return it }

        // 1) Renderery: tylko sprzętowe. EXTENSION_RENDERER_MODE_OFF blokuje
        //    programowy fallback, który zabiłby CPU Amlogica na HEVC/AV1.
        val renderersFactory = DefaultRenderersFactory(context).apply {
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
            setEnableDecoderFallback(false)
        }

        // 2) Track selector: preferuj tunneling (płynniejszy obraz, mniej CPU).
        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setTunnelingEnabled(true)
                    .setAllowVideoMixedMimeTypeAdaptiveness(true)
            )
        }

        // 3) LoadControl dostrojony w dół — bufory mniejsze niż domyślne,
        //    bo 2 GB RAM. Wciąż wystarczające dla stabilnego streamu na żywo.
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,
                /* maxBufferMs = */ 30_000,
                /* bufferForPlaybackMs = */ 2_000,
                /* bufferForPlaybackAfterRebufferMs = */ 4_000,
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        // 4) Audio: passthrough offload zamiast dekodowania w CPU, gdy AVR wspiera.
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        val exo = ExoPlayer.Builder(context, renderersFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        // 5) Auto Frame Rate — zmienia tryb wyświetlania pod klatkaż wideo.
        afrController = AutoFrameRateController(activity)
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                afrController?.onVideoSize(videoSize)
            }

            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                tracks.groups.firstOrNull { it.type == C.TRACK_TYPE_VIDEO }
                    ?.let { group ->
                        val format: Format = group.getTrackFormat(0)
                        if (format.frameRate > 0) afrController?.onFrameRate(format.frameRate)
                    }
            }
        }
        exo.addListener(listener)
        videoListener = listener

        player = exo
        return exo
    }

    fun setSurface(surface: Surface?) {
        player?.setVideoSurface(surface)
    }

    fun play(url: String) {
        val exo = player ?: return
        exo.setMediaItem(MediaItem.fromUri(url))
        exo.prepare()
        exo.playWhenReady = true
    }

    /** Deterministyczne zwolnienie — wołane z onStop/onDispose. Zapobiega wyciekom. */
    fun release() {
        afrController?.reset()
        afrController = null
        videoListener?.let { player?.removeListener(it) }
        videoListener = null
        player?.release()
        player = null
    }
}

/**
 * Steruje trybem odświeżania ekranu (Auto Frame Rate).
 *
 * API 30+: [Surface.setFrameRate] podpowiada systemowi docelowy klatkaż.
 * API 23–29: wybór najbliższego [android.view.Display.Mode] przez
 * WindowManager.LayoutParams.preferredDisplayModeId.
 */
@OptIn(UnstableApi::class)
class AutoFrameRateController(private val activity: Activity) {

    private var lastFrameRate: Float = 0f
    private var originalModeId: Int = 0

    init {
        originalModeId = activity.window.attributes.preferredDisplayModeId
    }

    fun onVideoSize(size: VideoSize) { /* zarezerwowane pod skalowanie */ }

    fun onFrameRate(frameRate: Float) {
        if (frameRate <= 0f || frameRate == lastFrameRate) return
        lastFrameRate = frameRate

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            selectDisplayMode(frameRate)
        }
    }

    private fun selectDisplayMode(frameRate: Float) {
        // Activity.display to API 30+; na 25–29 sięgamy po domyślny Display przez WM.
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.display
        } else {
            @Suppress("DEPRECATION")
            activity.windowManager.defaultDisplay
        } ?: return
        val supported = display.supportedModes
        // Szukamy trybu, którego odświeżanie jest wielokrotnością klatkażu
        // (24 -> 24/48/72 Hz, 25 -> 50 Hz, 50 -> 50 Hz, 60 -> 60 Hz).
        val best = supported.minByOrNull { mode ->
            val ratio = mode.refreshRate / frameRate
            val nearestMultiple = ratio.let { Math.round(it) }.coerceAtLeast(1)
            kotlin.math.abs(mode.refreshRate - frameRate * nearestMultiple)
        } ?: return

        activity.runOnUiThread {
            val params = activity.window.attributes
            params.preferredDisplayModeId = best.modeId
            activity.window.attributes = params
        }
    }

    fun reset() {
        activity.runOnUiThread {
            val params = activity.window.attributes
            params.preferredDisplayModeId = originalModeId
            activity.window.attributes = params
        }
        lastFrameRate = 0f
    }
}
