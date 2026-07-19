package pl.radiofala.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Klient publicznej, otwartej bazy stacji radiowych Radio-Browser
 * (https://www.radio-browser.info) – bez klucza API, community-driven,
 * używanej przez wiele niezależnych aplikacji radiowych.
 */
class RadioBrowserApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    // "all.api..." to nazwa DNS rozwiązywana na dowolny działający serwer lustrzany.
    private val baseUrl = "https://all.api.radio-browser.info/json"

    suspend fun byTag(tag: String, limit: Int = 100): List<RadioStation> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/stations/bytagexact/${encode(tag)}" +
            "?limit=$limit&hidebroken=true&order=clickcount&reverse=true"
        fetchStations(url)
    }

    suspend fun byCountry(countryCode: String, limit: Int = 100): List<RadioStation> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/stations/bycountrycodeexact/${encode(countryCode)}" +
            "?limit=$limit&hidebroken=true&order=clickcount&reverse=true"
        fetchStations(url)
    }

    suspend fun searchByName(query: String, limit: Int = 50): List<RadioStation> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/stations/search?name=${encode(query)}" +
            "&limit=$limit&hidebroken=true&order=clickcount&reverse=true"
        fetchStations(url)
    }

    /**
     * Rozwiązuje najświeższy adres strumienia i zgłasza odsłuchanie stacji
     * (zgodnie z zalecaną etykietą klientów Radio-Browser). Zwraca URL do odtworzenia.
     */
    suspend fun resolveStreamUrl(station: RadioStation): String = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/url/${station.stationUuid}")
                .header("User-Agent", USER_AGENT)
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext station.streamUrl
                val body = response.body?.string() ?: return@withContext station.streamUrl
                val resolved = org.json.JSONObject(body).optString("url")
                resolved.ifBlank { station.streamUrl }
            }
        } catch (_: Exception) {
            station.streamUrl
        }
    }

    private fun fetchStations(url: String): List<RadioStation> {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Radio-Browser odpowiedział kodem ${response.code}")
            }
            val body = response.body?.string() ?: throw IOException("Pusta odpowiedź")
            return parseStations(body)
        }
    }

    private fun parseStations(json: String): List<RadioStation> {
        val array = JSONArray(json)
        val result = mutableListOf<RadioStation>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val streamUrl = obj.optString("url_resolved").ifBlank { obj.optString("url") }
            val name = obj.optString("name").trim()
            if (streamUrl.isBlank() || name.isBlank()) continue
            result.add(
                RadioStation(
                    stationUuid = obj.optString("stationuuid"),
                    name = name,
                    streamUrl = streamUrl,
                    faviconUrl = obj.optString("favicon").takeIf { it.isNotBlank() },
                    tags = obj.optString("tags"),
                    countryCode = obj.optString("countrycode").takeIf { it.isNotBlank() },
                    bitrateKbps = obj.optInt("bitrate")
                )
            )
        }
        return result.distinctBy { it.stationUuid.ifBlank { it.streamUrl } }
    }

    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8")

    companion object {
        // Radio-Browser prosi klientów o identyfikację w nagłówku User-Agent.
        private const val USER_AGENT = "RadioFala-Android/1.0"
    }
}
