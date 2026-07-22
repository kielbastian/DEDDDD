package com.mibox.iptv.data.parser

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.InputStream

/** Jeden wpis playlisty M3U. */
data class M3uEntry(
    val name: String,
    val tvgId: String?,
    val tvgLogo: String?,
    val groupTitle: String?,
    val url: String,
)

/**
 * Strumieniowy parser M3U/M3U8.
 *
 * Czyta plik linia po linii ([readLine]) i emituje wpisy przez [Flow], zamiast
 * budować listę w pamięci. Repozytorium konsumuje strumień partiami (batch insert
 * do Room), więc zużycie RAM jest stałe niezależnie od rozmiaru playlisty
 * (10k–50k+ kanałów).
 */
class M3uParser {

    fun parse(input: InputStream): Flow<M3uEntry> = flow {
        input.bufferedReader().use { reader ->
            var pendingName: String? = null
            var pendingTvgId: String? = null
            var pendingLogo: String? = null
            var pendingGroup: String? = null

            var raw = reader.readLine()
            while (raw != null) {
                val line = raw.trim()
                when {
                    line.isEmpty() || line.startsWith("#EXTM3U") -> Unit

                    line.startsWith("#EXTINF") -> {
                        pendingTvgId = line.attr("tvg-id")
                        pendingLogo = line.attr("tvg-logo")
                        pendingGroup = line.attr("group-title")
                        // Nazwa kanału to tekst po ostatnim przecinku w linii EXTINF.
                        pendingName = line.substringAfterLast(',', "").trim()
                            .ifEmpty { line.attr("tvg-name") ?: "Unknown" }
                    }

                    line.startsWith("#") -> Unit // inne tagi (#EXTGRP itd.) — pomijamy

                    else -> {
                        // Linia z URL zamyka bieżący wpis.
                        val name = pendingName
                        if (name != null) {
                            emit(
                                M3uEntry(
                                    name = name,
                                    tvgId = pendingTvgId?.takeIf { it.isNotBlank() },
                                    tvgLogo = pendingLogo?.takeIf { it.isNotBlank() },
                                    groupTitle = pendingGroup?.takeIf { it.isNotBlank() },
                                    url = line,
                                )
                            )
                        }
                        pendingName = null
                        pendingTvgId = null
                        pendingLogo = null
                        pendingGroup = null
                    }
                }
                raw = reader.readLine()
            }
        }
    }

    /** Wyciąga wartość atrybutu `key="..."` z linii EXTINF. */
    private fun String.attr(key: String): String? {
        val marker = "$key=\""
        val start = indexOf(marker).takeIf { it >= 0 } ?: return null
        val from = start + marker.length
        val end = indexOf('"', from).takeIf { it >= 0 } ?: return null
        return substring(from, end)
    }
}
