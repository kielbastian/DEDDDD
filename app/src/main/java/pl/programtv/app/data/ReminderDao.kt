package pl.programtv.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders ORDER BY startMillis")
    fun observeReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE startMillis > :now ORDER BY startMillis")
    suspend fun futureReminders(now: Long): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE key = :key")
    suspend fun deleteByKey(key: String)

    @Query("DELETE FROM reminders WHERE stopMillis < :now")
    suspend fun deleteExpired(now: Long)
}
