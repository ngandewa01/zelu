package com.koolicream.zelu

import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale
import kotlin.math.atan2

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
    onDrill: (String, List<Row>) -> Unit,
) {
    val cats = Analytics.byCategory(list)
    val total = cats.sumOf { it.second }.coerceAtLeast(1.0)
    Text("Where it went", color = ZColor.Ink, fontSize = 30.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (onTrends != null) Row(
            Modifier.clip(RoundedCornerShape(20.dp)).border(1.dp, ZColor.Line, RoundedCornerShape(20.dp))
                .clickable { onTrends() }.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.Icon(Icons.Rounded.BarChart, null, tint = ZColor.Accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Trends", color = ZColor.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Row(
            Modifier.clip(RoundedCornerShape(20.dp)).border(1.dp, ZColor.Line, RoundedCornerShape(20.dp))
                .clickable { onEditBudgets() }.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.Icon(Icons.Rounded.AccountBalanceWallet, null, tint = ZColor.Accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Budgets", color = ZColor.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        if (onCalendar != null) Box(
            Modifier.clip(RoundedCornerShape(20.dp)).background(if (calActive) ZColor.OrangeSoft else Color.Transparent)
                .border(1.dp, if (calActive) ZColor.Accent else ZColor.Line, RoundedCornerShape(20.dp))
                .clickable { onCalendar() }.padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) { androidx.compose.material3.Icon(Icons.Rounded.CalendarMonth, null, tint = ZColor.Accent, modifier = Modifier.size(18.dp)) }
    }
    if (dayLabel != null) {
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.clip(RoundedCornerShape(16.dp)).background(ZColor.OrangeSoft).clickable { onClearDay() }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Showing $dayLabel", color = ZColor.Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            Text("✕", color = ZColor.Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
    tabs()
    slot()
    if (cats.isEmpty()) { Panel { MutedText("No spending in this view.") }; return }
    fun rowsFor(c: String) = list.filter { it.kind == "expense" && it.category == c }

    // Chart groups: top 6 categories, the rest folded into "Other".
    val topN = cats.take(6)
    val rest = cats.drop(6)
    val groups = topN.map { Triple(it.first, it.second, catColor(it.first)) } +
        (if (rest.isNotEmpty()) listOf(Triple("Other", rest.sumOf { it.second }, ZColor.Muted)) else emptyList())
    fun rowsForGroup(name: String) =
        if (name == "Other") rest.flatMap { rowsFor(it.first) } else rowsFor(name)

    if (showSummary) Panel {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Eyebrow("Spent")
                Text(money(total), color = ZColor.Ink, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            }
            val prevTotal = prevByCat.values.sum()
            if (prevTotal > 0 && dayLabel == null) {
                val ch = ((total - prevTotal) / prevTotal * 100).toInt()
                if (ch != 0) TonePill(
                    (if (ch > 0) "▲ " else "▼ ") + kotlin.math.abs(ch) + "% vs last month",
                    if (ch > 0) ZColor.Warn else ZColor.Income,
                    if (ch > 0) ZColor.WarnSoft else ZColor.IncomeSoft,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Donut(groups.map { it.third to (it.second / total).toFloat() }, "${cats.size}", "categories", 128.dp, 20.dp) { i ->
                onDrill(groups[i].first, rowsForGroup(groups[i].first))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                groups.forEach { (name, v, color) ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onDrill(name, rowsForGroup(name)) },
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(color))
                        Spacer(Modifier.width(8.dp))
                        Text(name, color = ZColor.Ink, fontSize = 13.sp, modifier = Modifier.weight(1f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${(v / total * 100).toInt()}%", color = ZColor.Muted, fontSize = 12.sp)
                    }
                }
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
                        if (ch > 0) TonePill("▲ $ch% vs last month", ZColor.Warn, ZColor.WarnSoft)
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

@Composable internal fun SpendNowPanel(spent: Double, budget: Double, daysLeft: Int, dayNum: Int, curSame: Double, prevSame: Double, note: String? = null) {
    Panel {
        Eyebrow("Right now")
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val left = (budget - spent).coerceAtLeast(0.0)
            val over = budget > 0 && spent > budget
            if (budget <= 0) Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(ZColor.Paper).padding(12.dp)) {
                MutedText("Spent so far", 11)
                Text(money(spent), color = ZColor.Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text("No budget set · $daysLeft days to go", color = ZColor.InkSoft, fontSize = 11.sp)
            } else Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(if (over) ZColor.WarnSoft else ZColor.IncomeSoft).padding(12.dp)) {
                MutedText("Daily allowance", 11)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(money(if (daysLeft > 0) left / daysLeft else 0.0), color = if (over) ZColor.Warn else ZColor.Income,
                        fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(" a day", color = ZColor.Muted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 3.dp))
                }
                Spacer(Modifier.height(2.dp))
                Text(if (over) "Budget used up · " + money(spent - budget) + " over" else money(left) + " left · $daysLeft days to go",
                    color = ZColor.InkSoft, fontSize = 11.sp)
            }
            Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(ZColor.Paper).padding(12.dp)) {
                MutedText("By the ${dayNum}${ordinal(dayNum)}", 11)
                Text(money(curSame), color = ZColor.Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                if (prevSame > 0) {
                    val ch = ((curSame - prevSame) / prevSame * 100).toInt()
                    Text((if (ch > 0) "▲ $ch%" else if (ch < 0) "▼ ${kotlin.math.abs(ch)}%" else "same") + " vs " + shortAmt(prevSame) + " last month",
                        color = if (ch > 0) ZColor.Warn else if (ch < 0) ZColor.Income else ZColor.Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                } else MutedText("no data for last month", 11)
            }
        }
        if (note != null) { Spacer(Modifier.height(10.dp)); MutedText(note, 11) }
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
