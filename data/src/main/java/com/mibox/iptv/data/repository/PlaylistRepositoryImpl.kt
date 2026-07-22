package com.mibox.iptv.data.repository

import com.mibox.iptv.core.DispatcherProvider
import com.mibox.iptv.data.local.dao.CategoryDao
import com.mibox.iptv.data.local.dao.ChannelDao
import com.mibox.iptv.data.local.dao.SourceDao
import com.mibox.iptv.data.local.entity.ChannelEntity
import com.mibox.iptv.data.mapper.toDomain
import com.mibox.iptv.data.mapper.toEntity
import com.mibox.iptv.data.parser.M3uEntry
import com.mibox.iptv.data.parser.M3uParser
import com.mibox.iptv.domain.model.Category
import com.mibox.iptv.domain.model.Channel
import com.mibox.iptv.domain.model.ContentKind
import com.mibox.iptv.domain.model.PlaylistSource
import com.mibox.iptv.domain.repository.PlaylistRepository
import com.mibox.iptv.domain.repository.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

class PlaylistRepositoryImpl @Inject constructor(
    private val sourceDao: SourceDao,
    private val categoryDao: CategoryDao,
    private val channelDao: ChannelDao,
    private val m3uParser: M3uParser,
    private val httpClient: OkHttpClient,
    private val dispatchers: DispatcherProvider,
) : PlaylistRepository {

    override fun sources(): Flow<List<PlaylistSource>> =
        sourceDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun addSource(source: PlaylistSource): Long =
        sourceDao.insert(source.toEntity())

    override suspend fun removeSource(sourceId: Long) = sourceDao.delete(sourceId)

    /**
     * Strumieniowa synchronizacja playlisty M3U.
     *
     * Kanały są zapisywane partiami po [BATCH_SIZE] — RAM nie rośnie z rozmiarem
     * playlisty. Kategorie wyprowadzamy z group-title napotkanych po drodze.
     */
    override fun syncPlaylist(sourceId: Long): Flow<SyncStatus> = flow {
        emit(SyncStatus.Running(0, "Pobieranie playlisty"))

        val source = sources().first().firstOrNull { it.id == sourceId }
        val url = when (source) {
            is PlaylistSource.M3u -> source.url
            else -> {
                emit(SyncStatus.Error("Nieobsługiwane źródło (użyj Xtream synk osobno)"))
                return@flow
            }
        }

        channelDao.clearSource(sourceId)

        val request = Request.Builder().url(url).build()
        httpClient.newCall(request).execute().use { response ->
            val body = response.body
            if (!response.isSuccessful || body == null) {
                emit(SyncStatus.Error("HTTP ${response.code}"))
                return@flow
            }

            val batch = ArrayList<ChannelEntity>(BATCH_SIZE)
            var total = 0
            var order = 0

            m3uParser.parse(body.byteStream()).collect { entry: M3uEntry ->
                batch += entry.toChannelEntity(sourceId, order++)
                if (batch.size >= BATCH_SIZE) {
                    channelDao.insertBatch(batch)
                    total += batch.size
                    batch.clear()
                    emit(SyncStatus.Running(total, "Import kanałów"))
                }
            }
            if (batch.isNotEmpty()) {
                channelDao.insertBatch(batch)
                total += batch.size
            }
            emit(SyncStatus.Done(total))
        }
    }.flowOn(dispatchers.io)

    override fun categories(sourceId: Long, kind: ContentKind): Flow<List<Category>> =
        categoryDao.observe(sourceId, kind.name).map { list -> list.map { it.toDomain() } }

    override fun channels(sourceId: Long, categoryId: String?): Flow<List<Channel>> =
        channelDao.observeByCategory(sourceId, categoryId)
            .map { list -> list.map { it.toDomain() } }

    override suspend fun channelAt(sourceId: Long, categoryId: String?, index: Int): Channel? =
        channelDao.channelAt(sourceId, categoryId, index)?.toDomain()

    override suspend fun channelCount(sourceId: Long, categoryId: String?): Int =
        channelDao.countByCategory(sourceId, categoryId)

    private fun M3uEntry.toChannelEntity(sourceId: Long, order: Int) = ChannelEntity(
        sourceId = sourceId,
        categoryId = groupTitle?.let { "$sourceId:$it" },
        name = name,
        tvgId = tvgId,
        logoUrl = tvgLogo,
        streamUrl = url,
        sortOrder = order,
        kind = ContentKind.LIVE.name,
    )

    private companion object {
        const val BATCH_SIZE = 500
    }
}
