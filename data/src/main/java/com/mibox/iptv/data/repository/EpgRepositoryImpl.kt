package com.mibox.iptv.data.repository

import com.mibox.iptv.core.DispatcherProvider
import com.mibox.iptv.data.local.dao.EpgDao
import com.mibox.iptv.data.local.dao.SourceDao
import com.mibox.iptv.data.local.entity.EpgProgramEntity
import com.mibox.iptv.data.mapper.toDomain
import com.mibox.iptv.data.parser.XmltvParser
import com.mibox.iptv.domain.model.NowNext
import com.mibox.iptv.domain.repository.EpgRepository
import com.mibox.iptv.domain.repository.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

class EpgRepositoryImpl @Inject constructor(
    private val epgDao: EpgDao,
    private val sourceDao: SourceDao,
    private val xmltvParser: XmltvParser,
    private val httpClient: OkHttpClient,
    private val dispatchers: DispatcherProvider,
) : EpgRepository {

    override fun syncEpg(sourceId: Long): Flow<SyncStatus> = flow {
        emit(SyncStatus.Running(0, "Pobieranie EPG"))

        val source = sourceDao.observeAll().first().firstOrNull { it.id == sourceId }
        val epgUrl = source?.epgUrl
        if (epgUrl.isNullOrBlank()) {
            emit(SyncStatus.Error("Brak URL EPG dla źródła"))
            return@flow
        }

        // Usuwamy przeterminowane wpisy przed importem, żeby baza nie puchła.
        epgDao.deleteExpired(System.currentTimeMillis() - RETENTION_MILLIS)

        val request = Request.Builder().url(epgUrl).build()
        httpClient.newCall(request).execute().use { response ->
            val body = response.body
            if (!response.isSuccessful || body == null) {
                emit(SyncStatus.Error("HTTP ${response.code}"))
                return@flow
            }

            val gzip = epgUrl.endsWith(".gz", ignoreCase = true)
            val batch = ArrayList<EpgProgramEntity>(BATCH_SIZE)
            var total = 0

            xmltvParser.parse(body.byteStream(), gzip).collect { programme ->
                batch += EpgProgramEntity(
                    channelTvgId = programme.channelId,
                    startMillis = programme.startMillis,
                    endMillis = programme.endMillis,
                    title = programme.title,
                    description = programme.description,
                )
                if (batch.size >= BATCH_SIZE) {
                    epgDao.insertBatch(batch)
                    total += batch.size
                    batch.clear()
                    emit(SyncStatus.Running(total, "Import EPG"))
                }
            }
            if (batch.isNotEmpty()) {
                epgDao.insertBatch(batch)
                total += batch.size
            }
            emit(SyncStatus.Done(total))
        }
    }.flowOn(dispatchers.io)

    override fun nowNext(channelTvgId: String): Flow<NowNext> {
        val now = System.currentTimeMillis()
        return combine(
            epgDao.observeNow(channelTvgId, now),
            epgDao.observeNext(channelTvgId, now),
        ) { nowEntity, nextEntity ->
            NowNext(now = nowEntity?.toDomain(), next = nextEntity?.toDomain())
        }
    }

    override suspend fun cleanupExpired(beforeMillis: Long) = epgDao.deleteExpired(beforeMillis)

    private companion object {
        const val BATCH_SIZE = 500
        const val RETENTION_MILLIS = 6L * 60 * 60 * 1000 // przeszłość >6h usuwana
    }
}
