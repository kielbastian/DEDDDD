package com.mibox.iptv.domain.model

/** Źródło playlisty konfigurowane przez użytkownika. */
sealed interface PlaylistSource {
    val id: Long
    val name: String

    data class M3u(
        override val id: Long,
        override val name: String,
        val url: String,
        val epgUrl: String? = null,
    ) : PlaylistSource

    data class Xtream(
        override val id: Long,
        override val name: String,
        val host: String,      // np. http://serwer:8080
        val username: String,
        val password: String,
    ) : PlaylistSource
}

data class Category(
    val id: String,
    val sourceId: Long,
    val name: String,
    val kind: ContentKind,
)

enum class ContentKind { LIVE, VOD, SERIES }

data class Channel(
    val id: Long,
    val sourceId: Long,
    val categoryId: String?,
    val name: String,
    val tvgId: String?,        // klucz łączący z EPG (XMLTV channel id)
    val logoUrl: String?,
    val streamUrl: String,
    val sortOrder: Int,
    val kind: ContentKind = ContentKind.LIVE,
)

data class EpgProgram(
    val id: Long,
    val channelTvgId: String,
    val startMillis: Long,
    val endMillis: Long,
    val title: String,
    val description: String?,
) {
    fun progress(nowMillis: Long): Float {
        val span = (endMillis - startMillis).coerceAtLeast(1)
        return ((nowMillis - startMillis).toFloat() / span).coerceIn(0f, 1f)
    }
}

/** Aktualny + następny program dla kanału. */
data class NowNext(
    val now: EpgProgram?,
    val next: EpgProgram?,
)
