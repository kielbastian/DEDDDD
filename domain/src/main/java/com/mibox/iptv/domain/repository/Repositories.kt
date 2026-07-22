package com.mibox.iptv.domain.repository

import com.mibox.iptv.domain.model.Category
import com.mibox.iptv.domain.model.Channel
import com.mibox.iptv.domain.model.ContentKind
import com.mibox.iptv.domain.model.NowNext
import com.mibox.iptv.domain.model.PlaylistSource
import kotlinx.coroutines.flow.Flow

/** Postęp synchronizacji playlisty / EPG raportowany do UI. */
sealed interface SyncStatus {
    data object Idle : SyncStatus
    data class Running(val processed: Int, val label: String) : SyncStatus
    data class Done(val total: Int) : SyncStatus
    data class Error(val message: String) : SyncStatus
}

interface PlaylistRepository {
    fun sources(): Flow<List<PlaylistSource>>
    suspend fun addSource(source: PlaylistSource): Long
    suspend fun removeSource(sourceId: Long)

    /** Strumieniowa synchronizacja — parsowanie i batch insert do bazy. */
    fun syncPlaylist(sourceId: Long): Flow<SyncStatus>

    fun categories(sourceId: Long, kind: ContentKind): Flow<List<Category>>

    /** Kanały dla kategorii — zwracane jako Flow list (w prezentacji owijane w Paging). */
    fun channels(sourceId: Long, categoryId: String?): Flow<List<Channel>>

    suspend fun channelAt(sourceId: Long, categoryId: String?, index: Int): Channel?
    suspend fun channelCount(sourceId: Long, categoryId: String?): Int
}

interface EpgRepository {
    fun syncEpg(sourceId: Long): Flow<SyncStatus>
    fun nowNext(channelTvgId: String): Flow<NowNext>
    suspend fun cleanupExpired(beforeMillis: Long)
}
