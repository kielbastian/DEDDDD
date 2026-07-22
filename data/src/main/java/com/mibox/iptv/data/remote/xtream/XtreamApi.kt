package com.mibox.iptv.data.remote.xtream

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Xtream Codes player API.
 *
 * Bazowy URL: http(s)://host:port/  (a metody dopisują player_api.php + akcje).
 * Strumień na żywo: {host}/live/{user}/{pass}/{stream_id}.{ext}
 */
interface XtreamApi {

    @GET("player_api.php")
    suspend fun getLiveCategories(
        @Query("username") user: String,
        @Query("password") pass: String,
        @Query("action") action: String = "get_live_categories",
    ): List<XtreamCategoryDto>

    @GET("player_api.php")
    suspend fun getLiveStreams(
        @Query("username") user: String,
        @Query("password") pass: String,
        @Query("action") action: String = "get_live_streams",
    ): List<XtreamLiveStreamDto>

    @GET("player_api.php")
    suspend fun getVodCategories(
        @Query("username") user: String,
        @Query("password") pass: String,
        @Query("action") action: String = "get_vod_categories",
    ): List<XtreamCategoryDto>

    @GET("player_api.php")
    suspend fun getVodStreams(
        @Query("username") user: String,
        @Query("password") pass: String,
        @Query("action") action: String = "get_vod_streams",
    ): List<XtreamVodStreamDto>

    @GET("player_api.php")
    suspend fun getShortEpg(
        @Query("username") user: String,
        @Query("password") pass: String,
        @Query("stream_id") streamId: Int,
        @Query("action") action: String = "get_short_epg",
    ): XtreamEpgListingDto
}

@Serializable
data class XtreamCategoryDto(
    @SerialName("category_id") val categoryId: String,
    @SerialName("category_name") val categoryName: String,
)

@Serializable
data class XtreamLiveStreamDto(
    @SerialName("stream_id") val streamId: Int,
    @SerialName("name") val name: String,
    @SerialName("stream_icon") val icon: String? = null,
    @SerialName("epg_channel_id") val epgChannelId: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
)

@Serializable
data class XtreamVodStreamDto(
    @SerialName("stream_id") val streamId: Int,
    @SerialName("name") val name: String,
    @SerialName("stream_icon") val icon: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("container_extension") val containerExtension: String? = "mp4",
)

@Serializable
data class XtreamEpgListingDto(
    @SerialName("epg_listings") val listings: List<XtreamEpgItemDto> = emptyList(),
)

@Serializable
data class XtreamEpgItemDto(
    @SerialName("title") val titleBase64: String? = null,
    @SerialName("description") val descriptionBase64: String? = null,
    @SerialName("start_timestamp") val startTs: Long = 0,
    @SerialName("stop_timestamp") val stopTs: Long = 0,
)
