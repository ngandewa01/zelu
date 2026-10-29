package com.koolicream.zelu

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import java.util.Calendar

// The daily checks (dues, daily and weekly summaries). Each is a one-off alarm that its receiver sets again when it
// fires. Unlike the old repeating alarm, it is allowed to run while the phone dozes, so a 9 AM reminder comes at 9,
// not whenever the phone is next picked up. It needs no exact-alarm permission: Android may still shift it by minutes.
object Alarms {
    /** The next time it is [minutes] past midnight in India, at least a minute after [now] (so a firing never repeats). */
    fun nextAt(minutes: Int, now: Long): Long = Calendar.getInstance(Dates.IST).apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, minutes / 60); set(Calendar.MINUTE, minutes % 60); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        if (timeInMillis <= now + 60_000L) add(Calendar.DAY_OF_MONTH, 1)
    }.timeInMillis

    /** Sets [pi] to fire at [minutes] past midnight (India time), replacing any alarm set with it before. */
    fun setDaily(context: Context, minutes: Int, pi: PendingIntent) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val at = nextAt(minutes, System.currentTimeMillis())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        else am.set(AlarmManager.RTC_WAKEUP, at, pi)
    }
}
