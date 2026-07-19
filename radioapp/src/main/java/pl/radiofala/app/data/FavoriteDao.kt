package pl.radiofala.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Query("SELECT * FROM favorite_stations ORDER BY addedAtMillis DESC")
    fun observeFavorites(): Flow<List<FavoriteStationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteStationEntity)

    @Query("DELETE FROM favorite_stations WHERE stationUuid = :stationUuid")
    suspend fun deleteByUuid(stationUuid: String)
}
