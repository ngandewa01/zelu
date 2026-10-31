package com.koolicream.zelu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// The top of Spend: what the month cost, how that sits against a usual month, and twelve months as bars. Tapping a bar
// switches the whole page to that month, so there is no separate Trends page.
@Composable internal fun MonthCard(spendRows: List<Row>, month: String, total: Double, budget: Double, onPickMonth: (String) -> Unit) {
    val months = remember(month) { Analytics.monthsUpTo(Dates.todayMonth(), 12) }
    val values = remember(spendRows, months) { Analytics.monthlyTotals(spendRows, months) }
    val sel = months.indexOf(month)
    val others = values.filterIndexed { i, v -> i != sel && v > 0 }
    val usual = PatternMath.median(others)
    Column(Modifier.fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(22.dp)).background(ZColor.Card)
        .padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { Eyebrow(stringResource(R.string.month_spent, Dates.monthLong(month))) }
            if (usual > 0 && total > 0) {
                val d = Math.round((total - usual) / usual * 100).toInt()
                if (d != 0) Box(Modifier.clip(RoundedCornerShape(10.dp)).background(if (d > 0) ZColor.WarnSoft else ZColor.IncomeSoft)
                    .padding(horizontal = 9.dp, vertical = 4.dp)) {
                    Text((if (d > 0) "▲ " else "▼ ") + kotlin.math.abs(d) + "% " + stringResource(R.string.vs_usual),
                        color = if (d > 0) ZColor.Warn else ZColor.Income, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
        Text(money(total), color = ZColor.Ink, fontSize = 34.sp, fontWeight = FontWeight.Bold)
        if (usual > 0) MutedText(stringResource(R.string.usual_month_is, money(usual)), 13)
        Spacer(Modifier.height(10.dp))
        MonthChart(months, values, ZColor.Accent, sel, { onPickMonth(months[it]) }, budget, fit = true)
        if (usual > 0) MutedText(stringResource(R.string.usual_line_legend), 11)
    }
}

// One pattern: a short headline for its small card, and bars that each list their own payments.
internal class Pattern(val title: String, val short: String, val head: String, val sub: String, val labels: List<String>, val values: List<Double>,
                       val top: Int, val rowsFor: (Int) -> List<Row>)

// Three small cards in a row; tapping one opens its chart below, and tapping a bar there lists just those payments.
@Composable internal fun PatternsBlock(patterns: List<Pattern>, onRowClick: (Row) -> Unit) {
    if (patterns.isEmpty()) return
    var open by remember { mutableStateOf<Int?>(null) }
    var pick by remember { mutableStateOf<Int?>(null) }
    Spacer(Modifier.height(16.dp))
    Eyebrow(stringResource(R.string.patterns_title))
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        patterns.forEachIndexed { i, p ->
            val on = open == i
            Column(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(ZColor.Card)
                .border(if (on) 1.5.dp else 1.dp, if (on) ZColor.Accent else ZColor.Line, RoundedCornerShape(18.dp))
                .clickable { if (on) open = null else { open = i; pick = null } }.padding(10.dp)) {
                Text(p.short.uppercase(), color = ZColor.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Text(p.head, color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(p.sub, color = ZColor.Muted, fontSize = 11.sp, maxLines = 2, lineHeight = 13.sp)
                Spacer(Modifier.height(6.dp))
                MiniBars(p.values, p.top, 26)
            }
        }
    }
    val o = open ?: return
    val p = patterns.getOrNull(o) ?: return
    val chosen = pick ?: p.top
    Spacer(Modifier.height(10.dp))
    Panel {
        Eyebrow(p.title)
        Spacer(Modifier.height(10.dp))
        val mx = (p.values.maxOrNull() ?: 0.0).coerceAtLeast(1.0)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
            p.values.forEachIndexed { i, v ->
                Column(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable { pick = i }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("₹" + shortAmt(v), color = ZColor.Muted, fontSize = 10.sp, maxLines = 1, softWrap = false)
                    Spacer(Modifier.height(3.dp))
                    Box(Modifier.height(60.dp).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                        Box(Modifier.fillMaxWidth().height((60 * (v / mx)).coerceAtLeast(3.0).dp).clip(RoundedCornerShape(6.dp))
                            .background(if (i == chosen) ZColor.Accent else ZColor.Line))
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(p.labels[i], color = if (i == chosen) ZColor.Ink else ZColor.Muted, fontSize = 10.sp,
                        fontWeight = if (i == chosen) FontWeight.Bold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        val list = p.rowsFor(chosen).sortedByDescending { it.paise }
        Spacer(Modifier.height(12.dp))
        MutedText(stringResource(R.string.pattern_list_head, p.labels[chosen], list.size, money(list.rupees())), 12)
        list.take(30).forEach { TxnRow(it, false, onRowClick) }
    }
}

@Composable private fun MiniBars(values: List<Double>, top: Int, height: Int) {
    val mx = (values.maxOrNull() ?: 0.0).coerceAtLeast(1.0)
    Row(Modifier.fillMaxWidth().height(height.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
        values.forEachIndexed { i, v ->
            Box(Modifier.weight(1f).height((height * (v / mx)).coerceAtLeast(2.0).dp).clip(RoundedCornerShape(3.dp))
                .background(if (i == top) ZColor.Accent else ZColor.Line))
        }
    }
}

// The three patterns from everyday spending (EMI and rent left out): which weekday, which part of the day, and small payments.
@Composable internal fun spendPatterns(rows: List<Row>, from: String, to: String): List<Pattern> {
    val out = ArrayList<Pattern>()
    val spend = rows.filter { it.kind == Kind.EXPENSE && it.date >= from && it.date <= to }
    if (spend.isEmpty()) return out
    val dayShort = listOf(R.string.d_mon, R.string.d_tue, R.string.d_wed, R.string.d_thu, R.string.d_fri, R.string.d_sat, R.string.d_sun).map { stringResource(it) }
    val dayLong = listOf(R.string.dp_mon, R.string.dp_tue, R.string.dp_wed, R.string.dp_thu, R.string.dp_fri, R.string.dp_sat, R.string.dp_sun).map { stringResource(it) }
    val avg = Analytics.weekdayAverages(spend, from, to).toList()
    if (avg.any { it > 0 }) {
        val t = avg.indices.maxByOrNull { avg[it] } ?: 0
        out.add(Pattern(stringResource(R.string.weekday_pattern), stringResource(R.string.weekday_short), dayLong[t], stringResource(R.string.heaviest_day_sub, money(avg[t])), dayShort, avg, t) { i -> PatternMath.forWeekday(spend, i) })
    }
    val tod = PatternMath.timeOfDay(spend).toList()
    if (tod.any { it > 0 }) {
        val t = tod.indices.maxByOrNull { tod[it] } ?: 0
        val names = listOf(R.string.tod_morning, R.string.tod_afternoon, R.string.tod_evening, R.string.tod_night).map { stringResource(it) }
        out.add(Pattern(stringResource(R.string.time_of_day), stringResource(R.string.time_of_day), names[t], stringResource(R.string.busiest_time_sub, money(tod[t])), names, tod, t) { i -> PatternMath.forPartOfDay(spend, i) })
    }
    val small = spend.filter { it.paise < PatternMath.SMALL_LIMIT * 100 }
    if (small.isNotEmpty()) {
        val byDay = (0..6).map { i -> PatternMath.forWeekday(small, i).rupees() }
        val t = byDay.indices.maxByOrNull { byDay[it] } ?: 0
        out.add(Pattern(stringResource(R.string.small_spends), stringResource(R.string.small_spends), stringResource(R.string.small_card_head, small.size),
            stringResource(R.string.small_card_sub, money(PatternMath.SMALL_LIMIT), money(small.rupees())), dayShort, byDay, t) { i -> PatternMath.forWeekday(small, i) })
    }
    return out
}
