package com.mibox.iptv.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.mibox.iptv.data.local.entity.CategoryEntity
import com.mibox.iptv.data.local.entity.ChannelEntity
import com.mibox.iptv.data.local.entity.EpgProgramEntity
import com.mibox.iptv.data.local.entity.SourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceDao {
    @Query("SELECT * FROM sources")
    fun observeAll(): Flow<List<SourceEntity>>

    @Insert
    suspend fun insert(source: SourceEntity): Long

    @Query("DELETE FROM sources WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface CategoryDao {
    @Upsert
    suspend fun upsertAll(items: List<CategoryEntity>)

    @Query("SELECT * FROM categories WHERE sourceId = :sourceId AND kind = :kind ORDER BY name")
    fun observe(sourceId: Long, kind: String): Flow<List<CategoryEntity>>
}

@Dao
interface ChannelDao {
    // Batch insert w transakcji — sercem strumieniowego importu wielkich playlist.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(items: List<ChannelEntity>)

    @Query("DELETE FROM channels WHERE sourceId = :sourceId")
    suspend fun clearSource(sourceId: Long)

    @Query(
        """
        SELECT * FROM channels
        WHERE sourceId = :sourceId
          AND (:categoryId IS NULL OR categoryId = :categoryId)
        ORDER BY sortOrder
        """
    )
    fun pagingByCategory(sourceId: Long, categoryId: String?): PagingSource<Int, ChannelEntity>

    @Query(
        """
        SELECT * FROM channels
        WHERE sourceId = :sourceId
          AND (:categoryId IS NULL OR categoryId = :categoryId)
        ORDER BY sortOrder
        """
    )
    fun observeByCategory(sourceId: Long, categoryId: String?): Flow<List<ChannelEntity>>

    // Quick Zap: kanał na konkretnym offsetcie bez ładowania całej listy.
    @Query(
        """
        SELECT * FROM channels
        WHERE sourceId = :sourceId
          AND (:categoryId IS NULL OR categoryId = :categoryId)
        ORDER BY sortOrder
        LIMIT 1 OFFSET :index
        """
    )
    suspend fun channelAt(sourceId: Long, categoryId: String?, index: Int): ChannelEntity?

    @Query(
        """
        SELECT COUNT(*) FROM channels
        WHERE sourceId = :sourceId
          AND (:categoryId IS NULL OR categoryId = :categoryId)
        """
    )
    suspend fun countByCategory(sourceId: Long, categoryId: String?): Int
}

@Dao
interface EpgDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(items: List<EpgProgramEntity>)

    @Query("DELETE FROM epg_programs WHERE endMillis < :beforeMillis")
    suspend fun deleteExpired(beforeMillis: Long)

    // Program "teraz" — indeks (channelTvgId, start, end) czyni to zapytanie punktowym.
    @Query(
        """
        SELECT * FROM epg_programs
        WHERE channelTvgId = :tvgId AND startMillis <= :now AND endMillis > :now
        ORDER BY startMillis DESC LIMIT 1
        """
    )
    fun observeNow(tvgId: String, now: Long): Flow<EpgProgramEntity?>

    // Kolejny program po "teraz".
    @Query(
        """
        SELECT * FROM epg_programs
        WHERE channelTvgId = :tvgId AND startMillis > :now
        ORDER BY startMillis ASC LIMIT 1
        """
    )
    fun observeNext(tvgId: String, now: Long): Flow<EpgProgramEntity?>
}
