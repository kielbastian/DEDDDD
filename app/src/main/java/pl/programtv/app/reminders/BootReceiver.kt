package pl.programtv.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pl.programtv.app.data.EpgDatabase

/** Po restarcie telefonu odtwarza alarmy dla przyszłych przypomnień (AlarmManager je czyści). */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = EpgDatabase.get(context).reminderDao()
                dao.futureReminders(System.currentTimeMillis()).forEach { reminder ->
                    ReminderScheduler.schedule(context, reminder)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
