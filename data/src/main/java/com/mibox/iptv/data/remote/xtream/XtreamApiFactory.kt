package com.mibox.iptv.data.remote.xtream

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Buduje [XtreamApi] dla konkretnego hosta użytkownika.
 *
 * Base URL Xtreama zależy od źródła (host:port), więc Retrofit tworzymy per źródło,
 * współdzieląc jeden [OkHttpClient] (pool połączeń, timeouty).
 */
@Singleton
class XtreamApiFactory @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
) {
    fun create(host: String): XtreamApi {
        val base = host.trimEnd('/') + "/"
        return Retrofit.Builder()
            .baseUrl(base)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(XtreamApi::class.java)
    }

    companion object {
        /** URL strumienia na żywo Xtreama. */
        fun liveStreamUrl(host: String, user: String, pass: String, streamId: Int): String =
            "${host.trimEnd('/')}/live/$user/$pass/$streamId.ts"

        /** URL pliku VOD. */
        fun vodStreamUrl(
            host: String, user: String, pass: String, streamId: Int, ext: String,
        ): String = "${host.trimEnd('/')}/movie/$user/$pass/$streamId.$ext"
    }
}
