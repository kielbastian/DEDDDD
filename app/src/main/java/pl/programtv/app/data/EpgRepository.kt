package pl.programtv.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream

/**
 * Pobiera plik XMLTV z internetu, parsuje go i zapisuje do lokalnej bazy.
 */
class EpgRepository(context: Context) {

    private val prefs = context.getSharedPreferences("epg_prefs", Context.MODE_PRIVATE)
    private val db = EpgDatabase.get(context)
    val dao: EpgDao = db.epgDao()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    var epgUrl: String
        get() = prefs.getString(KEY_URL, DEFAULT_EPG_URL) ?: DEFAULT_EPG_URL
        set(value) {
            prefs.edit().putString(KEY_URL, value.trim()).apply()
        }

    val lastUpdateMillis: Long
        get() = prefs.getLong(KEY_LAST_UPDATE, 0L)

    /** Czy pomijać stacje radiowe i kanały niepolskojęzyczne. Domyślnie tak. */
    var polishOnly: Boolean
        get() = prefs.getBoolean(KEY_POLISH_ONLY, true)
        set(value) {
            prefs.edit().putBoolean(KEY_POLISH_ONLY, value).apply()
        }

    suspend fun hasData(): Boolean = dao.programmeCount() > 0

    /** Pobiera i zapisuje aktualny program TV. Rzuca IOException przy błędzie sieci. */
    suspend fun refresh() = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(epgUrl)
            .header("Accept-Encoding", "gzip")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Serwer EPG odpowiedział kodem ${response.code}")
            }
            val body = response.body ?: throw IOException("Pusta odpowiedź serwera EPG")
            val stream = maybeGunzip(BufferedInputStream(body.byteStream()))
            val result = XmltvParser.parse(stream)
            if (result.channels.isEmpty()) {
                throw IOException("Plik EPG nie zawiera żadnych kanałów")
            }

            var channels = result.channels
            var programmes = result.programmes
            if (polishOnly) {
                channels = channels.filter { ch ->
                    !isRadioChannel(ch.displayName) &&
                        !isKnownForeignChannel(ch.displayName) &&
                        isPolishOrUnknown(result.languageByChannel[ch.id])
                }
                val keptIds = channels.mapTo(HashSet()) { it.id }
                programmes = programmes.filter { it.channelId in keptIds }
                // Zabezpieczenie: gdyby filtr usunął wszystko, zachowaj oryginał.
                if (channels.isEmpty()) {
                    channels = result.channels
                    programmes = result.programmes
                }
            }

            dao.replaceAll(channels, programmes)
            prefs.edit().putLong(KEY_LAST_UPDATE, System.currentTimeMillis()).apply()
        }
    }

    /** Heurystyka: czy nazwa wskazuje na stację radiową. */
    private fun isRadioChannel(name: String): Boolean {
        val n = name.lowercase()
        if (Regex("\\bradio\\b").containsMatchIn(n)) return true
        if (Regex("\\bfm\\b").containsMatchIn(n)) return true
        return RADIO_BRANDS.any { n.contains(it) }
    }

    /** Kanał uznajemy za polski, gdy język jest „pl” albo nieznany (brak oznaczenia). */
    private fun isPolishOrUnknown(language: String?): Boolean {
        if (language.isNullOrBlank()) return true
        val lang = language.lowercase()
        return lang == "pl" || lang.startsWith("pol")
    }

    /**
     * Rozpoznaje po nazwie dobrze znane zagraniczne stacje – wiele darmowych
     * źródeł XMLTV nie oznacza języka w ogóle, więc sam atrybut „lang” nie wystarczy.
     */
    private fun isKnownForeignChannel(name: String): Boolean {
        val n = name.lowercase()
        return FOREIGN_BRANDS.any { n.contains(it) }
    }

    /** Rozpoznaje po nagłówku, czy strumień jest spakowany gzipem. */
    private fun maybeGunzip(input: BufferedInputStream): InputStream {
        input.mark(2)
        val b1 = input.read()
        val b2 = input.read()
        input.reset()
        return if (b1 == 0x1f && b2 == 0x8b) GZIPInputStream(input) else input
    }

    companion object {
        // Darmowe, społecznościowe źródło EPG dla polskich kanałów (format XMLTV).
        // Adres można zmienić w ustawieniach aplikacji.
        const val DEFAULT_EPG_URL = "https://epg.ovh/pl.xml"

        private const val KEY_URL = "epg_url"
        private const val KEY_LAST_UPDATE = "last_update"
        private const val KEY_POLISH_ONLY = "polish_only"

        // Znane marki radiowe (nazwa kanału zawiera którąś z fraz).
        private val RADIO_BRANDS = listOf(
            "rmf", "eska rock", "rock radio", "antyradio", "chillizet",
            "muzo", "tok fm", "vox fm", "radio zet", "radiozet",
            "meloradio", "polskie radio"
        )

        // Dobrze znane zagraniczne stacje, które czasem trafiają do zbiorczych źródeł EPG.
        private val FOREIGN_BRANDS = listOf(
            "cnn", "bbc", "al jazeera", "sky news", "euronews",
            "russia today", "france 24", "deutsche welle", " dw ",
            "rai uno", "rai due", "rai tre", "das erste", "zdf",
            "prosieben", "sat.1", "rtl2", "rtl deutschland", "orf ",
            "nova sport", "markiza", "duna tv", "m1 ", "prima cool",
            "1+1", "inter tv", "ntv ", "rossiya", "channel one russia",
            "cnbc", "bloomberg", "fox news", "abc news", "cbs news"
        )
    }
}
