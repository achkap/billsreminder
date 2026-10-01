package gr.logariasmoi.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import gr.logariasmoi.MainActivity
import gr.logariasmoi.R
import gr.logariasmoi.data.Bill
import gr.logariasmoi.data.Fmt
import gr.logariasmoi.data.Providers
import gr.logariasmoi.data.Repository
import gr.logariasmoi.data.reminderEvents
import gr.logariasmoi.data.tr
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private const val CHANNEL_ID = "reminders"
private const val HOUR = 3_600_000L
private const val DAY = 24 * HOUR

object ReminderScheduler {

    fun canScheduleExact(context: Context): Boolean {
        val am = context.getSystemService(AlarmManager::class.java)
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
    }

    /** Arms a single alarm for the earliest upcoming reminder. */
    fun schedule(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = PendingIntent.getBroadcast(
            context, 0, Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val now = System.currentTimeMillis()
        val next = Repository.current.reminderEvents(now, now + 400 * DAY).firstOrNull()
        if (next == null) {
            am.cancel(pi); return
        }
        if (canScheduleExact(context)) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.at, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.at, pi)
        }
    }

    /** Shows every reminder that became due since the last check, then re-arms the alarm. */
    fun process(context: Context) {
        Repository.init(context)
        val data = Repository.current
        val now = System.currentTimeMillis()
        val from = maxOf(data.lastReminderCheck, now - 2 * DAY)
        data.reminderEvents(from, now)
            .groupBy { it.bill.id to it.date }
            .forEach { (_, events) -> Notifications.show(context, events.last().bill, events.last().date) }
        Repository.setLastReminderCheck(now)
        schedule(context)
    }
}

object Notifications {
    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, tr("Υπενθυμίσεις πληρωμών", "Payment reminders"), NotificationManager.IMPORTANCE_HIGH).apply {
            description = tr("Ειδοποιήσεις για λογαριασμούς που πλησιάζουν ή έχουν λήξει", "Alerts for bills that are due soon or overdue")
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun id(billId: String, date: LocalDate) = (billId + date).hashCode()

    fun canPost(context: Context) = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun show(context: Context, bill: Bill, date: LocalDate) {
        if (!canPost(context)) return
        val today = LocalDate.now()
        val days = ChronoUnit.DAYS.between(today, date)
        val whenText = when {
            days < 0 -> tr("Ληξιπρόθεσμος! Έληγε", "Overdue! Was due") + " ${Fmt.short(date)} (${Fmt.relative(date, today)})"
            days == 0L -> tr("Λήγει σήμερα", "Due today")
            else -> tr("Λήγει", "Due") + " ${Fmt.relative(date, today)}, ${Fmt.short(date)}"
        }
        val details = buildList {
            add(whenText)
            if (bill.autoDebit) add(tr("Πληρώνεται με πάγια εντολή", "Paid by direct debit"))
            if (bill.rfCode.isNotBlank()) add("RF: ${bill.rfCode}")
        }.joinToString("\n")

        val nid = id(bill.id, date)
        val open = PendingIntent.getActivity(
            context, nid,
            Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_BILL_ID, bill.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = if (bill.amount > 0) "${bill.name} · ${Fmt.money(bill.amount)}" else bill.name
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(Providers.get(bill.providerId)?.color?.toInt() ?: 0xFF1565C0.toInt())
            .setContentTitle(title)
            .setContentText(whenText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(details))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(0, tr("Πλήρωσα", "Paid"), ActionReceiver.intent(context, ActionReceiver.PAID, bill.id, date))
            .addAction(0, tr("Αργότερα", "Later"), ActionReceiver.intent(context, ActionReceiver.SNOOZE, bill.id, date))
        if (bill.rfCode.isNotBlank()) {
            builder.addAction(0, tr("Αντιγραφή RF", "Copy RF"), ActionReceiver.intent(context, ActionReceiver.COPY_RF, bill.id, date))
        }
        try {
            NotificationManagerCompat.from(context).notify(nid, builder.build())
        } catch (_: SecurityException) {
        }
    }

    fun cancel(context: Context, billId: String, date: LocalDate) =
        NotificationManagerCompat.from(context).cancel(id(billId, date))
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = ReminderScheduler.process(context)
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = ReminderScheduler.process(context)
}

class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Repository.init(context)
        val billId = intent.getStringExtra(EXTRA_BILL) ?: return
        val date = intent.getStringExtra(EXTRA_DATE)?.let(LocalDate::parse) ?: return
        when (intent.action) {
            PAID -> {
                Repository.markPaid(billId, date)
                Notifications.cancel(context, billId, date)
            }
            SNOOZE -> {
                val minutes = Repository.current.settings.snoozeMinutes
                Repository.snooze(billId, date, System.currentTimeMillis() + minutes * 60_000L)
                Notifications.cancel(context, billId, date)
            }
            COPY_RF -> {
                val rf = Repository.current.bills.firstOrNull { it.id == billId }?.rfCode ?: return
                context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("RF", rf))
            }
        }
    }

    companion object {
        const val PAID = "gr.logariasmoi.PAID"
        const val SNOOZE = "gr.logariasmoi.SNOOZE"
        const val COPY_RF = "gr.logariasmoi.COPY_RF"
        private const val EXTRA_BILL = "bill"
        private const val EXTRA_DATE = "date"

        fun intent(context: Context, action: String, billId: String, date: LocalDate): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                (action + billId + date).hashCode(),
                Intent(context, ActionReceiver::class.java).setAction(action)
                    .putExtra(EXTRA_BILL, billId).putExtra(EXTRA_DATE, date.toString()),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
    }
}
