package com.mibox.iptv.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Kontrolki działające i dotykiem (telefon), i D-Padem (TV).
 *
 * `androidx.tv:tv-material` Button/Card są zaprojektowane wyłącznie pod nawigację
 * pilotem i na ekranie dotykowym często nie reagują na stuknięcie. Te wersje
 * opierają się na `Modifier.clickable`, który obsługuje oba typy wejścia, a stan
 * fokusa (z pilota) zaznaczamy kolorem tła.
 */

private val Accent = Color(0xFF3DA9FC)
private val SurfaceColor = Color(0xFF1B2530)
private val CardColor = Color(0xFF1B2530)
private val CardFocused = Color(0xFF264056)

@Composable
fun AppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (focused) Accent else SurfaceColor)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
fun AppCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (focused) CardFocused else CardColor)
            .clickable(interactionSource = interaction, indication = null) { onClick() },
        content = content,
    )
}
