package pl.programtv.app.ui

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val polish = Locale("pl", "PL")
private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", polish)
private val dayFormat = DateTimeFormatter.ofPattern("EEEE, d MMMM", polish)
private val shortDateTimeFormat = DateTimeFormatter.ofPattern("EEE d.MM, HH:mm", polish)

fun formatTime(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(timeFormat)

fun formatShortDateTime(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(shortDateTimeFormat)

fun localDateOf(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

/** Nagłówek dnia: "Dzisiaj", "Jutro" albo pełna data po polsku. */
fun formatDayHeader(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "Dzisiaj"
        today.plusDays(1) -> "Jutro"
        else -> date.format(dayFormat).replaceFirstChar { it.uppercase(polish) }
    }
}

/** Postęp trwającej audycji w zakresie 0–1. */
fun progressOf(startMillis: Long, stopMillis: Long, now: Long = System.currentTimeMillis()): Float {
    if (stopMillis <= startMillis) return 0f
    return ((now - startMillis).toFloat() / (stopMillis - startMillis)).coerceIn(0f, 1f)
}
