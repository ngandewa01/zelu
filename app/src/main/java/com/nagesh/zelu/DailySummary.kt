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

// Nightly (10 PM IST unless you pick another time) "₹8,091 spent today · ₹62,279 in October" notification.
// Quiet days with no spending post nothing.
object DailySummary {
    private const val CHANNEL = "zelu_daily"
    private const val ID = 9200

    fun schedule(context: Context) {
        try {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!NotifyPrefs.dailyOn(context)) { am.cancel(pending(context)); return }
            val mins = NotifyPrefs.dailyAt(context)
            val at = Calendar.getInstance(Dates.IST).apply {
                set(Calendar.HOUR_OF_DAY, mins / 60); set(Calendar.MINUTE, mins % 60); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_MONTH, 1)
            }
            am.setInexactRepeating(AlarmManager.RTC, at.timeInMillis, AlarmManager.INTERVAL_DAY, pending(context))
        } catch (_: Exception) {
        }
    }

    private fun pending(context: Context): PendingIntent {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        return PendingIntent.getBroadcast(context, 7002, Intent(context, DailyReceiver::class.java), flags)
    }

    fun notifyToday(context: Context) {
        if (!NotifyPrefs.dailyOn(context)) return
        val today = Dates.today()
        // An inexact alarm can fire twice in a day; post once.
        val sp = context.getSharedPreferences("zelu_daily", Context.MODE_PRIVATE)
        if (sp.getString("last", "") == today) return

        val rows = Storage.loadRows(context)
        // Same totals as the app's screens: expenses less refunds, transfers excluded.
        val todayCats = Analytics.categoryTotals(rows, today, "All")
        val spent = todayCats.values.sum()
        if (spent <= 0) return
        val month = Dates.todayMonth()
        val monthSpent = Analytics.categoryTotals(rows, month, "All").values.sum()
        sp.edit().putString("last", today).apply()

        fun inr(v: Double) = "₹" + String.format(java.util.Locale.US, "%,.0f", v)
        val monthLine = inr(monthSpent) + " in " + Dates.monthLong(month)
        val top = todayCats.entries.filter { it.value > 0 }.sortedByDescending { it.value }.take(2)
            .joinToString(" · ") { it.key + " " + inr(it.value) }

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Daily summary", NotificationManager.IMPORTANCE_DEFAULT))
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val b = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(context, CHANNEL) else @Suppress("DEPRECATION") Notification.Builder(context)
        b.setSmallIcon(R.drawable.ic_stat_zelu)
            .setContentTitle(inr(spent) + " spent today")
            .setContentText(monthLine)
            .setStyle(Notification.BigTextStyle().bigText(listOf(monthLine, top).filter { it.isNotBlank() }.joinToString("\n")))
            .setAutoCancel(true)
        if (launch != null) {
            val open = PendingIntent.getActivity(context, 5, launch, flags)
            b.setContentIntent(open).addAction(Notification.Action.Builder(null, "Review", open).build())
        }
        try { nm.notify(ID, b.build()) } catch (_: Exception) {}
    }
}

class DailyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try { DailySummary.notifyToday(context) } catch (_: Exception) {}
    }
}
