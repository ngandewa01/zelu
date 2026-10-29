package com.koolicream.zelu

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable internal fun BucketsView(
    list: List<Row>, prevByCat: Map<String, Double> = emptyMap(),
    budgets: Map<String, Double> = emptyMap(), onEditBudgets: () -> Unit = {},
    onTrends: (() -> Unit)? = null,
    onCalendar: (() -> Unit)? = null, calActive: Boolean = false,
    dayLabel: String? = null, onClearDay: () -> Unit = {},
    slot: @Composable () -> Unit = {},
    afterSummary: @Composable () -> Unit = {},
    tabs: @Composable () -> Unit = {},
    showSummary: Boolean = true,
    showCategories: Boolean = true,
    prevLabel: String = "last month",
    onDrill: (String, List<Row>) -> Unit,
) {
    val cats = Analytics.byCategory(list)
    val total = cats.sumOf { it.second }.coerceAtLeast(1.0)
    Text("Where it went", color = ZColor.Ink, fontSize = 30.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(10.dp))
    // Three slim, equal buttons the height of the tabs: a bright icon on a round tile, then the word. White, so they
    // read as buttons next to the tabs below.
    @Composable fun RowScope.ToolButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, light: Long, dark: Long,
                                        on: Boolean, onClick: () -> Unit) {
        val tint = Color(if (ZTheme.dark) dark else light)
        Row(
            Modifier.weight(1f).clip(RoundedCornerShape(22.dp))
                .background(if (on) tint.copy(alpha = if (ZTheme.dark) 0.22f else 0.14f) else ZColor.Tile)
                .border(if (on) 1.5.dp else 1.dp, if (on) tint else ZColor.Line, RoundedCornerShape(22.dp))
                .clickable { onClick() }.padding(start = 4.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(28.dp).clip(CircleShape).background(ZColor.Card), contentAlignment = Alignment.Center) {
                androidx.compose.material3.Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(7.dp))
            FitText(label, ZColor.Ink, 13f, FontWeight.SemiBold, Modifier.weight(1f, fill = false))
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (onTrends != null) ToolButton(Icons.Rounded.BarChart, "Trends", 0xFF06B6D4, 0xFF22D3EE, false) { onTrends() }
        ToolButton(Icons.Rounded.AccountBalanceWallet, "Budgets", 0xFF8B5CF6, 0xFFA78BFA, false) { onEditBudgets() }
        if (onCalendar != null) ToolButton(Icons.Rounded.CalendarMonth, "Calendar", 0xFFFF6B1A, 0xFFFF8A3D, calActive) { onCalendar() }
    }
    if (dayLabel != null) {
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.clip(RoundedCornerShape(16.dp)).background(ZColor.OrangeSoft).clickable(onClickLabel = "Show every day") { onClearDay() }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Showing $dayLabel", color = ZColor.Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            Text("✕", color = ZColor.Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Unspoken)
        }
    }
    tabs()
    slot()
    if (cats.isEmpty()) { Panel { MutedText("No spending in this view.") }; return }
    fun rowsFor(c: String) = list.filter { it.kind == Kind.EXPENSE && it.category == c }

    // Chart groups: top 6 categories, the rest folded into "Other".
    val topN = cats.take(6)
    val rest = cats.drop(6)
    val groups = topN.map { Triple(it.first, it.second, catColor(it.first)) } +
        (if (rest.isNotEmpty()) listOf(Triple("Other", rest.sumOf { it.second }, ZColor.Muted)) else emptyList())
    fun rowsForGroup(name: String) =
        if (name == "Other") rest.flatMap { rowsFor(it.first) } else rowsFor(name)

    if (showSummary) Panel {
        // The total sits inside the pie; how much and how fast against the budget is on Home.
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Donut(groups.map { it.third to (it.second / total).toFloat() }, money(total),
                if (cats.size == 1) "1 category" else "${cats.size} categories", 150.dp, 22.dp) { i ->
                onDrill(groups[i].first, rowsForGroup(groups[i].first))
            }
        }

        // Each category once: amount, and how it moved against the month before.
        val prevTotal = prevByCat.values.sum()
        val compare = prevTotal > 0 && dayLabel == null
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Eyebrow(if (compare) "Vs $prevLabel" else "By category")
            if (compare) InfoDot("How each category moved against $prevLabel.")
            Spacer(Modifier.weight(1f))
            val ch = if (compare) ((total - prevTotal) / prevTotal * 100).toInt() else 0
            if (ch != 0) Text((if (ch > 0) "▲ " else "▼ ") + kotlin.math.abs(ch) + "%", color = if (ch > 0) ZColor.InkSoft else ZColor.Income,
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        groups.forEach { (name, v, color) ->
            val before = if (name == "Other") rest.sumOf { prevByCat[it.first] ?: 0.0 } else prevByCat[name] ?: 0.0
            val d = v - before
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onDrill(name, rowsForGroup(name)) },
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(color))
                Spacer(Modifier.width(8.dp))
                Text(name, color = ZColor.Ink, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(money(v), color = ZColor.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(" · ${(v / total * 100).toInt()}%", color = ZColor.Muted, fontSize = 12.sp)
                if (compare) Text(
                    when {
                        kotlin.math.abs(d) < 1 -> "same"
                        before <= 0 -> "new"
                        d > 0 -> "▲ " + money(d)
                        else -> "▼ " + money(-d)
                    },
                    // More spending is shown plainly, less in green: a change, not an alarm.
                    color = if (d < -0.5) ZColor.Income else ZColor.Muted, fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End, modifier = Modifier.width(92.dp),
                )
            }
        }
    }

    if (showSummary) afterSummary()
    if (!showCategories) return
    val top = cats.first().second
    Panel {
        Eyebrow("By category")
        cats.forEachIndexed { i, (c, v) ->
            Spacer(Modifier.height(if (i == 0) 10.dp else 16.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onDrill(c, rowsFor(c)) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MerchantTile("", c, 34.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(c, color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(money(v), color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(" ${(v / total * 100).toInt()}%", color = ZColor.Muted, fontSize = 12.sp,
                        modifier = Modifier.width(42.dp), textAlign = TextAlign.End)
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val prev = prevByCat[c]
                    if (dayLabel != null) {
                    } else if (prev != null && prev > 0) {
                        val ch = ((v - prev) / prev * 100).toInt()
                        if (ch > 0) TonePill("▲ $ch% vs last month", ZColor.InkSoft, ZColor.Paper)
                        else if (ch < 0) TonePill("▼ ${kotlin.math.abs(ch)}% vs last month", ZColor.Income, ZColor.IncomeSoft)
                        else TonePill("• same as last month", ZColor.Muted, ZColor.Paper)
                    } else {
                        TonePill("new this month", ZColor.Muted, ZColor.Paper)
                    }
                    val budget = if (dayLabel == null) budgets[c] else null
                    if (budget != null && budget > 0) {
                        if (v > budget) TonePill("Over by " + money(v - budget), ZColor.Warn, ZColor.WarnSoft)
                        else TonePill(money(v) + " of " + money(budget), ZColor.InkSoft, ZColor.Paper)
                    }
                }
                val budget = if (dayLabel == null) budgets[c] else null
                Spacer(Modifier.height(8.dp))
                if (budget != null && budget > 0) Meter((v / budget).toFloat().coerceIn(0f, 1f), if (v > budget) ZColor.Warn else ZColor.Income)
                else Meter((v / top).toFloat(), catColor(c))
            }
        }
    }
}

@Composable internal fun WeekdayPanel(list: List<Row>, from: String, to: String, note: String? = null) {
    if (to < from) return
    var days = 0; var d = from
    while (d <= to && days < 400) { days++; d = Dates.shiftDays(d, 1) }
    if (days < 7) return
    val avg = Analytics.weekdayAverages(list, from, to)
    val mx = avg.maxOrNull() ?: 0.0
    if (mx <= 0.0) return
    val names = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    val letters = listOf("M", "T", "W", "T", "F", "S", "S")
    val top = avg.indices.maxByOrNull { avg[it] } ?: 0
    Panel {
        Eyebrow("Weekday pattern")
        Spacer(Modifier.height(4.dp))
        Text(names[top] + "s are your heaviest day", color = ZColor.Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        MutedText(money(avg[top]) + " on average", 12)
        if (note != null) MutedText(note, 11)
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
            avg.forEachIndexed { i, v ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(shortAmt(v), color = ZColor.Muted, fontSize = 10.sp, maxLines = 1)
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.height(64.dp).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                        Box(Modifier.fillMaxWidth().height((64 * (v / mx)).coerceAtLeast(3.0).dp).clip(RoundedCornerShape(6.dp))
                            .background(if (i == top) ZColor.Accent else ZColor.Line))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(letters[i], color = if (i == top) ZColor.Ink else ZColor.Muted, fontSize = 12.sp,
                        fontWeight = if (i == top) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}

@Composable internal fun RecurringPanel(list: List<Analytics.Recurring>) {
    if (list.isEmpty()) return
    val total = list.sumOf { it.amount }
    Panel {
        Eyebrow("Recurring")
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(money(total), color = ZColor.Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(" a month · ${list.size} payment${if (list.size == 1) "" else "s"}", color = ZColor.Muted, fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 3.dp))
        }
        list.take(6).forEach { r ->
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                MerchantTile(r.name, r.category, 34.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(r.name, color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    MutedText("last on " + r.last.takeLast(5).split("-").let { (mo, dd) -> "$dd/$mo" }, 11)
                }
                if (r.prev != null && r.amount > r.prev * 1.05) {
                    TonePill("▲ " + money(r.amount - r.prev), ZColor.Warn, ZColor.WarnSoft)
                    Spacer(Modifier.width(8.dp))
                }
                Text(money(r.amount), color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (list.size > 6) { Spacer(Modifier.height(10.dp)); MutedText("+ ${list.size - 6} more", 12) }
    }
}
