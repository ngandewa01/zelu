package com.nagesh.zelu

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ZeluApp() }
    }
}

fun money(n: Double): String = "₹" + String.format(Locale.US, "%,.0f", n)

private val PERIODS = listOf(
    "week" to "Week", "month" to "Month", "3m" to "3M", "6m" to "6M",
    "year" to "1Y", "all" to "All",
)

@Composable
fun ZeluApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var rows by remember { mutableStateOf<List<Row>>(emptyList()) }
    var view by remember { mutableStateOf("ledger") }
    var period by remember { mutableStateOf("month") }
    var month by remember { mutableStateOf(Dates.todayMonth()) }
    var account by remember { mutableStateOf("All") }
    var daySel by remember { mutableStateOf(0) }
    var askOpen by remember { mutableStateOf(false) } // category picker open for ask
    var loading by remember { mutableStateOf(false) }

    fun hasPerm() = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
        PackageManager.PERMISSION_GRANTED

    fun reload() {
        scope.launch { rows = withContext(Dispatchers.IO) { Storage.loadRows(context) } }
    }

    fun rescan() {
        scope.launch {
            loading = true
            withContext(Dispatchers.IO) {
                if (Storage.dataVersion(context) < Storage.PARSER_VERSION) Storage.clear(context)
                Ingest.ingest(context, SmsReader.readInbox(context, 5000))
                Storage.setDataVersion(context, Storage.PARSER_VERSION)
            }
            rows = withContext(Dispatchers.IO) { Storage.loadRows(context) }
            loading = false
        }
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { res -> if (res.values.all { it }) rescan() }

    LaunchedEffect(Unit) {
        rows = withContext(Dispatchers.IO) { Storage.loadRows(context) }
        if (hasPerm()) rescan()
        else permLauncher.launch(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS))
    }

    val accounts = remember(rows) {
        listOf("All") + rows.map { it.account }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val f = Analytics.Filter(period, month, "", "", account)
    val list = Analytics.filter(rows, f)

    // decisions
    fun decide(id: String, kind: String, category: String, payee: String?, remember: Boolean) {
        scope.launch {
            withContext(Dispatchers.IO) {
                Storage.setOverride(context, id, kind, category)
                if (remember && !payee.isNullOrBlank()) Storage.setPayeeRule(context, payee, kind, category)
            }
            rows = withContext(Dispatchers.IO) { Storage.loadRows(context) }
            askOpen = false
        }
    }

    Box(Modifier.fillMaxSize().background(ZColor.Paper)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, top = 46.dp, bottom = 36.dp),
        ) {
            BrandBar()
            Spacer(Modifier.height(12.dp))
            ViewSwitcher(view) { view = it; daySel = 0 }
            Spacer(Modifier.height(12.dp))

            // period + account chips
            ChipScroll {
                PERIODS.forEach { (p, lbl) ->
                    Chip(lbl, period == p, filled = true) { period = p; daySel = 0 }
                }
            }
            if (period == "month") {
                Spacer(Modifier.height(8.dp))
                ChipScroll {
                    Dates.recentMonths(Dates.todayMonth(), 6).forEach { m ->
                        Chip(Dates.monthLabel(m), month == m) { month = m; daySel = 0 }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            ChipScroll {
                accounts.forEach { a -> Chip(a, account == a) { account = a; daySel = 0 } }
            }

            if (loading) {
                Spacer(Modifier.height(10.dp)); MutedText("Reading messages…")
            }

            Spacer(Modifier.height(8.dp))
            when (view) {
                "ledger" -> LedgerView(rows, list, f, accounts, month, account, daySel,
                    { daySel = if (daySel == it) 0 else it }, askOpen, { askOpen = it }, ::decide)
                "buckets" -> BucketsView(list)
                "payees" -> PayeesView(list)
                "pace" -> PaceView(rows, list, f, month, account)
            }
        }
    }
}

// ---------- shared pieces ----------

@Composable private fun BrandBar() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(ZColor.Ink),
            contentAlignment = Alignment.Center,
        ) { Text("Z", color = ZColor.Accent, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
        Spacer(Modifier.width(9.dp))
        Row {
            Text("Ze", color = ZColor.Ink, fontWeight = FontWeight.Bold, fontSize = 23.sp)
            Text("lu", color = ZColor.Accent, fontWeight = FontWeight.Bold, fontSize = 23.sp)
        }
    }
}

