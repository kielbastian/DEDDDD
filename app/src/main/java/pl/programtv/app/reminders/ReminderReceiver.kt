package pl.programtv.app.reminders

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pl.programtv.app.MainActivity
import pl.programtv.app.R
import pl.programtv.app.data.EpgDatabase
import pl.programtv.app.ui.formatTime

private const val CHANNEL_ID = "program_reminders"

/** Odbiera zaplanowany alarm i pokazuje powiadomienie o zbliżającym się programie. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val key = intent.getStringExtra(ReminderScheduler.EXTRA_KEY) ?: return
        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE).orEmpty()
        val channelName = intent.getStringExtra(ReminderScheduler.EXTRA_CHANNEL).orEmpty()
        val startMillis = intent.getLongExtra(ReminderScheduler.EXTRA_START, 0L)

        ensureChannel(context)
        showNotification(context, key, title, channelName, startMillis)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                EpgDatabase.get(context).reminderDao().deleteByKey(key)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Przypomnienia o programach",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Powiadomienia o filmach i programach oznaczonych gwiazdką"
        }
        manager.createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission")
    private fun showNotification(
        context: Context,
        key: String,
        title: String,
        channelName: String,
        startMillis: Long
    ) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            key.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeText = if (startMillis > 0) " o ${formatTime(startMillis)}" else ""
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Zaraz na antenie: $title")
            .setContentText("$channelName$timeText")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$channelName$timeText"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        NotificationManagerCompat.from(context).notify(key.hashCode(), notification)
    }
}
