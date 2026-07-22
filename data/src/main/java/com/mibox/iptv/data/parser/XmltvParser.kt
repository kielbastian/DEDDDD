package com.mibox.iptv.data.parser

import android.util.Xml
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.zip.GZIPInputStream

/** Program EPG sparsowany z XMLTV. */
data class XmltvProgramme(
    val channelId: String,
    val startMillis: Long,
    val endMillis: Long,
    val title: String,
    val description: String?,
)

/**
 * Strumieniowy parser XMLTV oparty na [XmlPullParser] (pull, nie DOM).
 *
 * Wielkie EPG (dziesiątki MB) NIE są ładowane do drzewa DOM — czytamy zdarzenia
 * sekwencyjnie i emitujemy [XmltvProgramme] jeden po drugim. Repozytorium zapisuje
 * je partiami do Room. Dzięki temu parsowanie wielkiego EPG mieści się w budżecie
 * RAM Mi Boxa. Obsługiwane jest wejście spakowane gzipem (.xml.gz).
 */
class XmltvParser {

    // Format XMLTV: "20260722183000 +0200"
    private val dateFormat = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US)

    fun parse(rawInput: InputStream, gzip: Boolean): Flow<XmltvProgramme> = flow {
        val stream = if (gzip) GZIPInputStream(rawInput) else rawInput
        stream.use { input ->
            val parser = Xml.newPullParser().apply {
                setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                setInput(input, null)
            }

            var channelId: String? = null
            var start = 0L
            var end = 0L
            var title: String? = null
            var description: String? = null
            var currentTag: String? = null

            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> when (parser.name) {
                        "programme" -> {
                            channelId = parser.getAttributeValue(null, "channel")
                            start = parseTime(parser.getAttributeValue(null, "start"))
                            end = parseTime(parser.getAttributeValue(null, "stop"))
                            title = null
                            description = null
                        }
                        "title", "desc" -> currentTag = parser.name
                    }

                    XmlPullParser.TEXT -> when (currentTag) {
                        "title" -> title = parser.text?.trim()
                        "desc" -> description = parser.text?.trim()
                    }

                    XmlPullParser.END_TAG -> when (parser.name) {
                        "title", "desc" -> currentTag = null
                        "programme" -> {
                            val id = channelId
                            if (id != null && end > start) {
                                emit(
                                    XmltvProgramme(
                                        channelId = id,
                                        startMillis = start,
                                        endMillis = end,
                                        title = title ?: "",
                                        description = description,
                                    )
                                )
                            }
                        }
                    }
                }
                event = parser.next()
            }
        }
    }

    private fun parseTime(value: String?): Long {
        if (value.isNullOrBlank()) return 0L
        return try {
            // XMLTV bywa bez spacji przed strefą; normalizujemy do formatu z separatorem.
            val normalized = if (value.length > 14 && value[14] != ' ') {
                value.substring(0, 14) + " " + value.substring(14)
            } else value
            dateFormat.parse(normalized)?.time ?: 0L
        } catch (_: Exception) {
            0L
        }
    }
}
