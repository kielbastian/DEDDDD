package com.mibox.iptv.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mibox.iptv.data.local.dao.CategoryDao
import com.mibox.iptv.data.local.dao.ChannelDao
import com.mibox.iptv.data.local.dao.EpgDao
import com.mibox.iptv.data.local.dao.SourceDao
import com.mibox.iptv.data.local.entity.CategoryEntity
import com.mibox.iptv.data.local.entity.ChannelEntity
import com.mibox.iptv.data.local.entity.EpgProgramEntity
import com.mibox.iptv.data.local.entity.SourceEntity

@Database(
    entities = [
        SourceEntity::class,
        CategoryEntity::class,
        ChannelEntity::class,
        EpgProgramEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class IptvDatabase : RoomDatabase() {
    abstract fun sourceDao(): SourceDao
    abstract fun categoryDao(): CategoryDao
    abstract fun channelDao(): ChannelDao
    abstract fun epgDao(): EpgDao

    companion object {
        const val NAME = "mibox_iptv.db"
    }
}
