package com.koolicream.zelu

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Today
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.rounded.Category
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay

@Composable internal fun PaceView(
    allRows: List<Row>, list: List<Row>, f: Analytics.Filter, month: String, account: String,
    excludeEmi: Boolean = false, leave: Set<String> = emptySet(),
) {
    // The budget is for flexible spending: EMI and costs marked Fixed are left out here.
    val baseRows = allRows.filter { it.category !in leave && !(excludeEmi && it.category == "EMI") }
    Text("Budget pace", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    if (leave.isNotEmpty()) MutedText("Leaves out EMI and fixed costs", 12)

    if (Analytics.BUDGET <= 0) {
        MutedText("Set a monthly budget from the menu to see if you are ahead or behind.", 13)
    } else if (f.period == "month") {
        val dim = Dates.daysInMonth(month)
        val isCur = month == Dates.todayMonth()
        val upto = if (isCur) Dates.dayOfMonth() else dim
        val run = Analytics.runningTotals(baseRows, month, account, upto)
        val total = if (upto > 0) run[upto] else 0.0
        val pace = Analytics.BUDGET * upto / dim
        val diff = total - pace
        Panel {
            Eyebrow("${Dates.monthLong(month)} running total")
            Text(money(total), color = ZColor.Ink, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            MutedText(
                if (isCur) (if (diff > 0) "${money(diff)} above the pace for day $upto"
                else "${money(-diff)} under the pace for day $upto")
                else "Month total against a ${money(Analytics.BUDGET)} budget", 13)
            if (isCur && upto > 0) {
                val projected = total / upto * dim
                Spacer(Modifier.height(8.dp))
                MutedText("On this run-rate", 12)
                Text("~${money(projected)} by ${Dates.monthLabel(month)}-end",
                    color = if (projected > Analytics.BUDGET) ZColor.Accent else ZColor.Income,
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(12.dp))
            PaceChart(run, upto, dim, Analytics.BUDGET)
        }
    } else {
        Panel { MutedText("The running total works on a single month. Switch to Month to see it.") }
    }
}

@Composable internal fun PaceChart(run: DoubleArray, upto: Int, dim: Int, budget: Double) {
    val peak = if (upto >= 1) (1..upto).maxOf { run[it] } else 0.0
    val maxY = maxOf(budget, peak) * 1.05
    Canvas(Modifier.fillMaxWidth().height(170.dp).clipToBounds()) {
        val w = size.width; val h = size.height
        val padL = 6f; val padR = 6f; val padB = 18f; val padT = 10f
        fun x(d: Int) = padL + (d - 1).toFloat() / (dim - 1).coerceAtLeast(1) * (w - padL - padR)
        fun y(v: Double) = padT + (1 - (v / maxY).toFloat()) * (h - padT - padB)
        // budget line
        drawLine(ZColor.Line, Offset(padL, y(budget)), Offset(w - padR, y(budget)), 2f)
        // baseline
        drawLine(ZColor.Line, Offset(padL, y(0.0)), Offset(w - padR, y(0.0)), 2f)
        // pace diagonal
        drawLine(ZColor.Muted.copy(alpha = 0.5f), Offset(x(1), y(0.0)), Offset(x(dim), y(budget)), 2f)
        if (upto >= 1) {
            val path = Path(); val area = Path()
            area.moveTo(x(1), y(0.0))
            for (d in 1..upto) {
                val px = x(d); val py = y(run[d])
                if (d == 1) path.moveTo(px, py) else path.lineTo(px, py)
                area.lineTo(px, py)
            }
            area.lineTo(x(upto), y(0.0)); area.close()
            drawPath(area, ZColor.Accent.copy(alpha = 0.14f))
            drawPath(path, ZColor.Accent, style = Stroke(width = 6f))
            drawCircle(ZColor.Accent, 9f, Offset(x(upto), y(run[upto])))
        }
    }
    MutedText("Day 1 to $dim · Budget ${money(budget)}", 11)
}


// ---------- Account chips with bank logos ----------

internal data class TrendBucket(
    val key: String, val label: String, val title: String,
    val rows: List<Row>, val spent: Double, val income: Double,
)

internal fun buildBuckets(rows: List<Row>, period: String): List<TrendBucket> {
    if (rows.isEmpty()) return emptyList()
    fun keyOf(d: String) = when (period) {
        "day" -> d
        "week" -> weekStart(d)
        "quarter" -> d.take(4) + "-Q" + ((d.substring(5, 7).toInt() - 1) / 3 + 1)
        else -> d.take(7)
    }
    fun prev(k: String) = when (period) {
        "day" -> Dates.shiftDays(k, -1)
        "week" -> Dates.shiftDays(k, -7)
        "quarter" -> { val y = k.take(4).toInt(); val q = k.last().digitToInt(); if (q == 1) "${y - 1}-Q4" else "$y-Q${q - 1}" }
        else -> Dates.shiftMonth(k, -1)
    }
    val cap = when (period) { "day" -> 45; "week" -> 26; "quarter" -> 12; else -> 24 }
    val firstKey = keyOf(rows.minOf { it.date })
    var k = keyOf(Dates.today())
    val keys = ArrayList<String>()
    for (i in 0 until cap) { keys.add(k); if (k <= firstKey) break; k = prev(k) }
    keys.reverse()
    val grouped = rows.groupBy { keyOf(it.date) }
    return keys.map { key ->
        val rs = grouped[key] ?: emptyList()
        val s = Analytics.summarize(rs)
        val (label, title) = when (period) {
            "day" -> key.substring(8, 10).toInt().toString() to (dayMonth(key) + " '" + key.substring(2, 4))
            "week" -> dayMonth(key) to (dayMonth(key) + " – " + dayMonth(Dates.shiftDays(key, 6)) + " '" + key.substring(2, 4))
            "quarter" -> ("Q" + key.last() + "'" + key.substring(2, 4)) to ("Q" + key.last() + " " + key.take(4))
            else -> (MONTH_ABBR[key.substring(5, 7).toInt() - 1] + "'" + key.substring(2, 4)) to
                (MONTH_ABBR[key.substring(5, 7).toInt() - 1] + " '" + key.substring(2, 4))
        }
        TrendBucket(key, label, title, rs, s.spent, s.income)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun TrendsScreen(
    rows: List<Row>, accounts: List<String>, onBack: () -> Unit, onReview: (String, List<Row>) -> Unit,
) {
    var period by remember { mutableStateOf("month") }
    var mode by remember { mutableStateOf("both") }
    var acct by remember { mutableStateOf("All") }
    var cat by remember { mutableStateOf("All") }
    var sel by remember { mutableStateOf(-1) }
    var filterOpen by remember { mutableStateOf(false) }
    val base = remember(rows, acct, cat) {
        rows.filter { it.kind == "expense" || it.kind == "income" || it.kind == "refund" }
            .filter { (acct == "All" || it.account == acct) && (cat == "All" || it.category == cat) }
    }
    val buckets = remember(base, period) { buildBuckets(base, period) }
    val n = buckets.size
    val idx = if (sel in 0 until n) sel else n - 1
    val bw = 56.dp
    val chartH = 200.dp
    val scroll = rememberScrollState()
    LaunchedEffect(period, n) { delay(80); scroll.scrollTo(scroll.maxValue) }

    val saved = buckets.map { it.income - it.spent }
    val maxV = niceCeil(when (mode) {
        "saved" -> maxOf(0.0, saved.maxOrNull() ?: 0.0)
        "cat" -> buckets.maxOfOrNull { it.spent } ?: 0.0
        else -> buckets.maxOfOrNull { maxOf(it.spent, it.income) } ?: 0.0
    })
    val minV = if (mode == "saved") -niceCeil(maxOf(0.0, -(saved.minOrNull() ?: 0.0))).let { if (saved.any { v -> v < 0 }) it else 0.0 } else 0.0
    val topCats = remember(base, mode) { Analytics.byCategory(base).take(5).map { it.first } }
    val perBucketCats = remember(buckets, mode) { buckets.map { b -> Analytics.byCategory(b.rows).toMap() } }

    Box(Modifier.fillMaxSize().background(ZColor.Paper)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 18.dp, end = 18.dp, top = 46.dp, bottom = 30.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(20.dp)).background(ZColor.Card)
                    .border(1.dp, ZColor.Line, RoundedCornerShape(20.dp)).clickable { onBack() }.buttonLabel("Back"), contentAlignment = Alignment.Center) {
                    Text("‹", color = ZColor.Ink, fontSize = 24.sp, modifier = Unspoken)
                }
                Spacer(Modifier.width(12.dp))
                Text("Trends", color = ZColor.Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TopIcon("Filters", onClick = { filterOpen = true }, dot = acct != "All" || cat != "All", dotLabel = "some are on") { FunnelIcon() }
            }
            Spacer(Modifier.height(14.dp))
            TabRowWrap {
                IconTab(Icons.Rounded.Today, "Day", 0xFFFF6B1A, 0xFFFF8A3D, period == "day") { period = "day"; sel = -1 }
                IconTab(Icons.Rounded.DateRange, "Week", 0xFF3B82F6, 0xFF60A5FA, period == "week") { period = "week"; sel = -1 }
                IconTab(Icons.Rounded.CalendarMonth, "Month", 0xFF22B455, 0xFF3DD171, period == "month") { period = "month"; sel = -1 }
                IconTab(Icons.Rounded.Event, "Quarter", 0xFF8B5CF6, 0xFFA78BFA, period == "quarter") { period = "quarter"; sel = -1 }
            }
            Spacer(Modifier.height(8.dp))
            TabRowWrap {
                IconTab(Icons.Rounded.SwapVert, "Spend & income", 0xFF0D9488, 0xFF2DD4BF, mode == "both") { mode = "both" }
                IconTab(Icons.Rounded.Savings, "Saved", 0xFF22B455, 0xFF3DD171, mode == "saved") { mode = "saved" }
                IconTab(Icons.Rounded.Category, "By category", 0xFF8B5CF6, 0xFFA78BFA, mode == "cat") { mode = "cat" }
            }
            Spacer(Modifier.height(14.dp))
            if (n == 0) {
                Panel { MutedText("No transactions for this filter.") }
            } else {
                Panel {
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.width(40.dp).height(chartH), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.End) {
                            (0..4).forEach { i -> Text(shortAmt(maxV - (maxV - minV) * i / 4), color = ZColor.Muted, fontSize = 10.sp) }
                        }
                        Spacer(Modifier.width(6.dp))
                        Column(Modifier.weight(1f).horizontalScroll(scroll)) {
                            Canvas(
                                Modifier.width(bw * n).height(chartH).pointerInput(n) {
                                    detectTapGestures { o -> sel = (o.x / bw.toPx()).toInt().coerceIn(0, n - 1) }
                                },
                            ) {
                                val h = size.height; val w = size.width; val bwPx = bw.toPx(); val barW = 22.dp.toPx()
                                val span = (maxV - minV).coerceAtLeast(1.0)
                                fun y(v: Double) = (h * (maxV - v) / span).toFloat()
                                for (i in 0..4) drawLine(ZColor.Line, androidx.compose.ui.geometry.Offset(0f, h * i / 4), androidx.compose.ui.geometry.Offset(w, h * i / 4), 1.dp.toPx())
                                drawRoundRect(Color.Black.copy(alpha = 0.06f),
                                    androidx.compose.ui.geometry.Offset(idx * bwPx + 4.dp.toPx(), 0f),
                                    androidx.compose.ui.geometry.Size(bwPx - 8.dp.toPx(), h), androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()))
                                val rad = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                                buckets.forEachIndexed { i, b ->
                                    val cx = i * bwPx + bwPx / 2
                                    when (mode) {
                                        "saved" -> {
                                            val v = b.income - b.spent
                                            val y0 = y(0.0); val y1 = y(v)
                                            if (kotlin.math.abs(v) > 0.5) drawRoundRect(if (v >= 0) ZColor.Income else ZColor.Warn,
                                                androidx.compose.ui.geometry.Offset(cx - barW / 2, minOf(y0, y1)),
                                                androidx.compose.ui.geometry.Size(barW, kotlin.math.abs(y0 - y1).coerceAtLeast(2f)), rad)
                                        }
                                        "cat" -> {
                                            var top = y(0.0)
                                            val m = perBucketCats[i]
                                            val parts = topCats.map { c -> catColor(c) to (m[c] ?: 0.0) } +
                                                listOf(Color(0xFFB8C0BB) to (b.spent - topCats.sumOf { c -> m[c] ?: 0.0 }).coerceAtLeast(0.0))
                                            parts.forEach { (col, v) ->
                                                if (v > 0.5) {
                                                    val hh = (h * v / span).toFloat()
                                                    drawRect(col, androidx.compose.ui.geometry.Offset(cx - barW / 2, top - hh), androidx.compose.ui.geometry.Size(barW, hh))
                                                    top -= hh
                                                }
                                            }
                                        }
                                        else -> if (b.spent > 0.5) drawRoundRect(ZColor.Accent,
                                            androidx.compose.ui.geometry.Offset(cx - barW / 2, y(b.spent)),
                                            androidx.compose.ui.geometry.Size(barW, h - y(b.spent)), rad)
                                    }
                                }
                                if (mode == "both") {
                                    val path = androidx.compose.ui.graphics.Path()
                                    buckets.forEachIndexed { i, b ->
                                        val px = i * bwPx + bwPx / 2; val py = y(b.income)
                                        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                                    }
                                    drawPath(path, ZColor.Income, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx()))
                                    buckets.forEachIndexed { i, b ->
                                        drawCircle(ZColor.Income, 4.dp.toPx(), androidx.compose.ui.geometry.Offset(i * bwPx + bwPx / 2, y(b.income)))
                                    }
                                }
                            }
                            Row {
                                buckets.forEachIndexed { i, b ->
                                    Box(Modifier.width(bw), contentAlignment = Alignment.Center) {
                                        Text(b.label, color = if (i == idx) ZColor.Ink else ZColor.Muted, fontSize = 11.sp,
                                            fontWeight = if (i == idx) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                    if (mode == "cat") {
                        Spacer(Modifier.height(10.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            topCats.forEach { c ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(catColor(c)))
                                    Spacer(Modifier.width(5.dp)); MutedText(c, 11)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFB8C0BB)))
                                Spacer(Modifier.width(5.dp)); MutedText("Other", 11)
                            }
                        }
                    } else if (mode == "both") {
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(ZColor.Accent)); Spacer(Modifier.width(5.dp)); MutedText("Spent", 11)
                            Spacer(Modifier.width(14.dp))
                            Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(ZColor.Income)); Spacer(Modifier.width(5.dp)); MutedText("Money in", 11)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                val b = buckets[idx]
                Panel {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(b.title, color = ZColor.Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        if (acct != "All") MutedText(accountLabel(acct), 12)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Fact("Spent", money(b.spent), ZColor.Accent)
                        Fact("Money in", money(b.income), ZColor.Income)
                        val sv = b.income - b.spent
                        Fact("Saved", (if (sv < 0) "−" else "") + money(kotlin.math.abs(sv)), if (sv >= 0) ZColor.Income else ZColor.Warn)
                    }
                    val top = Analytics.byCategory(b.rows).take(3)
                    if (top.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        top.forEach { (c, v) ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                MerchantTile("", c, 32.dp)
                                Spacer(Modifier.width(10.dp))
                                Text(c, color = ZColor.Ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                Text(money(v), color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(ZColor.Ink)
                            .clickable { onReview(b.title, b.rows.newestFirst()) }.padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("Review " + b.label, color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
        if (filterOpen) {
            var dAcct by remember { mutableStateOf(acct) }
            var dCat by remember { mutableStateOf(cat) }
            val cats = listOf("All") + (Classifier.CATEGORIES.keys + CustomCats.names + "Unknown").distinct()
            Dialog(onDismissRequest = { filterOpen = false }) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Paper).padding(18.dp)
                    .heightIn(max = 640.dp).verticalScroll(rememberScrollState())) {
                    Text("Filter trends", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(14.dp))
                    MutedText("Source account", 12)
                    Spacer(Modifier.height(6.dp))
                    AccountChips(accounts, dAcct) { dAcct = it }
                    Spacer(Modifier.height(16.dp))
                    MutedText("Category", 12)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        cats.forEach { c ->
                            val on = c == dCat
                            val icon = CatIcons.forCategory(c)
                            Row(
                                Modifier.clip(RoundedCornerShape(20.dp)).background(if (on) ZColor.Ink else ZColor.Card)
                                    .border(1.dp, if (on) ZColor.Ink else ZColor.Line, RoundedCornerShape(20.dp))
                                    .clickable { dCat = c }.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (icon != null) {
                                    androidx.compose.material3.Icon(icon, null, tint = if (on) ZColor.Paper else catColor(c), modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text(if (c == "All") "All categories" else c, color = if (on) ZColor.Paper else ZColor.Ink, fontSize = 13.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Clear filter", color = ZColor.Accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { acct = "All"; cat = "All"; sel = -1; filterOpen = false }.padding(8.dp))
                        Box(
                            Modifier.clip(RoundedCornerShape(24.dp)).background(ZColor.Ink)
                                .clickable { acct = dAcct; cat = dCat; sel = -1; filterOpen = false }
                                .padding(horizontal = 26.dp, vertical = 11.dp),
                        ) { Text("Apply filter", color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
                    }
                }
            }
        }
    }
}
