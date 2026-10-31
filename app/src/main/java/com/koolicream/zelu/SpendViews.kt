package com.koolicream.zelu

import androidx.compose.ui.res.stringResource

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
    monthlyBudget: Double = 0.0, onEditMonthly: () -> Unit = {}, onSetBudget: (String) -> Unit = {},
    onOpenCategory: (String) -> Unit = {},
    onDrill: (String, List<Row>) -> Unit,
) {
    val cats = Analytics.byCategory(list)
    val total = cats.sumOf { it.second }.coerceAtLeast(1.0)
    Text(stringResource(R.string.where_it_went), color = ZColor.Ink, fontSize = 30.sp, fontWeight = FontWeight.Bold)
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
    if (dayLabel != null) {
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.clip(RoundedCornerShape(16.dp)).background(ZColor.OrangeSoft).clickable(onClickLabel = "Show every day") { onClearDay() }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.showing, dayLabel), color = ZColor.Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            Text("✕", color = ZColor.Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Unspoken)
        }
    }
    tabs()
    slot()
    if (cats.isEmpty()) { Panel { MutedText(stringResource(R.string.no_spending_in_this_view)) }; return }
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

        // One line on how the month compares, then each category as a chip. The full list, with budgets, is in Categories.
        val prevTotal = prevByCat.values.sum()
        val compare = prevTotal > 0 && dayLabel == null
        val ch = if (compare) ((total - prevTotal) / prevTotal * 100).toInt() else 0
        if (ch != 0) {
            Spacer(Modifier.height(10.dp))
            Text((if (ch > 0) "▲ " else "▼ ") + kotlin.math.abs(ch) + "% vs " + prevLabel, color = if (ch > 0) ZColor.InkSoft else ZColor.Income,
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(12.dp))
        CategoryChips(groups.map { Triple(it.first, it.third, (it.second / total * 100).toInt()) }) { name -> onDrill(name, rowsForGroup(name)) }
    }

    if (showSummary) afterSummary()
    if (!showCategories) return
    val top = cats.first().second
    Panel {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Eyebrow(stringResource(R.string.by_category))
            Spacer(Modifier.weight(1f))
            // The month's budget lives here, next to what was spent, and is set from here.
            if (dayLabel == null) Text(
                if (monthlyBudget > 0) stringResource(R.string.budget_amount, money(monthlyBudget)) else stringResource(R.string.set_monthly_budget),
                color = ZColor.Accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onEditMonthly() }.padding(horizontal = 6.dp, vertical = 4.dp))
        }
        cats.forEachIndexed { i, (c, v) ->
            Spacer(Modifier.height(if (i == 0) 10.dp else 16.dp))
            // A day picked on the calendar keeps the short list of that day's payments; otherwise a category is a page of its own.
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { if (dayLabel == null) onOpenCategory(c) else onDrill(c, rowsFor(c)) }) {
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
                    // One button at the end of the row: sort what nothing could, or set (and change) this category's budget.
                    if (dayLabel == null) {
                        Spacer(Modifier.weight(1f))
                        val label = when {
                            c == "Unknown" -> stringResource(R.string.categorise_action)
                            budget != null && budget > 0 -> stringResource(R.string.budget_action)
                            else -> stringResource(R.string.set_budget_action)
                        }
                        Text(label, color = ZColor.Accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                .clickable { if (c == "Unknown") onDrill(c, rowsFor(c)) else onSetBudget(c) }
                                .padding(horizontal = 6.dp, vertical = 3.dp))
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

@Composable internal fun WeekdayPanel(list: List<Row>, from: String, to: String, note: String? = null, onOpen: (String, List<Row>) -> Unit = { _, _ -> }) {
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
        Eyebrow(stringResource(R.string.weekday_pattern))
        Spacer(Modifier.height(4.dp))
        Text(names[top] + "s are your heaviest day", color = ZColor.Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        MutedText(money(avg[top]) + " on average", 12)
        if (note != null) MutedText(note, 11)
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
            avg.forEachIndexed { i, v ->
                Column(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable { onOpen(names[i], PatternMath.forWeekday(list.filter { it.date >= from && it.date <= to }, i)) },
                    horizontalAlignment = Alignment.CenterHorizontally) {
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

