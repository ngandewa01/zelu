package com.koolicream.zelu

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

// Sunday note: the last 7 days against the 7 before, and where the money went. Quiet weeks post nothing.
object WeeklySummary {
    private const val CHANNEL = "zelu_weekly"
    private const val ID = 9300

    fun schedule(context: Context) {
        if (context is DemoContext) return  // sample data never changes the real reminders
        try {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!NotifyPrefs.weeklyOn(context)) { am.cancel(pending(context)); return }
            val mins = NotifyPrefs.weeklyAt(context)
            val at = Calendar.getInstance(Dates.IST).apply {
                set(Calendar.HOUR_OF_DAY, mins / 60); set(Calendar.MINUTE, mins % 60); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_MONTH, 1)
            }
            // Fires daily; it only posts on Sundays.
            am.setInexactRepeating(AlarmManager.RTC, at.timeInMillis, AlarmManager.INTERVAL_DAY, pending(context))
        } catch (_: Exception) {
        }
    }

    private fun pending(context: Context): PendingIntent {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        return PendingIntent.getBroadcast(context, 7003, Intent(context, WeeklyReceiver::class.java), flags)
    }

    private fun weekTotals(rows: List<Row>, endDate: String): Map<String, Double> {
        val m = HashMap<String, Double>()
        for (i in 0 until 7) Analytics.categoryTotals(rows, Dates.shiftDays(endDate, -i), "All").forEach { (k, v) -> m[k] = (m[k] ?: 0.0) + v }
        return m
    }

    fun notifyWeek(context: Context) {
        if (!NotifyPrefs.weeklyOn(context)) return
        if (Calendar.getInstance(Dates.IST).get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) return
        val today = Dates.today()
        val sp = context.getSharedPreferences("zelu_weekly", Context.MODE_PRIVATE)
        if (sp.getString("last", "") == today) return

        val rows = Storage.loadRows(context)
        val thisWeek = weekTotals(rows, today)
        val spent = thisWeek.values.sum()
        if (spent <= 0) return
        val before = weekTotals(rows, Dates.shiftDays(today, -7)).values.sum()
        sp.edit().putString("last", today).apply()

        fun inr(v: Double) = "₹" + String.format(java.util.Locale.US, "%,.0f", v)
        val trend = if (before > 0) {
            val d = Math.round((spent - before) / before * 100).toInt()
            if (d > 0) "$d% more than the week before" else if (d < 0) "${-d}% less than the week before" else "Same as the week before"
        } else ""
        val top = thisWeek.entries.filter { it.value > 0 }.sortedByDescending { it.value }.take(3)
            .joinToString(" · ") { it.key + " " + inr(it.value) }
        val text = listOf(trend, top).filter { it.isNotBlank() }.joinToString("\n")

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Weekly summary", NotificationManager.IMPORTANCE_DEFAULT))
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val b = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(context, CHANNEL) else @Suppress("DEPRECATION") Notification.Builder(context)
        b.setSmallIcon(R.drawable.ic_stat_zelu)
            .setContentTitle(inr(spent) + " spent this week")
            .setContentText(trend.ifBlank { top })
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setAutoCancel(true)
        if (launch != null) {
            val open = PendingIntent.getActivity(context, 6, launch, flags)
            b.setContentIntent(open).addAction(Notification.Action.Builder(null, "Review", open).build())
        }
        try { nm.notify(ID, b.build()) } catch (_: Exception) {}
    }
}

class WeeklyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try { WeeklySummary.notifyWeek(context) } catch (_: Exception) {}
    }
}
