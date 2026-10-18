package com.nagesh.zelu

import android.content.Context

// What Zelu may notify you about, and when. Read by the alert code and the Notifications page.
object NotifyPrefs {
    private fun sp(c: Context) = c.getSharedPreferences("zelu_notify", Context.MODE_PRIVATE)

    fun spend(c: Context) = sp(c).getBoolean("spend", true)
    fun setSpend(c: Context, on: Boolean) = sp(c).edit().putBoolean("spend", on).apply()

    fun dailyOn(c: Context) = sp(c).getBoolean("daily", true)
    fun setDailyOn(c: Context, on: Boolean) { sp(c).edit().putBoolean("daily", on).apply(); DailySummary.schedule(c) }
    // Minutes after midnight (IST). 22:00 until you change it.
    fun dailyAt(c: Context) = sp(c).getInt("daily_at", 22 * 60)
    fun setDailyAt(c: Context, minutes: Int) { sp(c).edit().putInt("daily_at", minutes).apply(); DailySummary.schedule(c) }

    fun weeklyOn(c: Context) = sp(c).getBoolean("weekly", true)
    fun setWeeklyOn(c: Context, on: Boolean) { sp(c).edit().putBoolean("weekly", on).apply(); WeeklySummary.schedule(c) }
    // Sundays, at this time (minutes after midnight IST). 11:00 until you change it.
    fun weeklyAt(c: Context) = sp(c).getInt("weekly_at", 11 * 60)
    fun setWeeklyAt(c: Context, minutes: Int) { sp(c).edit().putInt("weekly_at", minutes).apply(); WeeklySummary.schedule(c) }

    fun duesOn(c: Context) = sp(c).getBoolean("dues", true)
    fun setDuesOn(c: Context, on: Boolean) = sp(c).edit().putBoolean("dues", on).apply()

    // How many days before a due date to remind. The presets are what the Notifications page cycles through.
    val LEAD_PRESETS: List<Pair<String, List<Int>>> = listOf(
        "3 days, 1 day and on the day" to listOf(3, 1, 0),
        "2 days before and on the day" to listOf(2, 0),
        "Only on the day" to listOf(0),
        "A week, 3 days, 1 day and on the day" to listOf(7, 3, 1, 0),
    )
    fun leadIndex(c: Context) = sp(c).getInt("lead", 0).coerceIn(0, LEAD_PRESETS.size - 1)
    fun setLeadIndex(c: Context, i: Int) = sp(c).edit().putInt("lead", i).apply()
    fun leadDays(c: Context): List<Int> = LEAD_PRESETS[leadIndex(c)].second

    fun recapOn(c: Context) = sp(c).getBoolean("recap", true)
    fun setRecapOn(c: Context, on: Boolean) = sp(c).edit().putBoolean("recap", on).apply()

    fun timeText(minutes: Int): String {
        val h = minutes / 60; val m = minutes % 60
        return String.format(java.util.Locale.US, "%d:%02d %s", if (h % 12 == 0) 12 else h % 12, m, if (h < 12) "AM" else "PM")
    }
}
