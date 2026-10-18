package com.nagesh.zelu

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

// Daily 9 AM (IST) background check: notifies 3 days before, 1 day before and on the
// due day of every unpaid bill / card due, with a "Mark paid" button.
object DueReminders {
    private const val CHANNEL = "zelu_dues"

    fun schedule(context: Context) {
        try {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val at = Calendar.getInstance(Dates.IST).apply {
                set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_MONTH, 1)
            }
            am.setInexactRepeating(AlarmManager.RTC, at.timeInMillis, AlarmManager.INTERVAL_DAY, pending(context))
        } catch (_: Exception) {
        }
    }

    private fun pending(context: Context): PendingIntent {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        return PendingIntent.getBroadcast(context, 7001, Intent(context, DueReceiver::class.java), flags)
    }

    private fun daysLeft(due: String): Int {
        val a = Dates.parse(Dates.today()).timeInMillis
        val b = Dates.parse(due).timeInMillis
        return Math.round((b - a) / 86_400_000.0).toInt()
    }

    fun check(context: Context) {
        if (!NotifyPrefs.duesOn(context)) return
        val leads = NotifyPrefs.leadDays(context)
        val reminders = Storage.loadReminders(context).filter { !it.done }
        if (reminders.isEmpty()) return
        val debits = Storage.loadRows(context).filter { it.direction == "debit" }
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Bill reminders", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        for (r in reminders) {
            val d = daysLeft(r.dueDate)
            if (d !in leads) continue
            // Already paid? A debit of the same amount since just before the bill was raised.
            val from = Dates.shiftDays(r.dueDate, -25)
            if (debits.any { it.date >= from && kotlin.math.abs(it.amount - r.amount) < 1.0 }) continue
            val nid = (r.id.hashCode() and 0x7fffffff) % 900000 + 5000
            val pay = PendingIntent.getBroadcast(context, nid,
                Intent(context, MarkPaidReceiver::class.java).putExtra("id", r.id).putExtra("nid", nid), flags)
            val open = PendingIntent.getActivity(context, 2, launch, flags)
            val b = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                Notification.Builder(context, CHANNEL) else @Suppress("DEPRECATION") Notification.Builder(context)
            val who = r.counterparty?.takeIf { it.isNotBlank() } ?: "Bill"
            val amt = "₹" + String.format(java.util.Locale.US, "%,.0f", r.amount)
            val whenTxt = when (d) { 0 -> "due today"; 1 -> "due tomorrow"; else -> "due in $d days" }
            b.setSmallIcon(R.drawable.ic_stat_zelu)
                .setContentTitle("$who $amt $whenTxt")
                .setContentText(if (r.minAmount != null) "Minimum due ₹" + String.format(java.util.Locale.US, "%,.0f", r.minAmount) else "Tap to open Dues")
                .setAutoCancel(true)
                .setContentIntent(open)
                .addAction(Notification.Action.Builder(null, "Mark paid", pay).build())
            try { nm.notify(nid, b.build()) } catch (_: Exception) {}
        }
    }
}

class DueReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try { Recap.maybe(context) } catch (_: Exception) {}
        try { Backup.maybeAuto(context) } catch (_: Exception) {}
        try { ZeluWidget.refresh(context) } catch (_: Exception) {}
        try { DueReminders.check(context) } catch (_: Exception) {}
    }
}

class MarkPaidReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("id") ?: return
        try {
            Storage.setReminderDone(context, id, true)
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(intent.getIntExtra("nid", 0))
        } catch (_: Exception) {}
    }
}

// Alarms are cleared on reboot — put the daily check back.
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            DueReminders.schedule(context)
            DailySummary.schedule(context)
        }
    }
}
