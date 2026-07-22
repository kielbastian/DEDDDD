package com.mibox.iptv.di

import com.mibox.iptv.presentation.player.PlayerManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Udostępnia singleton PlayerManager Composable'om (poza wstrzykiwaniem konstruktorem). */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface PlayerEntryPoint {
    fun playerManager(): PlayerManager
}
