package com.mibox.iptv

import android.app.PictureInPictureParams
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.darkColorScheme
import com.mibox.iptv.presentation.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint

/** Umożliwia ekranom wywołanie trybu PiP bez trzymania referencji do Activity. */
val LocalPipController = staticCompositionLocalOf<PipController> {
    error("PipController not provided")
}

interface PipController {
    fun enterPip(aspectWidth: Int, aspectHeight: Int)
}

@AndroidEntryPoint
class MainActivity : ComponentActivity(), PipController {

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CompositionLocalProvider(LocalPipController provides this) {
                        AppNavHost()
                    }
                }
            }
        }
    }

    override fun enterPip(aspectWidth: Int, aspectHeight: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && packageManager
                .hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE)
        ) {
            enterPipInternal(aspectWidth, aspectHeight)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun enterPipInternal(aspectWidth: Int, aspectHeight: Int) {
        val ratio = Rational(aspectWidth.coerceAtLeast(1), aspectHeight.coerceAtLeast(1))
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(ratio)
            .build()
        enterPictureInPictureMode(params)
    }
}
