package com.nagesh.zelu

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

@Composable internal fun PayeesView(
    list: List<Row>, onDrill: (String, List<Row>) -> Unit,
    onRecategorize: (String?, String, String, String) -> Unit, onNewCat: (((String) -> Unit)) -> Unit,
    onRename: (String, String) -> Unit,
    fixedSet: Analytics.FixedSet,
    allRows: List<Row> = emptyList(),
) {
    val spend = list.filter { it.kind == "expense" }
    val payees = Analytics.byPayee(list)
    var query by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf("amount") }
    var catSel by remember { mutableStateOf("All") }
    var open by remember { mutableStateOf<String?>(null) }
    var ptab by remember { mutableStateOf("overview") }
    var limit by remember { mutableStateOf(10) }
    fun nameOf(r: Row) = r.counterparty?.takeIf { it.isNotBlank() } ?: r.category
    val total = payees.sumOf { it.amount }
    val lastDate = spend.groupBy { nameOf(it) }.mapValues { e -> e.value.maxOf { it.date } }

    Text("Who you paid", color = ZColor.Ink, fontSize = 30.sp, fontWeight = FontWeight.Bold)
    if (payees.isEmpty()) { Panel { MutedText("No payments in this view.") }; return }

    // 1. Summary: how concentrated your spending is.
    Panel {
        Eyebrow("This view")
        Spacer(Modifier.height(6.dp))
        Text("${payees.size} payees · ${money(total)}", color = ZColor.Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        val top5 = payees.take(5)
        val share = if (total > 0) top5.sumOf { it.amount } / total else 0.0
        MutedText("Your top ${top5.size} take ${Math.round(share * 100)}% of it.", 13)
        if (total > 0) {
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))) {
                top5.forEach { p ->
                    Box(Modifier.weight((p.amount / total).toFloat().coerceAtLeast(0.01f)).fillMaxHeight()
                        .background(catColor(p.cats.firstOrNull() ?: "Unknown")))
                }
                val rest = (1.0 - share).toFloat()
                if (rest > 0.005f) Box(Modifier.weight(rest).fillMaxHeight().background(ZColor.Line))
            }
        }
    }

    // Tabs: the picture first, the full list on its own tab.
    val freshN = run {
        val thisM = Dates.todayMonth()
        val seen = HashMap<String, String>()
        allRows.filter { it.kind == "expense" }.forEach { r -> val n = nameOf(r); val cur = seen[n]; if (cur == null || r.date < cur) seen[n] = r.date }
        if (seen.size <= 6) 0 else payees.count { seen[it.name]?.startsWith(thisM) == true }.coerceAtMost(6)
    }
    Spacer(Modifier.height(12.dp))
    ChipScroll {
        Chip("Overview", ptab == "overview", true) { ptab = "overview" }
        Chip("All", ptab == "all", true) { ptab = "all" }
        Chip("New" + (if (freshN > 0) " ($freshN)" else ""), ptab == "new", true) { ptab = "new" }
    }
    if (ptab == "overview") {
        var pick by remember { mutableStateOf(0) }
        data class Tile(val name: String, val amt: Double, val cats: List<String>, val count: Int, val rest: Boolean)
        val topList = payees.take(9)
        val restAmt = (total - topList.sumOf { it.amount }).coerceAtLeast(0.0)
        val tiles = (topList.map { Tile(it.name, it.amount, it.cats, it.count, false) } +
            (if (restAmt > 0.5 && payees.size > 9) listOf(Tile((payees.size - 9).toString() + " smaller payees", restAmt, emptyList(), 0, true)) else emptyList()))
            .sortedByDescending { it.amt }
        val cells = treemap(tiles.map { it.amt })
        val cur = tiles.getOrNull(pick) ?: tiles.first()
        Panel {
            BoxWithConstraints(Modifier.fillMaxWidth().height(230.dp).clip(RoundedCornerShape(14.dp))) {
                val bw = maxWidth; val bh = maxHeight
                cells.forEach { c ->
                    val t = tiles[c.idx]
                    val cw = bw * (c.w / 100f); val ch = bh * (c.h / 100f)
                    val col = if (t.rest) ZColor.Muted.copy(alpha = 0.55f) else catColor(t.cats.firstOrNull() ?: "Unknown")
                    val shape = RoundedCornerShape(10.dp)
                    Box(Modifier.offset(bw * (c.x / 100f), bh * (c.y / 100f)).size(cw, ch).padding(1.5.dp).clip(shape).background(col)
                        .then(if (pick == c.idx) Modifier.border(2.dp, ZColor.Ink, shape) else Modifier)
                        .clickable { pick = c.idx }.padding(7.dp)) {
                        if (cw > 64.dp && ch > 30.dp) Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                            Text(if (cw > 110.dp) t.name else t.name.substringBefore(' '), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (ch > 54.dp && cw > 84.dp) Text(money(t.amt), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(cur.name, color = ZColor.Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    MutedText(if (cur.rest) "Everyone else" else cur.count.toString() + (if (cur.count == 1) " payment · " else " payments · ") + cur.cats.joinToString(", "), 12)
                }
                Spacer(Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(money(cur.amt), color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    MutedText((if (total > 0) { val p = cur.amt / total * 100; if (p < 1) "<1%" else Math.round(p).toString() + "%" } else "") + " of spend", 11)
                }
            }
            Spacer(Modifier.height(10.dp))
            ChipScroll {
                if (cur.rest) Chip("See all payees", false) { ptab = "all" }
                else {
                    Chip("See payments", false) { onDrill(cur.name, spend.filter { nameOf(it) == cur.name }) }
                    Chip("Rename or change category", false) { open = cur.name }
                }
            }
        }
        val freq = payees.sortedByDescending { it.count }.take(4)
        if (freq.isNotEmpty() && freq.first().count > 1) {
            Spacer(Modifier.height(14.dp))
            Text("Paid most often", color = ZColor.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                freq.forEach { p ->
                    Column(Modifier.width(124.dp).clip(RoundedCornerShape(12.dp)).background(ZColor.Card).border(1.dp, ZColor.Line, RoundedCornerShape(12.dp))
                        .clickable { open = p.name }.padding(12.dp)) {
                        Text(p.count.toString() + "×", color = ZColor.Accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(p.name, color = ZColor.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        MutedText(money(p.amount), 12)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Chip("See all " + payees.size + " payees", false) { ptab = "all" }
    }

    // 2. Search, sort and category filter.
    Spacer(Modifier.height(14.dp))
    if (ptab == "all") OutlinedTextField(
        value = query, onValueChange = { query = it }, singleLine = true,
        placeholder = { Text("Search payee") }, modifier = Modifier.fillMaxWidth(),
    )
    if (ptab == "all") { Spacer(Modifier.height(8.dp))
    ChipScroll {
        listOf("amount" to "Amount", "count" to "Most paid", "recent" to "Recent").forEach { (k, l) ->
            Chip(l, sort == k, true) { sort = k }
        }
    } }
    val catsPresent = payees.flatMap { it.cats }.groupingBy { it }.eachCount().entries
        .sortedByDescending { it.value }.map { it.key }
    val sel = if (catSel in catsPresent) catSel else "All"
    androidx.compose.runtime.LaunchedEffect(query, sort, sel) { limit = 10 }
    if (ptab == "all") { Spacer(Modifier.height(8.dp))
    ChipScroll {
        Chip("All", sel == "All", true) { catSel = "All" }
        catsPresent.forEach { c -> Chip(customLabel(c), sel == c, true) { catSel = c } }
    } }

    val q = query.trim().lowercase()
    val filtered = payees.filter { p -> (q.isEmpty() || p.name.lowercase().contains(q)) && (sel == "All" || sel in p.cats) }
    val ordered = when (sort) {
        "count" -> filtered.sortedByDescending { it.count }
        "recent" -> filtered.sortedByDescending { lastDate[it.name] ?: "" }
        else -> filtered
    }
    val split = q.isEmpty() && sel == "All"
    val shownList = ordered.take(limit)
    val fixedP = shownList.filter { p -> p.name.trim().lowercase() in fixedSet.payees || (p.cats.isNotEmpty() && p.cats.all { it.lowercase() in fixedSet.cats }) }
    val everyday = shownList.filter { it !in fixedP }

    @Composable fun section(title: String, items: List<Analytics.Payee>) {
        if (items.isEmpty()) return
        val top = items.maxOf { it.amount }.coerceAtLeast(1.0)
        Panel {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Eyebrow(title); MutedText(money(items.sumOf { it.amount }), 12)
            }
            items.forEachIndexed { i, p ->
                Spacer(Modifier.height(if (i == 0) 10.dp else 14.dp))
                val c0 = p.cats.firstOrNull() ?: "Unknown"
                val pct = if (total > 0) Math.round(p.amount / total * 100).toInt() else 0
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { open = p.name }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MerchantTile(p.name, c0, 36.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.name, color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            MutedText("${p.count} ${if (p.count == 1) "payment" else "payments"} · ${p.cats.joinToString(", ")}", 11)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(money(p.amount), color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            MutedText(if (pct < 1) "<1%" else "$pct%", 11)
                        }
                    }
                    Spacer(Modifier.height(7.dp))
                    Meter((p.amount / top).toFloat(), catColor(c0))
                }
            }
        }
    }

    // 4b. Payees you have never paid before this month, so an unfamiliar charge stands out.
    if (ptab == "new") run {
        val thisM = Dates.todayMonth()
        val firstSeen = HashMap<String, String>()
        allRows.filter { it.kind == "expense" }.forEach { r ->
            val n = nameOf(r); val cur = firstSeen[n]
            if (cur == null || r.date < cur) firstSeen[n] = r.date
        }
        val fresh = payees.filter { firstSeen[it.name]?.startsWith(thisM) == true }.take(6)
        if (fresh.isEmpty() || firstSeen.size <= fresh.size) Panel { MutedText("Nothing new this month. Every payee here is one you have paid before.") }
        else Panel {
            Eyebrow("New this month")
            Spacer(Modifier.height(4.dp))
            MutedText("First time you've paid these. Check they're all yours.", 11)
            fresh.forEach { p ->
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { open = p.name },
                    verticalAlignment = Alignment.CenterVertically) {
                    MerchantTile(p.name, p.cats.firstOrNull() ?: "Unknown", 30.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.name, color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        MutedText(prettyDate(firstSeen[p.name] ?: ""), 11)
                    }
                    Text(money(p.amount), color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // 5. Fixed costs (EMI, rent...) are kept apart so they don't bury everyday spending.
    if (ptab == "all") {
        when {
            ordered.isEmpty() -> Panel { MutedText("No payee matches.") }
            split && fixedP.isNotEmpty() -> { section("Everyday spending", everyday); section("Fixed & recurring", fixedP) }
            else -> section(if (split) "All payees" else "Results", shownList)
        }
        if (ordered.size > limit) {
            Spacer(Modifier.height(12.dp))
            Chip("Show 10 more (" + (ordered.size - limit) + " left)", false) { limit += 10 }
        }
    }

    // 7. Payee detail.
    open?.let { name ->
        val rows = spend.filter { nameOf(it) == name }
        if (rows.isEmpty()) androidx.compose.runtime.LaunchedEffect(name) { open = null }
        else PayeeDetailDialog(name, rows, total, onSee = { open = null; onDrill(name, rows) },
            onRecategorize = onRecategorize, onNewCat = onNewCat,
            others = payees.map { it.name }.filter { !it.equals(name, ignoreCase = true) }.take(12),
            onRename = { to -> open = null; onRename(name, to) }, onDismiss = { open = null })
    }
}

@Composable internal fun PayeeDetailDialog(
    name: String, rows: List<Row>, viewTotal: Double, onSee: () -> Unit,
    onRecategorize: (String?, String, String, String) -> Unit, onNewCat: (((String) -> Unit)) -> Unit,
    others: List<String>, onRename: (String) -> Unit, onDismiss: () -> Unit,
) {
    var renaming by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(name) }
    val total = rows.sumOf { it.amount }
    val n = rows.size
    val primary = rows.groupBy { it.category }.maxByOrNull { it.value.size }?.key ?: "Unknown"
    val first = rows.minOf { it.date }; val last = rows.maxOf { it.date }
    val endM = last.take(7)
    val months = (5 downTo 0).map { Dates.shiftMonth(endM, -it) }
    val sums = months.map { m -> rows.filter { it.date.startsWith(m) }.sumOf { it.amount } }
    val mx = (sums.maxOrNull() ?: 0.0).coerceAtLeast(1.0)
    val withPayee = rows.firstOrNull { !it.counterparty.isNullOrBlank() }
    var editing by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp)
            .heightIn(max = 640.dp).verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MerchantTile(name, primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(name, color = ZColor.Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                    MutedText(primary + (if (viewTotal > 0) " · " + Math.round(total / viewTotal * 100) + "% of this view" else ""), 12)
                }
                Spacer(Modifier.width(10.dp))
                Text(money(total), color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Fact("Payments", n.toString())
                Fact("Average", money(total / n))
                Fact("Last paid", prettyDate(last))
            }
            Spacer(Modifier.height(4.dp))
            MutedText("First paid " + prettyDate(first), 12)
            Spacer(Modifier.height(14.dp))
            Eyebrow("Month by month")
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                months.forEachIndexed { i, m ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (sums[i] > 0) shortAmt(sums[i]) else "", color = ZColor.Muted, fontSize = 10.sp, maxLines = 1)
                        Spacer(Modifier.height(4.dp))
                        Box(Modifier.height(52.dp).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                            Box(Modifier.fillMaxWidth().height((52 * (sums[i] / mx)).coerceAtLeast(3.0).dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (sums[i] > 0) catColor(primary) else ZColor.Line))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(MONTH_ABBR[m.substring(5, 7).toInt() - 1], color = ZColor.Muted, fontSize = 10.sp)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.clip(RoundedCornerShape(20.dp)).border(1.dp, ZColor.Line, RoundedCornerShape(20.dp))
                    .clickable { onSee() }.padding(horizontal = 16.dp, vertical = 9.dp)) {
                    Text("See payments", color = ZColor.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                if (withPayee != null) Box(Modifier.clip(RoundedCornerShape(20.dp)).border(1.dp, ZColor.Line, RoundedCornerShape(20.dp))
                    .clickable { editing = !editing }.padding(horizontal = 16.dp, vertical = 9.dp)) {
                    Text(if (editing) "Cancel" else "Change category", color = ZColor.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.clip(RoundedCornerShape(20.dp)).border(1.dp, ZColor.Line, RoundedCornerShape(20.dp))
                .clickable { renaming = !renaming }.padding(horizontal = 16.dp, vertical = 9.dp)) {
                Text(if (renaming) "Cancel" else "Rename or merge", color = ZColor.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            if (renaming) {
                Spacer(Modifier.height(10.dp))
                MutedText("Give it a better name, or type another payee's name to merge the two.", 12)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = newName, onValueChange = { newName = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (others.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    MutedText("Merge into:", 11)
                    Spacer(Modifier.height(4.dp))
                    ChipScroll { others.forEach { o -> Chip(o, false) { newName = o } } }
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.align(Alignment.End).clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                    .clickable { if (newName.isNotBlank()) onRename(newName) }.padding(horizontal = 20.dp, vertical = 9.dp)) {
                    Text("Save name", color = ZColor.Paper, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            if (editing && withPayee != null) {
                fun pick(kind: String, cat: String) { onRecategorize(withPayee.counterparty, withPayee.id, kind, cat); onDismiss() }
                Spacer(Modifier.height(10.dp))
                MutedText("Applies to every payment to $name:", 12)
                Spacer(Modifier.height(6.dp))
                ChipScroll {
                    Classifier.CATEGORIES.keys.forEach { c -> Chip(c, false) { pick(if (c == "Investment") "invest" else "expense", c) } }
                    CustomCats.names.forEach { c -> Chip(customLabel(c), false) { pick("expense", c) } }
                    Chip("＋ New", false, filled = true) { onNewCat { c -> pick("expense", c) } }
                }
                Spacer(Modifier.height(6.dp))
                ChipScroll {
                    Chip("Money in", false) { pick("income", "Income") }
                    Chip("Lent", false) { pick("lent", "Lent") }
                    Chip("Borrowed", false) { pick("borrowed", "Borrowed") }
                    Chip("Ignore", false) { pick("ignore", "Ignore") }
                }
            }
            Spacer(Modifier.height(14.dp))
            Box(Modifier.align(Alignment.End).clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                .clickable { onDismiss() }.padding(horizontal = 22.dp, vertical = 10.dp)) {
                Text("Close", color = ZColor.Paper, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ---------- Investments ----------

@Composable internal fun InvestView(allRows: List<Row>, onDrill: (String, List<Row>) -> Unit) {
    val inv = Analytics.investments(allRows)
    Text("Investments", color = ZColor.Ink, fontSize = 30.sp, fontWeight = FontWeight.Bold)
    if (inv.isEmpty()) {
        Panel { MutedText("Nothing yet. PF, NPS and mutual-fund contributions will show up here.") }
        return
    }
    Panel {
        Eyebrow("Contributed")
        Spacer(Modifier.height(4.dp))
        Text(money(inv.sumOf { it.amount }), color = ZColor.Income, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        MutedText("PF, NPS and funds — kept out of your spending and income.", 11)
    }
    fun typeOf(r: Row) = r.counterparty?.takeIf { it.isNotBlank() } ?: "Investment"
    val byType = inv.groupBy { typeOf(it) }
        .map { (name, rows) -> Triple(name, rows.sumOf { it.amount }, rows.size) }
        .sortedByDescending { it.second }
    val top = byType.firstOrNull()?.second ?: 1.0
    Panel {
        Eyebrow("By type")
        byType.forEachIndexed { i, (name, amt, cnt) ->
            Spacer(Modifier.height(if (i == 0) 8.dp else 14.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .clickable { onDrill(name, inv.filter { typeOf(it) == name }) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MerchantTile(name, "Investment", 32.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(name, color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        MutedText("$cnt ${if (cnt == 1) "contribution" else "contributions"}", 11)
                    }
                    Text(money(amt), color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(7.dp))
                Meter((amt / top).toFloat(), catColor("Investment"))
            }
        }
    }
}

// ---------- Dues & Reminders ----------
