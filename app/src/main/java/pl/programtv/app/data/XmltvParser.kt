package pl.programtv.app.data

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Parser formatu XMLTV (http://wiki.xmltv.org) – standardu, w którym
 * publikowana jest większość darmowych źródeł programu TV (EPG).
 */
object XmltvParser {

    data class Result(
        val channels: List<ChannelEntity>,
        val programmes: List<ProgrammeEntity>
    )

    // Format daty XMLTV: 20260717203000 +0200 (strefa bywa pomijana).
    private val dateFormat = DateTimeFormatter.ofPattern("yyyyMMddHHmmss Z")
    private val dateFormatNoZone = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

    fun parse(input: InputStream): Result {
        val channels = mutableListOf<ChannelEntity>()
        val programmes = mutableListOf<ProgrammeEntity>()

        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(input, null)

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "channel" -> parseChannel(parser)?.let { channels.add(it) }
                    "programme" -> parseProgramme(parser)?.let { programmes.add(it) }
                }
            }
            event = parser.next()
        }
        return Result(channels, programmes)
    }

    private fun parseChannel(parser: XmlPullParser): ChannelEntity? {
        val id = parser.getAttributeValue(null, "id") ?: return null
        var name: String? = null
        var icon: String? = null

        var event = parser.next()
        while (!(event == XmlPullParser.END_TAG && parser.name == "channel")) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "display-name" -> if (name == null) name = readText(parser)
                    "icon" -> icon = parser.getAttributeValue(null, "src")
                }
            }
            event = parser.next()
        }
        return ChannelEntity(id = id, displayName = name ?: id, iconUrl = icon)
    }

    private fun parseProgramme(parser: XmlPullParser): ProgrammeEntity? {
        val channelId = parser.getAttributeValue(null, "channel") ?: return null
        val start = parseDate(parser.getAttributeValue(null, "start")) ?: return null
        val stop = parseDate(parser.getAttributeValue(null, "stop"))

        var title: String? = null
        var desc: String? = null
        var category: String? = null

        var event = parser.next()
        while (!(event == XmlPullParser.END_TAG && parser.name == "programme")) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "title" -> if (title == null) title = readText(parser)
                    "desc" -> if (desc == null) desc = readText(parser)
                    "category" -> if (category == null) category = readText(parser)
                }
            }
            event = parser.next()
        }

        val t = title?.trim().orEmpty()
        if (t.isEmpty()) return null

        return ProgrammeEntity(
            channelId = channelId,
            title = t,
            description = desc?.trim()?.takeIf { it.isNotEmpty() },
            category = category?.trim()?.takeIf { it.isNotEmpty() },
            startMillis = start,
            // Brak "stop" zdarza się w niektórych źródłach – przyjmij 2 h.
            stopMillis = stop ?: (start + 2 * 60 * 60 * 1000)
        )
    }

    private fun readText(parser: XmlPullParser): String {
        var text = ""
        if (parser.next() == XmlPullParser.TEXT) {
            text = parser.text ?: ""
            parser.nextTag()
        }
        return text
    }

    internal fun parseDate(raw: String?): Long? {
        val value = raw?.trim() ?: return null
        return try {
            OffsetDateTime.parse(value, dateFormat).toInstant().toEpochMilli()
        } catch (_: Exception) {
            try {
                java.time.LocalDateTime.parse(value.take(14), dateFormatNoZone)
                    .toInstant(ZoneOffset.UTC).toEpochMilli()
            } catch (_: Exception) {
                null
            }
        }
    }
}
