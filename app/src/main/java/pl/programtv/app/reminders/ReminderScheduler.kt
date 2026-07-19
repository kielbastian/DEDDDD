package pl.programtv.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import pl.programtv.app.data.ReminderEntity

/** Planuje i odwołuje lokalne powiadomienia-przypomnienia o programach. */
object ReminderScheduler {

    /** Ile przed startem programu wysyłamy przypomnienie. */
    private const val LEAD_MILLIS = 2 * 60 * 1000L

    const val EXTRA_KEY = "key"
    const val EXTRA_TITLE = "title"
    const val EXTRA_CHANNEL = "channel"
    const val EXTRA_START = "start"

    fun schedule(context: Context, reminder: ReminderEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = (reminder.startMillis - LEAD_MILLIS)
            .coerceAtLeast(System.currentTimeMillis() + 1_000)

        // create=true (domyślne) zawsze zwraca nie-null PendingIntent.
        val pendingIntent = buildPendingIntent(
            context, reminder.key, reminder.title, reminder.channelName, reminder.startMillis
        )!!
        try {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } catch (_: SecurityException) {
            // Brak uprawnień do alarmów w tle na niektórych urządzeniach – pomijamy po cichu.
        }
    }

    fun cancel(context: Context, key: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildPendingIntent(context, key, "", "", 0L, create = false) ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun buildPendingIntent(
        context: Context,
        key: String,
        title: String,
        channelName: String,
        startMillis: Long,
        create: Boolean = true
    ): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_KEY, key)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_CHANNEL, channelName)
            putExtra(EXTRA_START, startMillis)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE or
            if (!create) PendingIntent.FLAG_NO_CREATE else 0
        return PendingIntent.getBroadcast(context, key.hashCode(), intent, flags)
    }
}
