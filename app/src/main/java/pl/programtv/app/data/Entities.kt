package pl.programtv.app.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Kanał telewizyjny z pliku XMLTV. */
@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val iconUrl: String?,
    /** Czy użytkownik wybrał ten kanał do swojego programu TV. */
    val selected: Boolean = false
)

/** Pojedyncza pozycja programu TV. */
@Entity(
    tableName = "programmes",
    indices = [
        Index("channelId"),
        Index("startMillis"),
        Index("title")
    ]
)
data class ProgrammeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelId: String,
    val title: String,
    val description: String?,
    val category: String?,
    val startMillis: Long,
    val stopMillis: Long
)

/** Pozycja programu razem z nazwą kanału – wynik wyszukiwania / widok "teraz". */
data class ProgrammeWithChannel(
    @Embedded val programme: ProgrammeEntity,
    val channelName: String
)
