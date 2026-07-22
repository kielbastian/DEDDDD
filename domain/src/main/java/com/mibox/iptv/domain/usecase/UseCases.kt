package com.mibox.iptv.domain.usecase

import com.mibox.iptv.domain.model.Category
import com.mibox.iptv.domain.model.Channel
import com.mibox.iptv.domain.model.ContentKind
import com.mibox.iptv.domain.model.NowNext
import com.mibox.iptv.domain.repository.EpgRepository
import com.mibox.iptv.domain.repository.PlaylistRepository
import com.mibox.iptv.domain.repository.SyncStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SyncPlaylistUseCase @Inject constructor(
    private val playlist: PlaylistRepository,
    private val epg: EpgRepository,
) {
    /** Najpierw kanały, potem EPG — obie fazy strumieniowo. */
    operator fun invoke(sourceId: Long): Flow<SyncStatus> = playlist.syncPlaylist(sourceId)
    fun epgOnly(sourceId: Long): Flow<SyncStatus> = epg.syncEpg(sourceId)
}

class GetCategoriesUseCase @Inject constructor(
    private val playlist: PlaylistRepository,
) {
    operator fun invoke(sourceId: Long, kind: ContentKind): Flow<List<Category>> =
        playlist.categories(sourceId, kind)
}

class GetChannelsUseCase @Inject constructor(
    private val playlist: PlaylistRepository,
) {
    operator fun invoke(sourceId: Long, categoryId: String?): Flow<List<Channel>> =
        playlist.channels(sourceId, categoryId)
}

/** Wspiera Quick Zap — pobranie sąsiedniego kanału po indeksie. */
class GetAdjacentChannelUseCase @Inject constructor(
    private val playlist: PlaylistRepository,
) {
    suspend operator fun invoke(
        sourceId: Long,
        categoryId: String?,
        currentIndex: Int,
        delta: Int,
    ): Channel? {
        val count = playlist.channelCount(sourceId, categoryId)
        if (count == 0) return null
        val target = ((currentIndex + delta) % count + count) % count // zawijanie
        return playlist.channelAt(sourceId, categoryId, target)
    }
}

class GetNowNextUseCase @Inject constructor(
    private val epg: EpgRepository,
) {
    operator fun invoke(channelTvgId: String): Flow<NowNext> = epg.nowNext(channelTvgId)
}
