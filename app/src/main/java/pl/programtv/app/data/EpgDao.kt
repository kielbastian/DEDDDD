package pl.programtv.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface EpgDao {

    @Query("SELECT * FROM channels ORDER BY displayName COLLATE NOCASE")
    fun observeChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE selected = 1 ORDER BY displayName COLLATE NOCASE")
    fun observeSelectedChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT id FROM channels WHERE selected = 1")
    suspend fun selectedChannelIds(): List<String>

    @Query("UPDATE channels SET selected = :selected WHERE id = :channelId")
    suspend fun setChannelSelected(channelId: String, selected: Boolean)

    @Query("UPDATE channels SET selected = :selected WHERE id IN (:channelIds)")
    suspend fun setChannelsSelected(channelIds: List<String>, selected: Boolean)

    /** Co leci teraz na wybranych kanałach. */
    @Query(
        """
        SELECT p.*, c.displayName AS channelName, c.iconUrl AS channelIconUrl
        FROM programmes p
        JOIN channels c ON c.id = p.channelId
        WHERE c.selected = 1 AND p.startMillis <= :now AND p.stopMillis > :now
        ORDER BY c.displayName COLLATE NOCASE
        """
    )
    fun observeNowPlaying(now: Long): Flow<List<ProgrammeWithChannel>>

    /** Program jednego kanału od podanej chwili. */
    @Query(
        """
        SELECT p.*, c.displayName AS channelName, c.iconUrl AS channelIconUrl
        FROM programmes p
        JOIN channels c ON c.id = p.channelId
        WHERE p.channelId = :channelId AND p.stopMillis > :from
        ORDER BY p.startMillis
        LIMIT 300
        """
    )
    fun observeSchedule(channelId: String, from: Long): Flow<List<ProgrammeWithChannel>>

    /**
     * Wyszukiwanie po tytule (wszystkie kanały): pokazuje pozycje,
     * które jeszcze się nie skończyły.
     */
    @Query(
        """
        SELECT p.*, c.displayName AS channelName, c.iconUrl AS channelIconUrl
        FROM programmes p
        JOIN channels c ON c.id = p.channelId
        WHERE p.title LIKE '%' || :query || '%' AND p.stopMillis > :now
        ORDER BY p.startMillis
        LIMIT 300
        """
    )
    suspend fun searchByTitle(query: String, now: Long): List<ProgrammeWithChannel>

    /** Podpowiedzi tytułów do wyszukiwarki (pozycje jeszcze nadawane). */
    @Query(
        """
        SELECT DISTINCT p.title
        FROM programmes p
        WHERE p.title LIKE '%' || :query || '%' AND p.stopMillis > :now
        ORDER BY p.title COLLATE NOCASE
        LIMIT 30
        """
    )
    suspend fun suggestTitles(query: String, now: Long): List<String>

    /**
     * Kandydaci na filmy pełnometrażowe: pozycje o odpowiednio długim czasie
     * trwania, które jeszcze się nie zaczęły lub trwają. Gatunek dobierany
     * jest później na podstawie kategorii/opisu.
     */
    @Query(
        """
        SELECT p.*, c.displayName AS channelName, c.iconUrl AS channelIconUrl
        FROM programmes p
        JOIN channels c ON c.id = p.channelId
        WHERE p.stopMillis > :now AND (p.stopMillis - p.startMillis) >= :minDurationMillis
        ORDER BY p.startMillis
        LIMIT 1200
        """
    )
    suspend fun candidateMovies(now: Long, minDurationMillis: Long): List<ProgrammeWithChannel>

    @Query("SELECT COUNT(*) FROM programmes")
    suspend fun programmeCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Insert
    suspend fun insertProgrammes(programmes: List<ProgrammeEntity>)

    @Query("DELETE FROM channels")
    suspend fun clearChannels()

    @Query("DELETE FROM programmes")
    suspend fun clearProgrammes()

    /**
     * Podmienia cały program TV, zachowując zaznaczenie kanałów
     * wybranych wcześniej przez użytkownika.
     */
    @Transaction
    suspend fun replaceAll(channels: List<ChannelEntity>, programmes: List<ProgrammeEntity>) {
        val selected = selectedChannelIds().toSet()
        clearProgrammes()
        clearChannels()
        insertChannels(channels.map { it.copy(selected = it.id in selected) })
        programmes.chunked(500).forEach { insertProgrammes(it) }
    }
}
