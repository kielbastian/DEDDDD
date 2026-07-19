package pl.radiofala.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Stacja radiowa pobrana z publicznej bazy Radio-Browser. */
data class RadioStation(
    val stationUuid: String,
    val name: String,
    val streamUrl: String,
    val faviconUrl: String?,
    val tags: String,
    val countryCode: String?,
    val bitrateKbps: Int
)

/** Ulubiona stacja zapisana lokalnie – działa też offline (lista, nie odtwarzanie). */
@Entity(tableName = "favorite_stations")
data class FavoriteStationEntity(
    @PrimaryKey val stationUuid: String,
    val name: String,
    val streamUrl: String,
    val faviconUrl: String?,
    val tags: String,
    val countryCode: String?,
    val addedAtMillis: Long
)

fun RadioStation.toFavoriteEntity(addedAtMillis: Long = System.currentTimeMillis()) =
    FavoriteStationEntity(
        stationUuid = stationUuid,
        name = name,
        streamUrl = streamUrl,
        faviconUrl = faviconUrl,
        tags = tags,
        countryCode = countryCode,
        addedAtMillis = addedAtMillis
    )

fun FavoriteStationEntity.toStation() = RadioStation(
    stationUuid = stationUuid,
    name = name,
    streamUrl = streamUrl,
    faviconUrl = faviconUrl,
    tags = tags,
    countryCode = countryCode,
    bitrateKbps = 0
)

/** Sposób wypełnienia kategorii: tag Radio-Browser, kod kraju, ulubione lub wyszukiwanie. */
sealed class CategorySource {
    data class Tag(val tag: String) : CategorySource()
    data class Country(val code: String) : CategorySource()
    object Favorites : CategorySource()
}

data class RadioCategory(
    val id: String,
    val label: String,
    val accent: Long,
    val source: CategorySource
)

val RADIO_CATEGORIES = listOf(
    RadioCategory("favorites", "Ulubione", 0xFFE0A548, CategorySource.Favorites),
    RadioCategory("poland", "Polskie stacje", 0xFFEF4444, CategorySource.Country("PL")),
    RadioCategory("80s", "Lata 80", 0xFFEC4899, CategorySource.Tag("80s")),
    RadioCategory("90s", "Lata 90", 0xFF8B5CF6, CategorySource.Tag("90s")),
    RadioCategory("2000s", "Lata 2000", 0xFF3B82F6, CategorySource.Tag("2000s")),
    RadioCategory("rock", "Rock", 0xFF22C55E, CategorySource.Tag("rock")),
    RadioCategory("pop", "Pop", 0xFF46C7D8, CategorySource.Tag("pop")),
    RadioCategory("dance", "Dance / Disco", 0xFFF97316, CategorySource.Tag("dance")),
    RadioCategory("discopolo", "Disco Polo", 0xFFD946EF, CategorySource.Tag("disco polo")),
    RadioCategory("chillout", "Chillout", 0xFF14B8A6, CategorySource.Tag("chillout")),
    RadioCategory("classical", "Muzyka klasyczna", 0xFF64748B, CategorySource.Tag("classical")),
    RadioCategory("jazz", "Jazz", 0xFFA16207, CategorySource.Tag("jazz"))
)
