package com.nagesh.zelu

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.widget.RemoteViews

// Home-screen widget: this month's spending, and what is left of the budget. It follows the theme
// you picked in the app and updates whenever a new bank SMS arrives.
class ZeluWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) {
            try { manager.updateAppWidget(id, build(context)) } catch (_: Exception) {}
        }
    }

    companion object {
        fun refresh(context: Context) {
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
            val rows = Storage.loadRows(context)
            val spent = Analytics.categoryTotals(rows, month, "All").values.sum()
            val budget = Storage.loadMonthlyBudget(context)
            val today = Dates.today()
            val todaySpent = rows.filter { it.kind == "expense" && it.date == today }.sumOf { it.amount }
            Privacy.load(context)
            fun inr(v: Double) = if (Privacy.hideAll) Privacy.MASK else "₹" + String.format(java.util.Locale.US, "%,.0f", v)

            val v = RemoteViews(context.packageName, R.layout.widget_zelu)
            v.setInt(R.id.w_root, "setBackgroundColor", set.card.toInt())
            v.setTextViewText(R.id.w_label, Dates.monthLong(month) + " spent")
            v.setTextColor(R.id.w_label, set.muted.toInt())
            v.setTextViewText(R.id.w_amount, inr(spent))
            v.setTextColor(R.id.w_amount, set.ink.toInt())
            if (budget > 0) {
                val left = budget - spent
                v.setTextViewText(R.id.w_sub, if (left >= 0) inr(left) + " left of " + inr(budget) else inr(-left) + " over " + inr(budget))
                v.setTextColor(R.id.w_sub, (if (left >= 0) set.income else set.warn).toInt())
            } else {
                v.setTextViewText(R.id.w_sub, if (todaySpent > 0) "Today " + inr(todaySpent) else "Nothing spent today")
                v.setTextColor(R.id.w_sub, set.muted.toInt())
            }
            val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            if (launch != null) {
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
                v.setOnClickPendingIntent(R.id.w_root, PendingIntent.getActivity(context, 3, launch, flags))
            }
            return v
        }
    }
}
