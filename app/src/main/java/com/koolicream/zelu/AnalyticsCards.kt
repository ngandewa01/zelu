package com.koolicream.zelu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Two doors at the top of Analytics: month by month, and day by day.
@Composable internal fun AnalyticsEntries(onTrends: () -> Unit, onCalendar: (() -> Unit)?, calOpen: Boolean) {
    @Composable fun androidx.compose.foundation.layout.RowScope.Entry(icon: ImageVector, tint: Color, title: String, sub: String, on: Boolean, go: () -> Unit) {
        Row(Modifier.weight(1f).clip(RoundedCornerShape(22.dp)).background(if (on) tint.copy(alpha = 0.16f) else ZColor.Card)
            .clickable { go() }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(ZColor.Tile), contentAlignment = Alignment.Center) {
                androidx.compose.material3.Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = ZColor.Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(sub, color = ZColor.Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Entry(Icons.Rounded.BarChart, Color(0xFF06B6D4), stringResource(R.string.trends_title), stringResource(R.string.trends_sub), false, onTrends)
        if (onCalendar != null) Entry(Icons.Rounded.CalendarMonth, Color(0xFFFF6B1A), stringResource(R.string.calendar_title), stringResource(R.string.calendar_sub), calOpen, onCalendar)
    }
}

@Composable internal fun TimeOfDayPanel(rows: List<Row>) {
    val v = PatternMath.timeOfDay(rows)
    val mx = v.maxOrNull() ?: 0.0
    if (mx <= 0.0) return
    val top = v.indices.maxByOrNull { v[it] } ?: 0
    val names = listOf(R.string.tod_morning, R.string.tod_afternoon, R.string.tod_evening, R.string.tod_night).map { stringResource(it) }
    val spans = listOf(R.string.tod_morning_span, R.string.tod_afternoon_span, R.string.tod_evening_span, R.string.tod_night_span).map { stringResource(it) }
    Panel {
        Eyebrow(stringResource(R.string.time_of_day))
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.busiest_time, names[top].lowercase()), color = ZColor.Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        MutedText(money(v[top]) + " " + spans[top], 12)
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
            v.forEachIndexed { i, x ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(shortAmt(x), color = ZColor.Muted, fontSize = 10.sp, maxLines = 1)
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.height(64.dp).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                        Box(Modifier.fillMaxWidth().height((64 * (x / mx)).coerceAtLeast(3.0).dp).clip(RoundedCornerShape(6.dp))
                            .background(if (i == top) ZColor.Accent else ZColor.Line))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(names[i], color = if (i == top) ZColor.Ink else ZColor.Muted, fontSize = 11.sp,
                        fontWeight = if (i == top) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
                }
            }
        }
    }
}

@Composable internal fun BiggestPanel(rows: List<Row>, onRowClick: (Row) -> Unit) {
    val top = PatternMath.biggest(rows)
    if (top.isEmpty()) return
    Panel {
        Eyebrow(stringResource(R.string.biggest_payments))
        top.forEach { r ->
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onRowClick(r) }, verticalAlignment = Alignment.CenterVertically) {
                MerchantTile(r.counterparty ?: r.category, r.category, 34.dp, kind = r.kind)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(rowTitle(r), color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    MutedText(prettyDate(r.date) + " · " + r.category, 12)
                }
                Text(money(r.paise / 100.0), color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable internal fun MonthThirdsPanel(rows: List<Row>) {
    val v = PatternMath.thirds(rows)
    val total = v.sum()
    if (total <= 0.0) return
    val top = v.indices.maxByOrNull { v[it] } ?: 0
    val heads = listOf(R.string.third_first, R.string.third_middle, R.string.third_last).map { stringResource(it) }
    val spans = listOf(R.string.third_first_span, R.string.third_middle_span, R.string.third_last_span).map { stringResource(it) }
    Panel {
        Eyebrow(stringResource(R.string.through_the_month))
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.most_goes_out, heads[top]), color = ZColor.Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth().height(16.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            v.forEachIndexed { i, x -> if (x > 0) Box(Modifier.weight((x / total).toFloat().coerceAtLeast(0.02f)).height(16.dp).clip(RoundedCornerShape(8.dp))
                .background(if (i == top) ZColor.Accent else ZColor.Line)) }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            v.forEachIndexed { i, x ->
                Column(Modifier.weight(1f)) {
                    MutedText(spans[i], 11)
                    Text(money(x), color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
    }
}

@Composable internal fun SmallSpendsPanel(rows: List<Row>) {
    val s = PatternMath.small(rows)
    if (s.count == 0) return
    val all = rows.filter { it.kind == Kind.EXPENSE }.sumOf { it.paise } / 100.0
    val share = if (all > 0) Math.round(s.total / all * 100).toInt() else 0
    Panel {
        Eyebrow(stringResource(R.string.small_spends))
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.small_count, s.count, money(PatternMath.SMALL_LIMIT)), color = ZColor.Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        MutedText(stringResource(R.string.small_total, money(s.total), share), 12)
        Spacer(Modifier.height(8.dp))
        MutedText(stringResource(R.string.small_average, money(s.average)), 12)
    }
}
