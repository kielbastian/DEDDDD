package com.mibox.iptv.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [Index(value = ["sourceId", "kind"])],
)
data class CategoryEntity(
    @PrimaryKey val id: String,       // "sourceId:remoteCategoryId"
    val sourceId: Long,
    val remoteId: String,
    val name: String,
    val kind: String,                 // LIVE | VOD | SERIES
)

@Entity(
    tableName = "channels",
    indices = [
        Index(value = ["sourceId", "categoryId", "sortOrder"]),
        Index(value = ["tvgId"]),
        Index(value = ["name"]),
    ],
)
data class ChannelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceId: Long,
    val categoryId: String?,
    val name: String,
    val tvgId: String?,
    val logoUrl: String?,
    val streamUrl: String,
    val sortOrder: Int,
    val kind: String,
)

@Entity(
    tableName = "epg_programs",
    indices = [
        // Klucz do zapytań now/next: filtr po kanale + zakres czasu.
        Index(value = ["channelTvgId", "startMillis", "endMillis"]),
    ],
)
data class EpgProgramEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelTvgId: String,
    val startMillis: Long,
    val endMillis: Long,
    val title: String,
    val description: String?,
)

@Entity(tableName = "sources")
data class SourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,                 // M3U | XTREAM
    val name: String,
    val url: String?,                 // M3U url lub Xtream host
    val epgUrl: String?,
    val username: String?,
    val password: String?,
)
