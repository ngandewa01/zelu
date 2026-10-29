package com.koolicream.zelu

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

// Daily 9 AM (IST) background check: notifies 3 days before, 1 day before and on the
// due day of every unpaid bill / card due, with a "Mark paid" button.
object DueReminders {
    private const val CHANNEL = "zelu_dues"

    fun schedule(context: Context) {
        if (context is DemoContext) return  // sample data never changes the real reminders
        try {
            Alarms.setDaily(context, 9 * 60, pending(context))
        } catch (e: Exception) {
            CrashLog.warn("Schedule due reminders", e)
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
        val debits = Storage.loadRows(context).filter { it.direction == Dir.DEBIT }
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
            val amt = money(r.amount)
            val whenTxt = when (d) { 0 -> "due today"; 1 -> "due tomorrow"; else -> "due in $d days" }
            b.setSmallIcon(R.drawable.ic_stat_zelu)
                .setContentTitle("$who $amt $whenTxt")
                .setContentText(if (r.minAmount != null) "Minimum due " + money(r.minAmount) else "Tap to open Dues")
                .setAutoCancel(true)
                .setContentIntent(open)
                .addAction(Notification.Action.Builder(null, "Mark paid", pay).build())
            try { nm.notify(nid, b.build()) } catch (e: Exception) { CrashLog.warn("DueReminders.check", e) }
        }
    }
}

class DueReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = inBackground {
        DueReminders.schedule(context)  // tomorrow's
        // Bills due are posted here, while the alarm still holds the phone awake, so they come on time. The slow rest
        // (the encrypted weekly backup, the monthly recap, the widget) is background work that may wait for a moment
        // the phone is not dozing.
        try { DueReminders.check(context) } catch (e: Exception) { CrashLog.warn("Due reminders", e) }
        try { DailyWork.enqueue(context) } catch (e: Exception) { CrashLog.warn("Queue daily work", e) }
    }
}

// The daily check's slow parts, run by WorkManager. It keeps the work through a restart and retries it, and never
// runs two at once.
class DailyWork(context: Context, params: androidx.work.WorkerParameters) : androidx.work.CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val c = applicationContext
        try { Recap.maybe(c) } catch (e: Exception) { CrashLog.warn("Recap", e) }
        try { Backup.maybeAuto(c) } catch (e: Exception) { CrashLog.warn("Weekly backup", e) }
        try { ZeluWidget.refresh(c) } catch (e: Exception) { CrashLog.warn("Widget", e) }
        return Result.success()
    }

    companion object {
        fun enqueue(context: Context) {
            androidx.work.WorkManager.getInstance(context).enqueueUniqueWork("zelu-daily", androidx.work.ExistingWorkPolicy.KEEP,
                androidx.work.OneTimeWorkRequestBuilder<DailyWork>().build())
        }
    }
}

class MarkPaidReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("id") ?: return
        val nid = intent.getIntExtra("nid", 0)
        inBackground {
            Storage.setReminderDone(context, id, true)
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(nid)
        }
    }
}

// Alarms are cleared on reboot — put the daily check back.
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            DueReminders.schedule(context)
            DailySummary.schedule(context)
            WeeklySummary.schedule(context)
        }
    }
}
