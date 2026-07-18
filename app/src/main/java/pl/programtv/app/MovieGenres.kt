package pl.programtv.app

import pl.programtv.app.data.ProgrammeEntity

/** Kategoria filmowa z etykietą i słowami kluczowymi dopasowania. */
data class MovieGenre(val id: String, val label: String, val keywords: List<String>)

/** Minimalny czas trwania uznawany za film pełnometrażowy (70 minut). */
const val MIN_MOVIE_DURATION_MILLIS = 70L * 60L * 1000L

/** Dostępne zakładki gatunków na ekranie „Filmy”. */
val MOVIE_GENRES = listOf(
    MovieGenre("all", "Wszystkie", emptyList()),
    MovieGenre("horror", "Horrory", listOf("horror", "groza", "slasher")),
    MovieGenre(
        "action", "Sensacyjne",
        listOf("sensacyjny", "sensacja", "akcja", "thriller", "kryminał", "kryminalny", "szpiegowski")
    ),
    MovieGenre(
        "romcom", "Komedie rom.",
        listOf("komedia romantyczna", "romantyczn", "romans")
    ),
    MovieGenre(
        "martial", "Sztuki walki",
        listOf("sztuki walki", "kung fu", "karate", "martial", "ninja", "samuraj")
    ),
    MovieGenre(
        "scifi", "Sci-Fi",
        listOf("science fiction", "sci-fi", "fantastycznonaukowy", "fantasy", "fantastyka")
    ),
    MovieGenre("war", "Wojenne", listOf("wojenny", "wojna", "militarny")),
    MovieGenre("comedy", "Komedie", listOf("komedia", "komediow")),
    MovieGenre("family", "Familijne", listOf("familijny", "dla dzieci", "animowany", "animacja", "bajka"))
)

// Słowa wskazujące, że pozycja to film (a nie np. sport czy magazyn).
private val FILM_MARKERS: List<String> = buildList {
    add("film")
    add("kino")
    add("western")
    add("przygodowy")
    add("biograficzny")
    add("musical")
    add("dramat")
    add("obyczajowy")
    MOVIE_GENRES.forEach { addAll(it.keywords) }
}.distinct()

/** Tekst, po którym rozpoznajemy gatunek (kategoria + tytuł + opis). */
private fun ProgrammeEntity.genreText(): String =
    ((category ?: "") + " " + title + " " + (description ?: "")).lowercase()

/** Czy pozycja wygląda na film pełnometrażowy. */
fun isMovie(programme: ProgrammeEntity): Boolean {
    if (programme.stopMillis - programme.startMillis < MIN_MOVIE_DURATION_MILLIS) return false
    val text = programme.genreText()
    return FILM_MARKERS.any { text.contains(it) }
}

/** Czy film pasuje do wybranego gatunku ("all" pasuje zawsze). */
fun matchesGenre(programme: ProgrammeEntity, genre: MovieGenre): Boolean {
    if (genre.id == "all") return true
    val text = programme.genreText()
    return genre.keywords.any { text.contains(it) }
}
