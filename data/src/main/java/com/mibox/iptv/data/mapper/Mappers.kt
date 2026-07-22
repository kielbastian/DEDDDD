package com.mibox.iptv.data.mapper

import com.mibox.iptv.data.local.entity.CategoryEntity
import com.mibox.iptv.data.local.entity.ChannelEntity
import com.mibox.iptv.data.local.entity.EpgProgramEntity
import com.mibox.iptv.data.local.entity.SourceEntity
import com.mibox.iptv.domain.model.Category
import com.mibox.iptv.domain.model.Channel
import com.mibox.iptv.domain.model.ContentKind
import com.mibox.iptv.domain.model.EpgProgram
import com.mibox.iptv.domain.model.PlaylistSource

fun ChannelEntity.toDomain() = Channel(
    id = id,
    sourceId = sourceId,
    categoryId = categoryId,
    name = name,
    tvgId = tvgId,
    logoUrl = logoUrl,
    streamUrl = streamUrl,
    sortOrder = sortOrder,
    kind = ContentKind.valueOf(kind),
)

fun CategoryEntity.toDomain() = Category(
    id = id,
    sourceId = sourceId,
    name = name,
    kind = ContentKind.valueOf(kind),
)

fun EpgProgramEntity.toDomain() = EpgProgram(
    id = id,
    channelTvgId = channelTvgId,
    startMillis = startMillis,
    endMillis = endMillis,
    title = title,
    description = description,
)

fun SourceEntity.toDomain(): PlaylistSource = when (type) {
    "XTREAM" -> PlaylistSource.Xtream(
        id = id,
        name = name,
        host = url.orEmpty(),
        username = username.orEmpty(),
        password = password.orEmpty(),
    )
    else -> PlaylistSource.M3u(
        id = id,
        name = name,
        url = url.orEmpty(),
        epgUrl = epgUrl,
    )
}

fun PlaylistSource.toEntity(): SourceEntity = when (this) {
    is PlaylistSource.M3u -> SourceEntity(
        id = id, type = "M3U", name = name, url = url, epgUrl = epgUrl,
        username = null, password = null,
    )
    is PlaylistSource.Xtream -> SourceEntity(
        id = id, type = "XTREAM", name = name, url = host, epgUrl = null,
        username = username, password = password,
    )
}
