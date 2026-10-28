package com.koolicream.zelu

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Build
import android.view.View
import android.widget.RemoteViews

// Home-screen widget: this month's spending against last month at the same point, a small chart of both, today's
// spend and the top category, the budget left and the next bill due. It follows the theme you picked in the app and
// updates whenever a new bank SMS arrives.
class ZeluWidget : AppWidgetProvider() {

    // Building the widget reads every payment, so it is done off the main thread.
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = inBackground {
        val views = build(context)
        for (id in ids) {
            try { manager.updateAppWidget(id, views) } catch (_: Exception) {}
        }
    }

    companion object {
        fun refresh(context: Context) {
            if (context is DemoContext) return  // the widget shows your own data, never the sample
            try {
                val mgr = AppWidgetManager.getInstance(context)
                val ids = mgr.getAppWidgetIds(ComponentName(context, ZeluWidget::class.java))
                if (ids.isEmpty()) return
                val views = build(context)
                for (id in ids) mgr.updateAppWidget(id, views)
            } catch (_: Exception) {}
        }

        private fun build(context: Context): RemoteViews {
            val theme = context.getSharedPreferences("zelu_theme", Context.MODE_PRIVATE)
            val pal = PALETTES[theme.getInt("palette", 0).coerceIn(0, PALETTES.size - 1)]
            val set = if (theme.getBoolean("dark", false)) pal.dark else pal.light

            val month = Dates.todayMonth()
            val prevMonth = Dates.shiftMonth(month, -1)
            val today = Dates.today()
            val day = today.substring(8, 10).toInt()
            val rows = Storage.loadRows(context)
            // Same totals as the app's screens: expenses less refunds, transfers excluded.
            val cats = Analytics.categoryTotals(rows, month, "All")
            val spent = cats.values.sum()
            val todaySpent = Analytics.categoryTotals(rows, today, "All").values.sum()
            val budget = Storage.loadMonthlyBudget(context)

            // Running totals by day, this month and last, for the chart and the comparison.
            fun running(m: String): DoubleArray {
                val d = Analytics.dailyTotals(rows, m, "All")
                for (i in 1 until d.size) d[i] += d[i - 1]
                return d
            }
            val cur = running(month)
            val prev = running(prevMonth)
            val prevSameDay = prev[day.coerceAtMost(prev.size - 1)]

            val v = RemoteViews(context.packageName, R.layout.widget_zelu)
            v.setInt(R.id.w_root, "setBackgroundColor", set.card.toInt())
            v.setImageViewBitmap(R.id.w_logo, logo(set.accent.toInt(), set.accentInk.toInt()))

            v.setTextViewText(R.id.w_label, Dates.monthLong(month))
            v.setTextColor(R.id.w_label, set.muted.toInt())
            if (prevSameDay > 0 && cur[day] > 0) {
                val pct = Math.round((cur[day] - prevSameDay) / prevSameDay * 100).toInt()
                val prevName = Dates.monthLong(prevMonth).take(3)
                v.setTextViewText(R.id.w_delta, (if (pct > 0) "↑ $pct%" else "↓ ${-pct}%") + " vs $prevName")
                v.setTextColor(R.id.w_delta, (if (pct > 0) set.warn else set.income).toInt())
                v.setViewVisibility(R.id.w_delta, View.VISIBLE)
            } else v.setViewVisibility(R.id.w_delta, View.GONE)

            v.setTextViewText(R.id.w_amount, money(spent))
            v.setTextColor(R.id.w_amount, set.ink.toInt())
            v.setImageViewBitmap(R.id.w_chart, chart(cur, prev, day, budget, set))

            // Line 1: today, and the budget left or the top category.
            val top = cats.filter { it.value > 0 }.maxByOrNull { it.value }
            val second = when {
                budget > 0 && spent <= budget -> money(budget - spent) + " left of " + money(budget)
                budget > 0 -> money(spent - budget) + " over budget"
                top != null -> "Most on " + top.key + " " + money(top.value)
                else -> ""
            }
            v.setTextViewText(R.id.w_line1, listOf("Today " + money(todaySpent), second).filter { it.isNotBlank() }.joinToString("  ·  "))
            v.setTextColor(R.id.w_line1, set.ink.toInt())

            // Line 2: the next bill not yet paid.
            val next = nextDue(context, rows, today)
            if (next != null) {
                val name = next.counterparty?.takeIf { it.isNotBlank() } ?: next.account
                v.setTextViewText(R.id.w_line2, "Next due  " + name + "  " + money(next.amount) + "  ·  " + shortDate(next.dueDate))
                v.setTextColor(R.id.w_line2, (if (next.dueDate <= Dates.shiftDays(today, 3)) set.warn else set.muted).toInt())
                v.setViewVisibility(R.id.w_line2, View.VISIBLE)
            } else v.setViewVisibility(R.id.w_line2, View.GONE)

            val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            if (launch != null) {
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
                v.setOnClickPendingIntent(R.id.w_root, PendingIntent.getActivity(context, 3, launch, flags))
            }
            return v
        }

        /** The earliest unpaid bill due today or later, judged the same way as the Dues screen. */
        private fun nextDue(context: Context, rows: List<Row>, today: String): Reminder? {
            val reminders = Storage.loadReminders(context)
            val state = Storage.loadReminderState(context)
            val auto = autoSettledReminders(reminders, rows)
            return reminders.filter { r -> r.dueDate >= today && !(state[r.id] ?: (r.id in auto)) && r.amount > 0 }
                .minByOrNull { it.dueDate }
        }

        private fun shortDate(ymd: String): String = try {
            val d = ymd.substring(8, 10).toInt()
            "$d " + Dates.monthLong(ymd.take(7)).take(3)
        } catch (_: Exception) { ymd }

        /** Zelu's mark: the "Z" on the accent colour, as in the app icon. */
        private fun logo(bg: Int, fg: Int): Bitmap {
            val s = 60
            val b = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
            val c = Canvas(b)
            c.drawRoundRect(RectF(0f, 0f, s.toFloat(), s.toFloat()), 16f, 16f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bg })
            // The launcher icon's path, drawn in a 24-unit box scaled to the tile.
            val k = s / 24f * 0.78f
            val o = (s - 24 * k) / 2
            fun p(x: Float, y: Float) = Pair(o + x * k, o + y * k)
            val pts = listOf(p(5f, 4f), p(19f, 4f), p(19f, 7f), p(9.4f, 17f), p(19f, 17f), p(19f, 20f), p(5f, 20f), p(5f, 17f), p(14.6f, 7f), p(5f, 7f))
            val path = Path().apply { moveTo(pts[0].first, pts[0].second); pts.drop(1).forEach { lineTo(it.first, it.second) }; close() }
            c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fg })
            return b
        }

        /** Running spend this month (solid, up to today) over last month (dashed), and the budget as a line. */
        private fun chart(cur: DoubleArray, prev: DoubleArray, day: Int, budget: Double, set: PalSet): Bitmap {
            val w = 720; val h = 150; val pad = 8f
            val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val c = Canvas(b)
            val days = (cur.size - 1).coerceAtLeast(28)
            val top = maxOf(cur.getOrElse(day) { 0.0 }, prev.last(), budget, 1.0) * 1.08
            fun x(d: Int) = pad + (w - 2 * pad) * (d - 1) / (days - 1).toFloat()
            fun y(v: Double) = (h - pad - (h - 2 * pad) * (v / top)).toFloat()

            // Base line.
            c.drawLine(pad, h - pad, w - pad, h - pad, Paint().apply { color = set.line.toInt(); strokeWidth = 3f })
            if (budget > 0) c.drawLine(pad, y(budget), w - pad, y(budget), Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = set.warn.toInt(); alpha = 140; strokeWidth = 3f; pathEffect = DashPathEffect(floatArrayOf(6f, 10f), 0f)
            })
            if (prev.size > 2) {
                val pp = Path().apply { moveTo(x(1), y(prev[1])); for (d in 2 until prev.size) lineTo(x(d.coerceAtMost(days)), y(prev[d])) }
                c.drawPath(pp, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = set.muted.toInt(); alpha = 150; style = Paint.Style.STROKE; strokeWidth = 5f
                    pathEffect = DashPathEffect(floatArrayOf(14f, 12f), 0f); strokeCap = Paint.Cap.ROUND
                })
            }
            val last = day.coerceAtMost(cur.size - 1)
            if (last >= 1) {
                val line = Path().apply { moveTo(x(1), y(cur[1])); for (d in 2..last) lineTo(x(d), y(cur[d])) }
                val fill = Path(line).apply { lineTo(x(last), h - pad); lineTo(x(1), h - pad); close() }
                c.drawPath(fill, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = set.accent.toInt(); alpha = 45 })
                c.drawPath(line, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = set.accent.toInt(); style = Paint.Style.STROKE; strokeWidth = 7f; strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND
                })
                c.drawCircle(x(last), y(cur[last]), 11f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = set.accent.toInt() })
                c.drawCircle(x(last), y(cur[last]), 5f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = set.card.toInt() })
            }
            return b
        }
    }
}
