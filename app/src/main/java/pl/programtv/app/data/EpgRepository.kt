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
            dao.replaceAll(result.channels, result.programmes)
            prefs.edit().putLong(KEY_LAST_UPDATE, System.currentTimeMillis()).apply()
        }
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
    }
}
