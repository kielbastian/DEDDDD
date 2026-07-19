package pl.radiofala.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FavoriteStationEntity::class], version = 1, exportSchema = false)
abstract class RadioDatabase : RoomDatabase() {

    abstract fun favoriteDao(): FavoriteDao

    companion object {
        @Volatile
        private var instance: RadioDatabase? = null

        fun get(context: Context): RadioDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    RadioDatabase::class.java,
                    "radio_favorites.db"
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
    }
}
