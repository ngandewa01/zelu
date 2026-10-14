package com.nagesh.zelu

import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
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
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { ZeluApp() } }
    }
}

fun money(n: Double): String = "₹" + String.format(Locale.US, "%,.0f", n)

private val RANGE_OPTS = listOf(
    "week" to "This week", "month" to "By month", "3m" to "Last 3 months",
    "6m" to "Last 6 months", "year" to "This year", "all" to "All time", "custom" to "Custom dates",
)
private val TYPE_OPTS = listOf(
    "all" to "Everything", "expense" to "Spending only", "income" to "Money in only",
    "ignore" to "Ignored",
)
private val CAT_OPTS: List<Pair<String, String>> =
    listOf("All" to "All categories") +
        Classifier.CATEGORIES.keys.map { it to it } +
        listOf("Domestic help" to "Domestic help", "Unknown" to "Unknown")

private fun pickDate(context: Context, current: String, onPick: (String) -> Unit) {
    val p = current.split("-")
    val c = Calendar.getInstance()
    try { c.set(p[0].toInt(), p[1].toInt() - 1, p[2].toInt()) } catch (_: Exception) {}
    DatePickerDialog(context, { _, y, m, d -> onPick("%04d-%02d-%02d".format(y, m + 1, d)) },
        c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
}

@Composable private fun LogoMark(sizeDp: Float) {
    Box(
        Modifier.size(sizeDp.dp).clip(RoundedCornerShape((sizeDp * 0.26f).dp)).background(ZColor.Ink),
        contentAlignment = Alignment.Center,
    ) { Text("Z", color = ZColor.Accent, fontWeight = FontWeight.Bold, fontSize = (sizeDp * 0.5f).sp) }
}

@Composable private fun Cta(text: String, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(26.dp)).background(ZColor.Accent)
            .clickable { onClick() }.padding(horizontal = 30.dp, vertical = 13.dp),
    ) { Text(text, color = ZColor.AccentInk, fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }
}

@Composable private fun OnbPoint(title: String, sub: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).clip(RoundedCornerShape(5.dp)).background(ZColor.Accent))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(sub, color = ZColor.Muted, fontSize = 12.sp)
        }
    }
}

