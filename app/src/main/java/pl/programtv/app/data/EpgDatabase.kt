package pl.programtv.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ChannelEntity::class, ProgrammeEntity::class, ReminderEntity::class],
    version = 2,
    exportSchema = false
)
abstract class EpgDatabase : RoomDatabase() {

    abstract fun epgDao(): EpgDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var instance: EpgDatabase? = null

        fun get(context: Context): EpgDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    EpgDatabase::class.java,
                    "epg.db"
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
    }
}
