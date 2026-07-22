package com.mibox.iptv.data.repository

import com.mibox.iptv.core.DispatcherProvider
import com.mibox.iptv.data.local.dao.CategoryDao
import com.mibox.iptv.data.local.dao.ChannelDao
import com.mibox.iptv.data.local.dao.SourceDao
import com.mibox.iptv.data.local.entity.CategoryEntity
import com.mibox.iptv.data.local.entity.ChannelEntity
import com.mibox.iptv.data.mapper.toDomain
import com.mibox.iptv.data.mapper.toEntity
import com.mibox.iptv.data.remote.xtream.XtreamApiFactory
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
    private val xtreamApiFactory: XtreamApiFactory,
    private val httpClient: OkHttpClient,
    private val dispatchers: DispatcherProvider,
) : PlaylistRepository {

    override fun sources(): Flow<List<PlaylistSource>> =
        sourceDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun addSource(source: PlaylistSource): Long =
        sourceDao.insert(source.toEntity())

    override suspend fun removeSource(sourceId: Long) = sourceDao.delete(sourceId)

    override fun syncPlaylist(sourceId: Long): Flow<SyncStatus> = flow {
        emit(SyncStatus.Running(0, "Przygotowanie"))
        val source = sources().first().firstOrNull { it.id == sourceId }
        when (source) {
            is PlaylistSource.M3u -> syncM3u(source).collect { emit(it) }
            is PlaylistSource.Xtream -> syncXtream(source).collect { emit(it) }
            null -> emit(SyncStatus.Error("Nie znaleziono źródła"))
        }
    }.flowOn(dispatchers.io)

    // ---- M3U ----------------------------------------------------------------

    private fun syncM3u(source: PlaylistSource.M3u): Flow<SyncStatus> = flow {
        emit(SyncStatus.Running(0, "Pobieranie playlisty"))
        channelDao.clearSource(source.id)

        val request = Request.Builder().url(source.url).build()
        httpClient.newCall(request).execute().use { response ->
            val body = response.body
            if (!response.isSuccessful || body == null) {
                emit(SyncStatus.Error("HTTP ${response.code}"))
                return@flow
            }

            val batch = ArrayList<ChannelEntity>(BATCH_SIZE)
            val groups = LinkedHashSet<String>()
            var total = 0
            var order = 0

            m3uParser.parse(body.byteStream()).collect { entry: M3uEntry ->
                entry.groupTitle?.let { groups.add(it) }
                batch += entry.toChannelEntity(source.id, order++)
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

            // Kategorie wyprowadzone z group-title napotkanych w playliście.
            categoryDao.upsertAll(
                groups.map { g ->
                    CategoryEntity(
                        id = "${source.id}:$g",
                        sourceId = source.id,
                        remoteId = g,
                        name = g,
                        kind = ContentKind.LIVE.name,
                    )
                }
            )
            emit(SyncStatus.Done(total))
        }
    }

    // ---- Xtream Codes -------------------------------------------------------

    private fun syncXtream(source: PlaylistSource.Xtream): Flow<SyncStatus> = flow {
        emit(SyncStatus.Running(0, "Łączenie z serwerem Xtream"))
        val api = xtreamApiFactory.create(source.host)
        channelDao.clearSource(source.id)

        // Kategorie live.
        val categories = api.getLiveCategories(source.username, source.password)
        categoryDao.upsertAll(
            categories.map { dto ->
                CategoryEntity(
                    id = "${source.id}:${dto.categoryId}",
                    sourceId = source.id,
                    remoteId = dto.categoryId,
                    name = dto.categoryName,
                    kind = ContentKind.LIVE.name,
                )
            }
        )
        emit(SyncStatus.Running(categories.size, "Pobrano kategorie"))

        // Strumienie live → kanały (batch insert).
        val streams = api.getLiveStreams(source.username, source.password)
        val batch = ArrayList<ChannelEntity>(BATCH_SIZE)
        var total = 0
        streams.forEachIndexed { index, dto ->
            batch += ChannelEntity(
                sourceId = source.id,
                categoryId = dto.categoryId?.let { "${source.id}:$it" },
                name = dto.name,
                tvgId = dto.epgChannelId,
                logoUrl = dto.icon,
                streamUrl = XtreamApiFactory.liveStreamUrl(
                    source.host, source.username, source.password, dto.streamId,
                ),
                sortOrder = index,
                kind = ContentKind.LIVE.name,
            )
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

    // ---- odczyt -------------------------------------------------------------

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