@Composable private fun ViewSwitcher(view: String, onPick: (String) -> Unit) {
    val items = listOf("ledger" to "Ledger", "pace" to "Pace", "buckets" to "Buckets", "payees" to "Payees")
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(ZColor.Card)
            .border(1.dp, ZColor.Line, RoundedCornerShape(14.dp)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items.forEach { (k, lbl) ->
            val sel = view == k
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                    .background(if (sel) ZColor.Ink else Color.Transparent)
                    .clickable { onPick(k) }.padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(lbl, color = if (sel) ZColor.Paper else ZColor.Muted,
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable private fun ChipScroll(content: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable private fun Chip(label: String, selected: Boolean, filled: Boolean = false, onClick: () -> Unit) {
    val bg = if (selected && filled) ZColor.Accent else ZColor.Card
    val border = if (selected) (if (filled) ZColor.Accent else ZColor.Ink) else ZColor.Line
    val fg = if (selected && filled) ZColor.AccentInk else if (selected) ZColor.Ink else ZColor.Muted
    Box(
        Modifier.clip(RoundedCornerShape(20.dp)).background(bg)
            .border(1.dp, border, RoundedCornerShape(20.dp))
            .clickable { onClick() }.padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(label, color = fg, fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
    }
}

@Composable private fun Panel(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 14.dp).clip(RoundedCornerShape(18.dp))
            .background(ZColor.Card).border(1.dp, ZColor.Line, RoundedCornerShape(18.dp)).padding(18.dp),
        content = content,
    )
}

@Composable private fun Eyebrow(text: String) {
    Text(text.uppercase(), color = ZColor.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
}

@Composable private fun MutedText(text: String, size: Int = 13) {
    Text(text, color = ZColor.Muted, fontSize = size.sp)
}

@Composable private fun Meter(fraction: Float, color: Color = ZColor.Ink) {
    Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(ZColor.Line)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(6.dp)
            .clip(RoundedCornerShape(3.dp)).background(color))
    }
}

@Composable private fun TxnRow(t: Row, highlight: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        MerchantTile(t.counterparty ?: t.category, t.category)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(t.counterparty?.takeIf { it.isNotBlank() } ?: t.category,
                color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            MutedText("${t.date} · ${subtitleFor(t)}", 11)
        }
        Spacer(Modifier.width(10.dp))
        AmountText(t, highlight)
    }
}

private fun subtitleFor(t: Row): String = when (t.kind) {
    "expense" -> t.category
    "lent" -> "Lent"
    "borrowed" -> "Borrowed"
    "transfer" -> t.category.ifBlank { "Transfer" }
    "ignore" -> "Ignored"
    else -> t.kind.replaceFirstChar { it.uppercase() }
}

@Composable private fun AmountText(t: Row, highlight: Boolean) {
    when (t.kind) {
        "transfer" -> Text("Transfer", color = ZColor.Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        "ignore" -> Text("—", color = ZColor.Muted, fontSize = 14.sp)
        "income", "refund", "borrowed" ->
            Text("+" + money(t.amount), color = ZColor.Income, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        else -> Text("−" + money(t.amount),
            color = if (highlight && t.kind == "expense") ZColor.Accent else ZColor.Warn,
            fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ---------- Ledger ----------

@Composable private fun LedgerView(
    allRows: List<Row>, list: List<Row>, f: Analytics.Filter, accounts: List<String>,
    month: String, account: String, daySel: Int, onDay: (Int) -> Unit,
    askOpen: Boolean, setAskOpen: (Boolean) -> Unit,
    decide: (String, String, String, String?, Boolean) -> Unit,
) {
    val s = Analytics.summarize(list)
    val ins = Analytics.insight(list, f)
    val bigIds = Analytics.bigSpendIds(list)
    val pct = (s.spent / Analytics.BUDGET).toFloat()
    val budgetLeft = (Analytics.BUDGET - s.spent).coerceAtLeast(0.0)

    // Spent + budget
    Panel {
        Eyebrow("Spent in " + Analytics.periodLabel(f))
        Text(money(s.spent), color = ZColor.Ink, fontSize = 38.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp)); Meter(pct, ZColor.Accent)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Fact("Budget", money(Analytics.BUDGET))
            Fact("Left", money(budgetLeft))
            Fact("Money in", money(s.income), ZColor.Income)
        }
    }

    // At a glance
    if (ins != null) {
        Panel {
            Eyebrow("At a glance")
            Spacer(Modifier.height(8.dp))
            Text("Most went to ${ins.topCat} — ${money(ins.topCatAmt)}",
                color = ZColor.Ink, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
            MutedText("${ins.topCatPct}% of spend", 13)
            Spacer(Modifier.height(12.dp))
            val ob = ins.overBy
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Fact("Biggest spend", "${ins.biggestName} · ${money(ins.biggestAmt)}")
                if (ob != null) {
                    val over = ob > 0
                    Fact(if (over) "Over a steady pace" else "Under pace",
                        money(kotlin.math.abs(ob)), if (over) ZColor.Accent else ZColor.Income)
                }
            }
            if (ob != null && ob > 0) {
                Spacer(Modifier.height(8.dp))
                MutedText("Trimming ${ins.topCat} is the quickest win — it's your biggest bucket.", 13)
            }
        }
    }

    // Spent by account
    val accTotals = Analytics.spentByAccount(list, accounts)
    if (accTotals.isNotEmpty()) {
        val accMax = accTotals.maxOf { it.second }.coerceAtLeast(1.0)
        Panel {
            Eyebrow("Spent by account")
            accTotals.forEach { (a, v) ->
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(a, color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(110.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Box(Modifier.weight(1f)) { Meter((v / accMax).toFloat(), ZColor.Ink) }
                    Spacer(Modifier.width(10.dp))
                    Text(money(v), color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // Cards
    val cardAccounts = accounts.filter { it.contains(" CC ") || it.startsWith("CC ") }
    if (cardAccounts.isNotEmpty()) {
        Panel {
            Eyebrow("Cards")
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                cardAccounts.forEach { a ->
                    val spent = Analytics.filter(allRows, f.copy(account = a))
                        .filter { it.kind == "expense" }.sumOf { it.amount }
                    CardTile(a, spent)
                }
            }
            Spacer(Modifier.height(10.dp))
            MutedText("Card spends count here and in your totals. Paying the bill is a transfer.", 11)
        }
    }

    // Daily calendar (month only)
    if (f.period == "month") {
        val daily = Analytics.dailyTotals(allRows, month, account)
        Panel {
            Eyebrow("Daily spend")
            Spacer(Modifier.height(8.dp))
            Calendar(month, daily, daySel, onDay)
            Spacer(Modifier.height(8.dp))
            MutedText("Darker days cost more. Tap a date to see that day.", 12)
        }
        if (daySel > 0) {
            val dayStr = "%s-%02d".format(month, daySel)
            val dayRows = list.filter { it.date == dayStr }.sortedByDescending { it.amount }
            val dtot = dayRows.filter { it.kind == "expense" }.sumOf { it.amount }
            Panel {
                Eyebrow("$daySel ${Dates.monthLong(month)} · ${money(dtot)} spent")
                if (dayRows.isEmpty()) { Spacer(Modifier.height(6.dp)); MutedText("No transactions on this day.") }
                else dayRows.forEach { TxnRow(it, bigIds.contains(it.id)) }
            }
        }
    }

    // Money in & out
    Panel {
        Eyebrow("Money in & out")
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Fact("In", money(s.income), ZColor.Income)
            Fact("Out", money(s.spent))
            Fact("Net", money(s.net), if (s.net < 0) ZColor.Warn else ZColor.Ink)
        }
    }

    // Lending
    val lent = Analytics.lent(list); val borrowed = Analytics.borrowed(list)
    if (lent.isNotEmpty() || borrowed.isNotEmpty()) {
        Panel {
            Eyebrow("Lending")
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Fact("Owed to you", money(lent.sumOf { it.amount }), ZColor.Income)
                Fact("You owe", money(borrowed.sumOf { it.amount }), ZColor.Warn)
            }
            lent.forEach { Spacer(Modifier.height(6.dp)); MutedText("${it.counterparty ?: "Someone"} owes you ${money(it.amount)}") }
            borrowed.forEach { Spacer(Modifier.height(6.dp)); MutedText("You owe ${it.counterparty ?: "someone"} ${money(it.amount)}") }
        }
    }

    // Ask-me
    val pending = list.filter { it.kind == "expense" && it.category == "Unknown" }
    val next = pending.firstOrNull()
    if (next != null) {
        Panel {
            Eyebrow("Needs you · ${pending.size} to go")
            Spacer(Modifier.height(6.dp))
            Text(next.counterparty?.takeIf { it.isNotBlank() } ?: "Payment",
                color = ZColor.Ink, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
            MutedText("${money(next.amount)} on ${next.date}${if (next.account.isNotBlank()) " · ${next.account}" else ""}. What is this?", 13)
            Spacer(Modifier.height(10.dp))
            ChipScroll {
                Chip("Spending", false, filled = true) { setAskOpen(true) }
                Chip("Money in", false) { decide(next.id, "income", "Income", next.counterparty, false) }
                Chip("Lent", false) { decide(next.id, "lent", "Lent", next.counterparty, true) }
                Chip("Borrowed", false) { decide(next.id, "borrowed", "Borrowed", next.counterparty, true) }
                Chip("Ignore", false) { decide(next.id, "ignore", "Ignore", null, false) }
            }
            if (askOpen) {
                Spacer(Modifier.height(10.dp))
                MutedText("Pick a category (remembered for ${next.counterparty ?: "this payee"}):", 12)
                Spacer(Modifier.height(6.dp))
                ChipScroll {
                    Classifier.CATEGORIES.keys.forEach { c ->
                        Chip(c, false) { decide(next.id, "expense", c, next.counterparty, true) }
                    }
                    Chip("Domestic help", false) { decide(next.id, "expense", "Domestic help", next.counterparty, true) }
                }
            }
        }
    }

    // Latest
    Panel {
        Eyebrow("Latest")
        val latest = list.sortedByDescending { it.date }.take(30)
        if (latest.isEmpty()) { Spacer(Modifier.height(6.dp)); MutedText("No transactions here.") }
        else latest.forEach { TxnRow(it, bigIds.contains(it.id)) }
    }

    Spacer(Modifier.height(6.dp))
    MutedText("Private — everything stays on your phone.", 11)
}

@Composable private fun Fact(k: String, v: String, color: Color = ZColor.Ink) {
    Column {
        MutedText(k, 12)
        Text(v, color = color, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable private fun CardTile(account: String, spent: Double) {
    val last4 = account.takeLast(4)
    Column(
        Modifier.width(240.dp).clip(RoundedCornerShape(16.dp)).background(ZColor.Paper)
            .padding(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.width(5.dp).height(18.dp).clip(RoundedCornerShape(3.dp)).background(bankColor(account)))
            Text("•• $last4", color = ZColor.Muted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text(account, color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        MutedText("This cycle", 12)
        Text(money(spent), color = ZColor.Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable private fun Calendar(month: String, daily: DoubleArray, daySel: Int, onDay: (Int) -> Unit) {
    val dim = Dates.daysInMonth(month)
    val max = (daily.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
    val (y, mo) = month.split("-").map { it.toInt() }
    val cal = java.util.GregorianCalendar(Dates.IST); cal.clear(); cal.set(y, mo - 1, 1)
    val firstWeekday = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7 // Mon=0
    val cells = ArrayList<Int>() // 0 = blank
    repeat(firstWeekday) { cells.add(0) }
    for (d in 1..dim) cells.add(d)
    while (cells.size % 7 != 0) cells.add(0)

    Row(Modifier.fillMaxWidth()) {
        listOf("M", "T", "W", "T", "F", "S", "S").forEach {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { MutedText(it, 10) }
        }
    }
    cells.chunked(7).forEach { week ->
        Row(Modifier.fillMaxWidth().padding(top = 5.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            week.forEach { d ->
                if (d == 0) {
                    Box(Modifier.weight(1f).aspectRatio(1f))
                } else {
                    val v = daily[d]
                    val alpha = if (v <= 0) 0f else (0.18f + (v / max).toFloat() * 0.72f)
                    val bg = if (alpha == 0f) Color.Transparent else ZColor.Accent.copy(alpha = alpha)
                    Box(
                        Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(9.dp))
                            .background(bg)
                            .border(if (daySel == d) 2.dp else 0.dp, if (daySel == d) ZColor.Ink else Color.Transparent, RoundedCornerShape(9.dp))
                            .clickable { onDay(d) },
                        contentAlignment = Alignment.Center,
                    ) { Text("$d", color = ZColor.InkSoft, fontSize = 11.sp) }
                }
            }
        }
    }
}

// ---------- Buckets ----------

@Composable private fun BucketsView(list: List<Row>) {
    val cats = Analytics.byCategory(list)
    val total = cats.sumOf { it.second }.coerceAtLeast(1.0)
    Text("Where it went", color = ZColor.Ink, fontSize = 30.sp, fontWeight = FontWeight.Bold)
    if (cats.isEmpty()) { Panel { MutedText("No spending in this view.") }; return }
    Panel {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Donut(cats.map { catColor(it.first) to (it.second / total).toFloat() }, money(total))
        }
        Spacer(Modifier.height(14.dp))
        cats.take(8).forEach { (c, v) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(11.dp).clip(RoundedCornerShape(3.dp)).background(catColor(c)))
                Spacer(Modifier.width(8.dp))
                Text(c, color = ZColor.InkSoft, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text("${(v / total * 100).toInt()}%", color = ZColor.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
    val top = cats.first().second
    cats.forEach { (c, v) ->
        Panel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MerchantTile("", c, 34.dp)
                Spacer(Modifier.width(10.dp))
                Text(c, color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(money(v), color = ZColor.Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp)); Meter((v / top).toFloat(), catColor(c))
        }
    }
}

@Composable private fun Donut(segs: List<Pair<Color, Float>>, centerText: String) {
    Box(Modifier.size(190.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(190.dp)) {
            val stroke = 28.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)
            drawArc(ZColor.Paper, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            var start = -90f
            segs.forEach { (color, frac) ->
                val sweep = frac * 360f
                drawArc(color, start, sweep, false, topLeft, arcSize, style = Stroke(stroke))
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(centerText, color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            MutedText("total", 11)
        }
    }
}

// ---------- Payees ----------

@Composable private fun PayeesView(list: List<Row>) {
    val payees = Analytics.byPayee(list)
    Text("Who you paid", color = ZColor.Ink, fontSize = 30.sp, fontWeight = FontWeight.Bold)
    Panel {
        if (payees.isEmpty()) MutedText("No payments in this view.")
        else payees.forEachIndexed { i, p ->
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("%02d".format(i + 1), color = ZColor.Muted, fontSize = 15.sp, modifier = Modifier.width(28.dp))
                MerchantTile(p.name, p.cats.firstOrNull() ?: "Unknown")
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.name, color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    MutedText("${p.count} ${if (p.count == 1) "payment" else "payments"} · ${p.cats.joinToString(", ")}", 11)
                }
                Spacer(Modifier.width(10.dp))
                Text(money(p.amount), color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ---------- Pace ----------

@Composable private fun PaceView(allRows: List<Row>, list: List<Row>, f: Analytics.Filter, month: String, account: String) {
    Text("Are you ahead of budget?", color = ZColor.Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold)

    if (f.period == "month") {
        val dim = Dates.daysInMonth(month)
        val isCur = month == Dates.todayMonth()
        val upto = if (isCur) Dates.dayOfMonth() else dim
        val run = Analytics.runningTotals(allRows, month, account, upto)
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
            Spacer(Modifier.height(12.dp))
            PaceChart(run, upto, dim, Analytics.BUDGET)
        }
    } else {
        Panel { MutedText("The running total works on a single month. Switch to Month to see it.") }
    }

    // Three months
    val months = Dates.recentMonths(Dates.todayMonth(), 3)
    val data = Analytics.monthlySpendIncome(allRows, months, account)
    val maxV = (data.flatMap { listOf(it.second, it.third) }.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
    Panel {
        Eyebrow("Three months")
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Bottom) {
            data.forEach { (m, spent, income) ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(Modifier.height(120.dp), verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.width(26.dp).fillMaxHeight(((spent / maxV).toFloat()).coerceIn(0.02f, 1f))
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)).background(ZColor.Ink))
                        Box(Modifier.width(26.dp).fillMaxHeight(((income / maxV).toFloat()).coerceIn(0.02f, 1f))
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)).background(ZColor.Income))
                    }
                    Spacer(Modifier.height(6.dp))
                    MutedText(Dates.monthLabel(m), 11)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        MutedText("Ink is spending. Teal is money in.", 12)
    }
}

@Composable private fun PaceChart(run: DoubleArray, upto: Int, dim: Int, budget: Double) {
    val maxY = budget * 1.05
    Canvas(Modifier.fillMaxWidth().height(170.dp)) {
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
