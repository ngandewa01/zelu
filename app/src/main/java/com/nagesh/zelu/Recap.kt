package com.nagesh.zelu

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build

// On the first days of a month, one quiet notification that sums up the month that just ended.
object Recap {
    private const val CHANNEL = "zelu_recap"

    fun maybe(context: Context) {
        val month = Dates.todayMonth()
        val sp = context.getSharedPreferences("zelu_recap", Context.MODE_PRIVATE)
        if (Dates.dayOfMonth() > 3 || sp.getString("last", "") == month) return
        val prev = Dates.shiftMonth(month, -1)
        val rows = Storage.loadRows(context)
        val totals = Analytics.categoryTotals(rows, prev, "All")
        val spent = totals.values.sum()
        val income = rows.filter { it.kind == "income" && it.date.startsWith(prev) }.sumOf { it.amount }
        if (spent <= 0) return
        sp.edit().putString("last", month).apply()

        fun inr(v: Double) = "₹" + String.format(java.util.Locale.US, "%,.0f", v)
        val before = Analytics.categoryTotals(rows, Dates.shiftMonth(prev, -1), "All").values.sum()
        val top = totals.entries.filter { it.value > 0 }.sortedByDescending { it.value }.take(2)
            .joinToString(" · ") { it.key + " " + inr(it.value) }
        val trend = if (before > 0) {
            val d = Math.round((spent - before) / before * 100).toInt()
            if (d > 0) "$d% more than the month before" else if (d < 0) "${-d}% less than the month before" else "Same as the month before"
        } else ""
        val kept = if (income > 0) (if (income >= spent) "Kept " + inr(income - spent) + " of " + inr(income) else "Spent " + inr(spent - income) + " more than came in") else ""
        val text = listOf(top, trend, kept).filter { it.isNotBlank() }.joinToString("\n")

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Monthly recap", NotificationManager.IMPORTANCE_LOW))
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val b = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(context, CHANNEL) else @Suppress("DEPRECATION") Notification.Builder(context)
        b.setSmallIcon(R.drawable.ic_stat_zelu)
            .setContentTitle(Dates.monthLong(prev) + ": you spent " + inr(spent))
            .setContentText(text.substringBefore('\n'))
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setAutoCancel(true)
        if (launch != null) b.setContentIntent(PendingIntent.getActivity(context, 4, launch, flags))
        try { nm.notify(9100, b.build()) } catch (_: Exception) {}
    }
}