@Composable private fun OnboardFlow(stage: String, denied: Boolean, onGetStarted: () -> Unit, onAllow: () -> Unit) {
    Box(Modifier.fillMaxSize().background(ZColor.Paper).padding(28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
            when (stage) {
                "welcome" -> {
                    LogoMark(66f)
                    Text("Your money, from the messages you already get", color = ZColor.Ink,
                        fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    OnbPoint("Reads your bank & UPI alerts", "Spends, income and bills — found for you.")
                    OnbPoint("Stays on your phone", "Nothing uploaded. No account, no login.")
                    OnbPoint("Sorts itself", "Categories, merchants and trends, automatically.")
                    Cta("Get started", onGetStarted)
                }
                "perm" -> {
                    LogoMark(66f)
                    Text("Allow Zelu to read SMS", color = ZColor.Ink, fontSize = 22.sp,
                        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text("So it can turn your bank alerts into spends and bills, in the background.",
                        color = ZColor.Muted, fontSize = 14.sp, textAlign = TextAlign.Center)
                    Text("✓ No OTPs      ✓ No personal chats", color = ZColor.InkSoft,
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Cta("Allow SMS", onAllow)
                    if (denied) Text("Please allow SMS in Settings › Apps › Zelu › Permissions, then reopen Zelu.",
                        color = ZColor.Warn, fontSize = 13.sp, textAlign = TextAlign.Center)
                }
                "scan" -> {
                    LogoMark(66f)
                    Text("Reading your messages…", color = ZColor.Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    CircularProgressIndicator(color = ZColor.Accent)
                }
                else -> { // splash
                    LogoMark(86f)
                    Row {
                        Text("Ze", color = ZColor.Ink, fontWeight = FontWeight.Bold, fontSize = 38.sp)
                        Text("lu", color = ZColor.Accent, fontWeight = FontWeight.Bold, fontSize = 38.sp)
                    }
                    Text("Every rupee, tracked on your phone.", color = ZColor.Muted,
                        fontSize = 14.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun ZeluApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var rows by remember { mutableStateOf<List<Row>>(emptyList()) }
    var reminders by remember { mutableStateOf<List<Reminder>>(emptyList()) }
    var view by remember { mutableStateOf("ledger") }
    var period by remember { mutableStateOf("month") }
    var month by remember { mutableStateOf(Dates.todayMonth()) }
    var account by remember { mutableStateOf("All") }
    var daySel by remember { mutableStateOf(0) }
    var askOpen by remember { mutableStateOf(false) } // category picker open for ask
    var loading by remember { mutableStateOf(false) }
    var type by remember { mutableStateOf("all") }
    var cat by remember { mutableStateOf("All") }
    var excludeEmi by remember { mutableStateOf(false) }
    var controlsOpen by remember { mutableStateOf(false) }
    var customFrom by remember { mutableStateOf(Dates.shiftMonth(Dates.todayMonth(), -2) + "-01") }
    var customTo by remember { mutableStateOf(Dates.today()) }
    var stage by remember { mutableStateOf("splash") } // splash, welcome, perm, scan, app
    var permDenied by remember { mutableStateOf(false) }
    var detailRow by remember { mutableStateOf<Row?>(null) }
    var drill by remember { mutableStateOf<Pair<String, List<Row>>?>(null) }

    fun hasPerm() = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
        PackageManager.PERMISSION_GRANTED

    fun reload() {
        scope.launch {
            rows = withContext(Dispatchers.IO) { Storage.loadRows(context) }
            reminders = withContext(Dispatchers.IO) { Storage.loadReminders(context) }
        }
    }

    fun markReminder(id: String, done: Boolean) {
        scope.launch {
            withContext(Dispatchers.IO) { Storage.setReminderDone(context, id, done) }
            reminders = withContext(Dispatchers.IO) { Storage.loadReminders(context) }
        }
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
            reminders = withContext(Dispatchers.IO) { Storage.loadReminders(context) }
            loading = false
            if (stage == "scan") stage = "app"
        }
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { res -> if (res.values.all { it }) { stage = "scan"; rescan() } else { permDenied = true } }

    LaunchedEffect(Unit) {
        rows = withContext(Dispatchers.IO) { Storage.loadRows(context) }
        reminders = withContext(Dispatchers.IO) { Storage.loadReminders(context) }
        delay(1200)
        if (hasPerm()) { stage = "app"; rescan() } else { stage = "welcome" }
    }

    val accounts = remember(rows) {
        listOf("All") + rows.map { it.account }.filter { it.isNotBlank() }.distinct().sorted()
    }
    // The account filter lists bank accounts only; credit cards are reached via their card tile.
    val bankAccounts = remember(accounts) { accounts.filter { !it.contains(" CC") } }
    val f = Analytics.Filter(period, month, customFrom, customTo, account, type, cat)
    val list = Analytics.filter(rows, f)
    // "Exclude EMI" gives spend-after-EMI: drop EMI rows from the spend views
    // (but not when you're explicitly looking at the EMI category).
    val shown = if (excludeEmi && cat != "EMI") list.filter { it.category != "EMI" } else list

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

    if (stage != "app") {
        OnboardFlow(stage, permDenied, { stage = "perm" }) {
            permLauncher.launch(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS))
        }
        return
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

            // filter bar: arrows + summary pill + collapsible dropdowns
            val rangeLabel = when (period) {
                "week" -> "Last 7 days"; "month" -> Dates.monthLabel(month)
                "3m" -> "Last 3 months"; "6m" -> "Last 6 months"
                "year" -> Dates.today().take(4); "all" -> "All time"
                "custom" -> "$customFrom → $customTo"; else -> ""
            }
            val advActive = type != "all" || cat != "All"
            val summary = rangeLabel + (if (account != "All") " · $account" else "") + (if (advActive) " · filtered" else "")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                if (period == "month") NavButton("‹") { month = Dates.shiftMonth(month, -1); daySel = 0 }
                Row(
                    Modifier.weight(1f).clip(RoundedCornerShape(22.dp)).background(ZColor.Card)
                        .border(1.dp, ZColor.Line, RoundedCornerShape(22.dp))
                        .clickable { controlsOpen = !controlsOpen }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(summary, color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.width(8.dp))
                    Text(if (controlsOpen) "▴" else "▾", color = ZColor.Muted, fontSize = 12.sp)
                }
                if (period == "month") NavButton("›") { month = Dates.shiftMonth(month, 1); daySel = 0 }
            }
            if (controlsOpen) {
                Panel {
                    DropdownField("Range", RANGE_OPTS.first { it.first == period }.second, RANGE_OPTS) { period = it; daySel = 0 }
                    if (period == "custom") {
                        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Dates", color = ZColor.InkSoft, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(92.dp))
                            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                DateChip(customFrom) { pickDate(context, customFrom) { customFrom = it } }
                                DateChip(customTo) { pickDate(context, customTo) { customTo = it } }
                            }
                        }
                    }
                    DropdownField("Account", if (account == "All") "All accounts" else account,
                        bankAccounts.map { it to if (it == "All") "All accounts" else it }) { account = it; daySel = 0 }
                    DropdownField("Show", TYPE_OPTS.first { it.first == type }.second, TYPE_OPTS) { type = it; daySel = 0 }
                    DropdownField("Category", if (cat == "All") "All categories" else cat, CAT_OPTS) { cat = it; daySel = 0 }
                    Spacer(Modifier.height(6.dp))
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .clickable { excludeEmi = !excludeEmi }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(22.dp).clip(RoundedCornerShape(6.dp))
                                .background(if (excludeEmi) ZColor.Accent else ZColor.Paper)
                                .border(1.dp, if (excludeEmi) ZColor.Accent else ZColor.Line, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center,
                        ) { if (excludeEmi) Text("✓", color = ZColor.AccentInk, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Exclude EMI from spending", color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("See what you spend after loan EMIs", color = ZColor.Muted, fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Box(
                        Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                            .clickable { controlsOpen = false }.padding(horizontal = 22.dp, vertical = 10.dp),
                    ) { Text("Done", color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
                }
            }

            if (loading) {
                Spacer(Modifier.height(10.dp)); MutedText("Reading messages…")
            }

            Spacer(Modifier.height(8.dp))
            when (view) {
                "ledger" -> LedgerView(rows, shown, f, accounts, month, account, daySel,
                    { daySel = if (daySel == it) 0 else it }, askOpen, { askOpen = it }, ::decide,
                    { detailRow = it }, { title, r -> drill = title to r }, excludeEmi)
                "buckets" -> BucketsView(shown) { title, rows -> drill = title to rows }
                "payees" -> PayeesView(shown) { title, rows -> drill = title to rows }
                "invest" -> InvestView(rows) { title, r -> drill = title to r }
                "pace" -> PaceView(rows, shown, f, month, account, excludeEmi)
                "dues" -> RemindersView(reminders, ::markReminder)
            }
        }
    }

    drill?.let { (title, rows) ->
        DrillDialog(title, rows, onRowClick = { detailRow = it }, onDismiss = { drill = null })
    }

    detailRow?.let { r ->
        TxnDetailDialog(
            r,
            onReclassify = { kind, cat ->
                decide(r.id, kind, cat, r.counterparty, kind != "income" && kind != "ignore")
                detailRow = null
            },
            onDismiss = { detailRow = null },
        )
    }
}

@Composable private fun DrillDialog(title: String, rows: List<Row>, onRowClick: (Row) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp),
        ) {
            Text(title, color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            val total = rows.sumOf { it.amount }
            MutedText("${rows.size} ${if (rows.size == 1) "entry" else "entries"} · ${money(total)}", 12)
            Spacer(Modifier.height(10.dp))
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                rows.sortedByDescending { it.date }.forEach { TxnRow(it, false, onRowClick) }
            }
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier.align(Alignment.End).clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                    .clickable { onDismiss() }.padding(horizontal = 22.dp, vertical = 10.dp),
            ) { Text("Close", color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable private fun TxnDetailDialog(t: Row, onReclassify: (String, String) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MerchantTile(t.counterparty ?: t.category, t.category, kind = t.kind)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(t.counterparty?.takeIf { it.isNotBlank() } ?: t.category, color = ZColor.Ink,
                        fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    MutedText("${t.date} · ${subtitleFor(t)}", 12)
                }
                Spacer(Modifier.width(10.dp))
                AmountText(t, false)
            }
            if (t.account.isNotBlank()) { Spacer(Modifier.height(8.dp)); MutedText("Account: ${t.account}", 12) }
            Spacer(Modifier.height(16.dp))
            Eyebrow("From this message")
            Spacer(Modifier.height(6.dp))
            Text(
                t.raw.ifBlank { "(no message text stored)" }, color = ZColor.InkSoft, fontSize = 13.sp,
                modifier = Modifier.heightIn(max = 280.dp).verticalScroll(rememberScrollState()),
            )
            Spacer(Modifier.height(16.dp))
            var editing by remember { mutableStateOf(false) }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.clip(RoundedCornerShape(20.dp)).border(1.dp, ZColor.Line, RoundedCornerShape(20.dp))
                        .clickable { editing = !editing }.padding(horizontal = 16.dp, vertical = 9.dp),
                ) { Text(if (editing) "Cancel" else "Change category", color = ZColor.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                Box(
                    Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                        .clickable { onDismiss() }.padding(horizontal = 22.dp, vertical = 10.dp),
                ) { Text("Close", color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
            }
            if (editing) {
                Spacer(Modifier.height(10.dp))
                MutedText("Remembered for ${t.counterparty ?: "this payee"}:", 12)
                Spacer(Modifier.height(6.dp))
                ChipScroll {
                    Classifier.CATEGORIES.keys.forEach { c -> Chip(c, false) { onReclassify(if (c == "Investment") "invest" else "expense", c) } }
                }
                Spacer(Modifier.height(6.dp))
                ChipScroll {
                    Chip("Money in", false) { onReclassify("income", "Income") }
                    Chip("Lent", false) { onReclassify("lent", "Lent") }
                    Chip("Borrowed", false) { onReclassify("borrowed", "Borrowed") }
                    Chip("Ignore", false) { onReclassify("ignore", "Ignore") }
                }
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
    val items = listOf("ledger" to "Ledger", "pace" to "Pace", "buckets" to "Buckets", "payees" to "Payees", "invest" to "Invest", "dues" to "Dues")
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(ZColor.Card)
            .border(1.dp, ZColor.Line, RoundedCornerShape(14.dp)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items.forEach { (k, lbl) ->
            val sel = view == k
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                    .background(if (sel) ZColor.Ink else Color.Transparent)
                    .clickable { onPick(k) }.padding(vertical = 9.dp, horizontal = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(lbl, color = if (sel) ZColor.Paper else ZColor.Muted,
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}

@Composable private fun NavButton(symbol: String, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(RoundedCornerShape(20.dp)).background(ZColor.Card)
            .border(1.dp, ZColor.Line, RoundedCornerShape(20.dp)).clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { Text(symbol, color = ZColor.Ink, fontSize = 20.sp) }
}

@Composable private fun DateChip(value: String, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(10.dp)).background(ZColor.Card)
            .border(1.dp, ZColor.Line, RoundedCornerShape(10.dp)).clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 9.dp),
    ) { Text(value, color = ZColor.Ink, fontSize = 13.sp) }
}

@Composable private fun DropdownField(
    label: String, valueLabel: String, options: List<Pair<String, String>>, onPick: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = ZColor.InkSoft, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(92.dp))
        Box(Modifier.weight(1f)) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(ZColor.Card)
                    .border(1.dp, ZColor.Line, RoundedCornerShape(12.dp))
                    .clickable { expanded = true }.padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(valueLabel, color = ZColor.Ink, fontSize = 14.sp, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text("▾", color = ZColor.Muted, fontSize = 12.sp)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (v, l) ->
                    DropdownMenuItem(text = { Text(l, color = ZColor.Ink) },
                        onClick = { onPick(v); expanded = false })
                }
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

@Composable private fun TxnRow(t: Row, highlight: Boolean, onClick: (Row) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick(t) }.padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically) {
        MerchantTile(t.counterparty ?: t.category, t.category, kind = t.kind)
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
    "invest" -> "Investment"
    "ignore" -> "Ignored"
    else -> t.kind.replaceFirstChar { it.uppercase() }
}

@Composable private fun AmountText(t: Row, highlight: Boolean) {
    when (t.kind) {
        "transfer" -> Text("Transfer", color = ZColor.Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        "invest" -> Text(money(t.amount), color = ZColor.Muted, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
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
    onRowClick: (Row) -> Unit,
    onDrill: (String, List<Row>) -> Unit,
    excludeEmi: Boolean = false,
) {
    val s = Analytics.summarize(list)
    val ins = Analytics.insight(list, f)
    val bigIds = Analytics.bigSpendIds(list)
    val pct = (s.spent / Analytics.BUDGET).toFloat()
    val budgetLeft = (Analytics.BUDGET - s.spent).coerceAtLeast(0.0)

    // Spent + budget
    Panel {
        Eyebrow("Spent in " + Analytics.periodLabel(f) + if (excludeEmi) " · after EMI" else "")
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
            Spacer(Modifier.height(14.dp))
            val ob = ins.overBy
            MutedText("Biggest spend", 12)
            Text("${ins.biggestName} · ${money(ins.biggestAmt)}", color = ZColor.Ink,
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (ob != null) {
                Spacer(Modifier.height(12.dp))
                val over = ob > 0
                MutedText(if (over) "Over a steady pace by" else "Under a steady pace by", 12)
                Text(money(kotlin.math.abs(ob)), color = if (over) ZColor.Accent else ZColor.Income,
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            if (ob != null && ob > 0) {
                Spacer(Modifier.height(10.dp))
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
    val cardAccounts = accounts.filter { it.contains(" CC ") || it.startsWith("CC ") || it.endsWith(" CC") }
    if (cardAccounts.isNotEmpty()) {
        Panel {
            Eyebrow("Cards")
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                cardAccounts.forEach { a ->
                    // Card totals follow only the date range, not the Show/Category filter.
                    val cardRows = Analytics.filter(allRows, f.copy(account = a, type = "all", category = "All"))
                    val spent = cardRows.filter { it.kind == "expense" }.sumOf { it.amount }
                    CardTile(a, spent) { onDrill(a, cardRows) }
                }
            }
            Spacer(Modifier.height(10.dp))
            MutedText("Tap a card to see its spends. Card spends count in your totals; paying the bill is a transfer.", 11)
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
                else dayRows.forEach { TxnRow(it, bigIds.contains(it.id), onRowClick) }
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
                        Chip(c, false) { decide(next.id, if (c == "Investment") "invest" else "expense", c, next.counterparty, true) }
                    }
                    Chip("Domestic help", false) { decide(next.id, "expense", "Domestic help", next.counterparty, true) }
                }
            }
        }
    }

    // Latest
    Panel {
        Eyebrow("Latest")
        val latest = list.filter { it.kind != "invest" }.sortedByDescending { it.date }.take(30)
        if (latest.isEmpty()) { Spacer(Modifier.height(6.dp)); MutedText("No transactions here.") }
        else latest.forEach { TxnRow(it, bigIds.contains(it.id), onRowClick) }
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

@Composable private fun CardTile(account: String, spent: Double, onClick: () -> Unit = {}) {
    val last4 = account.takeLast(4)
    Column(
        Modifier.width(240.dp).clip(RoundedCornerShape(16.dp)).background(ZColor.Paper)
            .clickable { onClick() }.padding(16.dp),
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

@Composable private fun BucketsView(list: List<Row>, onDrill: (String, List<Row>) -> Unit) {
    val cats = Analytics.byCategory(list)
    val total = cats.sumOf { it.second }.coerceAtLeast(1.0)
    Text("Where it went", color = ZColor.Ink, fontSize = 30.sp, fontWeight = FontWeight.Bold)
    if (cats.isEmpty()) { Panel { MutedText("No spending in this view.") }; return }
    fun rowsFor(c: String) = list.filter { it.kind == "expense" && it.category == c }
    Panel {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Donut(cats.map { catColor(it.first) to (it.second / total).toFloat() }, money(total)) { i ->
                onDrill(cats[i].first, rowsFor(cats[i].first))
            }
        }
        Spacer(Modifier.height(6.dp))
        MutedText("Tap a slice or a card to see what's inside.", 11)
    }
    val top = cats.first().second
    Panel {
        Eyebrow("By category")
        cats.forEachIndexed { i, (c, v) ->
            Spacer(Modifier.height(if (i == 0) 8.dp else 14.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onDrill(c, rowsFor(c)) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MerchantTile("", c, 32.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(c, color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(money(v), color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(" ${(v / total * 100).toInt()}%", color = ZColor.Muted, fontSize = 12.sp,
                        modifier = Modifier.width(42.dp), textAlign = TextAlign.End)
                }
                Spacer(Modifier.height(7.dp))
                Meter((v / top).toFloat(), catColor(c))
            }
        }
    }
}

@Composable private fun Donut(segs: List<Pair<Color, Float>>, centerText: String, onSliceTap: (Int) -> Unit = {}) {
    Box(Modifier.size(190.dp), contentAlignment = Alignment.Center) {
        Canvas(
            Modifier.size(190.dp).pointerInput(segs) {
                detectTapGestures { off ->
                    val cx = size.width / 2f; val cy = size.height / 2f
                    val ang = Math.toDegrees(atan2((off.y - cy).toDouble(), (off.x - cx).toDouble())).toFloat()
                    val a = (ang + 90f + 360f) % 360f
                    var acc = 0f
                    for (i in segs.indices) {
                        val sweep = segs[i].second * 360f
                        if (a >= acc && a < acc + sweep) { onSliceTap(i); break }
                        acc += sweep
                    }
                }
            }
        ) {
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

@Composable private fun PayeesView(list: List<Row>, onDrill: (String, List<Row>) -> Unit) {
    val payees = Analytics.byPayee(list)
    Text("Who you paid", color = ZColor.Ink, fontSize = 30.sp, fontWeight = FontWeight.Bold)
    Panel {
        if (payees.isEmpty()) MutedText("No payments in this view.")
        else payees.forEachIndexed { i, p ->
            val rows = list.filter {
                it.kind == "expense" && (it.counterparty?.takeIf { c -> c.isNotBlank() } ?: it.category) == p.name
            }
            Row(
                Modifier.fillMaxWidth().clickable { onDrill(p.name, rows) }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
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

// ---------- Investments ----------

@Composable private fun InvestView(allRows: List<Row>, onDrill: (String, List<Row>) -> Unit) {
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

private val MONTH_ABBR = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
private fun prettyDate(iso: String): String = try {
    val p = iso.split("-"); "${p[2]} ${MONTH_ABBR[p[1].toInt() - 1]} ${p[0].takeLast(2)}"
} catch (_: Exception) { iso }

@Composable private fun RemindersView(reminders: List<Reminder>, onMark: (String, Boolean) -> Unit) {
    var tab by remember { mutableStateOf("upcoming") }
    val today = Dates.today()
    val pending = reminders.filter { !it.done }
    val upcoming = pending.filter { it.dueDate >= today }.sortedBy { it.dueDate }
    val overdue = pending.filter { it.dueDate < today }.sortedByDescending { it.dueDate }
    val paid = reminders.filter { it.done }.sortedByDescending { it.dueDate }

    Spacer(Modifier.height(4.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SegTab("Upcoming", upcoming.size, tab == "upcoming", Modifier.weight(1f)) { tab = "upcoming" }
        SegTab("Overdue", overdue.size, tab == "overdue", Modifier.weight(1f)) { tab = "overdue" }
    }
    val list = if (tab == "upcoming") upcoming else overdue
    if (list.isEmpty()) {
        Panel {
            Eyebrow(if (tab == "upcoming") "Upcoming" else "Overdue")
            Spacer(Modifier.height(8.dp))
            MutedText(
                if (tab == "upcoming") "No upcoming bills. Bill and EMI reminders from your SMS appear here."
                else "Nothing overdue.",
            )
        }
    } else {
        list.forEach { r -> ReminderCard(r, overdue = tab == "overdue") { onMark(r.id, true) } }
    }
    if (paid.isNotEmpty()) {
        Spacer(Modifier.height(18.dp))
        Eyebrow("Paid")
        paid.take(20).forEach { r -> ReminderCard(r, overdue = false, paid = true) { onMark(r.id, false) } }
    }
    Spacer(Modifier.height(6.dp))
    MutedText("Reminders are never counted as spending — only money that actually leaves your bank is.", 11)
}

@Composable private fun SegTab(label: String, count: Int, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(14.dp))
            .background(if (selected) ZColor.Ink else ZColor.Card)
            .border(1.dp, if (selected) ZColor.Ink else ZColor.Line, RoundedCornerShape(14.dp))
            .clickable { onClick() }.padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label + (if (count > 0) "  ·  $count" else ""),
            color = if (selected) ZColor.Paper else ZColor.Muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable private fun ReminderCard(r: Reminder, overdue: Boolean, paid: Boolean = false, onTick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 10.dp).clip(RoundedCornerShape(16.dp))
            .background(ZColor.Card).border(1.dp, ZColor.Line, RoundedCornerShape(16.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MerchantTile(r.counterparty ?: "Bill", "Bills", 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(r.counterparty?.takeIf { it.isNotBlank() } ?: r.account.ifBlank { "Bill" },
                color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(money(r.amount), color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                if (r.minAmount != null) { Spacer(Modifier.width(6.dp)); MutedText("Min ${money(r.minAmount)}", 12) }
            }
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text((if (overdue) "Due " else "") + prettyDate(r.dueDate),
                    color = if (overdue) ZColor.Warn else ZColor.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                if (r.recurring) { Spacer(Modifier.width(8.dp)); Pill("Monthly") }
                if (r.account.isNotBlank()) { Spacer(Modifier.width(8.dp)); MutedText(r.account, 11) }
            }
        }
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(17.dp))
                .background(if (paid) ZColor.Income.copy(alpha = 0.15f) else Color.Transparent)
                .border(1.5.dp, if (paid) ZColor.Income else if (overdue) ZColor.Warn else ZColor.Line, RoundedCornerShape(17.dp))
                .clickable { onTick() },
            contentAlignment = Alignment.Center,
        ) { Text("✓", color = if (paid) ZColor.Income else ZColor.Muted, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
    }
}

@Composable private fun Pill(text: String) {
    Box(
        Modifier.clip(RoundedCornerShape(8.dp)).background(ZColor.Paper)
            .border(1.dp, ZColor.Line, RoundedCornerShape(8.dp)).padding(horizontal = 7.dp, vertical = 2.dp),
    ) { Text(text, color = ZColor.InkSoft, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
}

// ---------- Pace ----------

@Composable private fun PaceView(
    allRows: List<Row>, list: List<Row>, f: Analytics.Filter, month: String, account: String,
    excludeEmi: Boolean = false,
) {
    // Respect the "Exclude EMI" toggle for the running-total and monthly charts too.
    val baseRows = if (excludeEmi) allRows.filter { it.category != "EMI" } else allRows
    Text("Are you ahead of budget?", color = ZColor.Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold)

    if (f.period == "month") {
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
            Spacer(Modifier.height(12.dp))
            PaceChart(run, upto, dim, Analytics.BUDGET)
        }
    } else {
        Panel { MutedText("The running total works on a single month. Switch to Month to see it.") }
    }

    // Three months
    val months = Dates.recentMonths(Dates.todayMonth(), 3)
    val data = Analytics.monthlySpendIncome(baseRows, months, account)
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
