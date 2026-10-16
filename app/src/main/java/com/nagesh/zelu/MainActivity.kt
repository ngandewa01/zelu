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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.runtime.Composable
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

@Composable private fun OnboardFlow(
    stage: String, denied: Boolean, stats: Triple<Int, Int, Int>,
    onBack: () -> Unit, onNext: () -> Unit, onAllow: () -> Unit,
) {
    val step = when (stage) { "welcome" -> 0; "privacy" -> 1; "perm" -> 2; else -> -1 }
    Box(Modifier.fillMaxSize().background(ZColor.Paper).padding(28.dp)) {
        if (step >= 0) Row(Modifier.align(Alignment.TopCenter).padding(top = 20.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(18.dp)).clickable(enabled = step > 0) { onBack() }, contentAlignment = Alignment.Center) {
                if (step > 0) Text("‹", color = ZColor.Ink, fontSize = 24.sp)
            }
            Spacer(Modifier.width(10.dp))
            (0..2).forEach { i ->
                Box(Modifier.weight(1f).padding(horizontal = 3.dp).height(4.dp).clip(RoundedCornerShape(2.dp))
                    .background(if (i <= step) ZColor.Ink else ZColor.Line))
            }
            Spacer(Modifier.width(46.dp))
        }
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
            when (stage) {
                "welcome" -> {
                    var shown by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { delay(600); shown = true }
                    val a by animateFloatAsState(if (shown) 1f else 0f, tween(1000), label = "sms")
                    Box(Modifier.fillMaxWidth().graphicsLayer { alpha = 1f - 0.55f * a }.clip(RoundedCornerShape(16.dp))
                        .background(ZColor.Card).border(1.dp, ZColor.Line, RoundedCornerShape(16.dp)).padding(14.dp)) {
                        Column {
                            Text("HDFCBK", color = ZColor.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("Rs.450 spent on card 9065 at SWIGGY on 16-10-26", color = ZColor.Ink, fontSize = 13.sp)
                        }
                    }
                    Text("↓", color = ZColor.Accent, fontSize = 24.sp, modifier = Modifier.graphicsLayer { alpha = a })
                    Row(Modifier.fillMaxWidth().graphicsLayer { alpha = a; translationY = (1f - a) * 30f }.clip(RoundedCornerShape(16.dp))
                        .background(ZColor.Card).border(1.dp, ZColor.Line, RoundedCornerShape(16.dp)).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        MerchantTile("Swiggy", "Food", 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Swiggy", color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            MutedText("16 Oct · Food", 11)
                        }
                        Text("−₹450", color = ZColor.Warn, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Your bank texts already know where your money went.", color = ZColor.Ink,
                        fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Cta("Get started", onNext)
                }
                "privacy" -> {
                    Text("Nothing leaves your phone.", color = ZColor.Ink, fontSize = 24.sp,
                        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf(
                            Triple("Reads", ZColor.Income, listOf("Bank & card alerts", "UPI payment messages", "Bill due reminders")),
                            Triple("Never reads", ZColor.Warn, listOf("OTPs", "Personal chats", "Anything from contacts")),
                        ).forEach { (title, color, items) ->
                            Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(ZColor.Card)
                                .border(1.dp, ZColor.Line, RoundedCornerShape(16.dp)).padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(title, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                items.forEach { Text((if (title == "Reads") "✓ " else "✕ ") + it, color = ZColor.InkSoft, fontSize = 13.sp) }
                            }
                        }
                    }
                    Text("No account. No login. No upload.", color = ZColor.Muted, fontSize = 14.sp)
                    Cta("Continue", onNext)
                }
                "perm" -> {
                    Text("Two quick permissions", color = ZColor.Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    listOf(
                        "SMS" to "Read your bank alerts and turn them into spends and bills.",
                        "Notifications" to "Bill due reminders and a nudge for each payment. Optional.",
                    ).forEach { (t, d) ->
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ZColor.Card)
                            .border(1.dp, ZColor.Line, RoundedCornerShape(16.dp)).padding(14.dp)) {
                            Text(t, color = ZColor.Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            MutedText(d, 13)
                        }
                    }
                    Cta("Allow both", onAllow)
                    if (denied) Text("SMS is needed to read your bank alerts. Allow it in Settings › Apps › Zelu › Permissions, then reopen Zelu.",
                        color = ZColor.Warn, fontSize = 13.sp, textAlign = TextAlign.Center)
                }
                "scan" -> {
                    LogoMark(66f)
                    Text("Reading your messages…", color = ZColor.Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    CircularProgressIndicator(color = ZColor.Accent)
                }
                "done" -> {
                    var go by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { go = true }
                    val p by animateIntAsState(if (go) stats.first else 0, tween(1400), label = "p")
                    val b by animateIntAsState(if (go) stats.second else 0, tween(1400), label = "b")
                    val c by animateIntAsState(if (go) stats.third else 0, tween(1400), label = "c")
                    Text("All set.", color = ZColor.Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text("%,d".format(p), color = ZColor.Accent, fontSize = 52.sp, fontWeight = FontWeight.Bold)
                    Text("payments found", color = ZColor.Muted, fontSize = 15.sp)
                    Text("$b bank account${if (b == 1) "" else "s"} · $c card${if (c == 1) "" else "s"}",
                        color = ZColor.Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Cta("Open Zelu", onNext)
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
    var reminderState by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var repaid by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var budgets by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var budgetsOpen by remember { mutableStateOf(false) }
    var savingsOpen by remember { mutableStateOf(false) }
    var trendsOpen by remember { mutableStateOf(false) }
    var fixed by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var menuOpen by remember { mutableStateOf(false) }
    var rangeOpen by remember { mutableStateOf(false) }
    var newCatCallback by remember { mutableStateOf<((String) -> Unit)?>(null) }
    var balances by remember { mutableStateOf<Map<String, Pair<Double, Long>>>(emptyMap()) }
    var limits by remember { mutableStateOf<Map<String, Pair<Double, Long>>>(emptyMap()) }
    var cardPrefs by remember { mutableStateOf<Map<String, Pair<Int, Double>>>(emptyMap()) }
    var cardOpen by remember { mutableStateOf<String?>(null) }
    var monthlyBudget by remember { mutableStateOf(60000.0) }
    var budgetDlg by remember { mutableStateOf(false) }
    Analytics.BUDGET = monthlyBudget
    var lendPerson by remember { mutableStateOf<String?>(null) }
    var borrowPerson by remember { mutableStateOf<String?>(null) }
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
    var query by remember { mutableStateOf("") }
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
            reminderState = withContext(Dispatchers.IO) { Storage.loadReminderState(context) }
            repaid = withContext(Dispatchers.IO) { Storage.loadRepaid(context) }
            budgets = withContext(Dispatchers.IO) { Storage.loadBudgets(context) }
            fixed = withContext(Dispatchers.IO) { Storage.loadFixed(context) }
            balances = withContext(Dispatchers.IO) { Storage.loadBalances(context) }
            limits = withContext(Dispatchers.IO) { Storage.loadLimits(context) }
            cardPrefs = withContext(Dispatchers.IO) { Storage.loadCardPrefs(context) }
            CustomCats.map = withContext(Dispatchers.IO) { Storage.loadCustomCats(context) }
        }
    }

    fun setFixed(name: String, amount: Double) {
        scope.launch {
            withContext(Dispatchers.IO) { Storage.setFixed(context, name, amount) }
            fixed = withContext(Dispatchers.IO) { Storage.loadFixed(context) }
        }
    }

    fun answerRefund(refundId: String, yes: Boolean) {
        scope.launch {
            withContext(Dispatchers.IO) { Storage.setRefundLink(context, refundId, yes) }
            rows = withContext(Dispatchers.IO) { Storage.loadRows(context) }
        }
    }

    fun setBudget(category: String, amount: Double) {
        scope.launch {
            withContext(Dispatchers.IO) { Storage.setBudget(context, category, amount) }
            budgets = withContext(Dispatchers.IO) { Storage.loadBudgets(context) }
        }
    }

    fun setCardPref(account: String, day: Int, budget: Double) {
        scope.launch {
            withContext(Dispatchers.IO) { Storage.setCardPref(context, account, day, budget) }
            cardPrefs = withContext(Dispatchers.IO) { Storage.loadCardPrefs(context) }
            monthlyBudget = withContext(Dispatchers.IO) { Storage.loadMonthlyBudget(context) }
        }
    }

    fun markReminder(id: String, done: Boolean) {
        scope.launch {
            withContext(Dispatchers.IO) { Storage.setReminderDone(context, id, done) }
            reminderState = withContext(Dispatchers.IO) { Storage.loadReminderState(context) }
            reminders = withContext(Dispatchers.IO) { Storage.loadReminders(context) }
        }
    }

    fun setRepayment(personKey: String, amount: Double) {
        scope.launch {
            withContext(Dispatchers.IO) { Storage.setRepaid(context, personKey, amount) }
            repaid = withContext(Dispatchers.IO) { Storage.loadRepaid(context) }
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
            // Refresh everything (balances, card limits, fixed plan, prefs too), not just the rows.
            reload()
            loading = false
            if (stage == "scan") stage = "done"
        }
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { res ->
        val smsOk = res[Manifest.permission.READ_SMS] == true && res[Manifest.permission.RECEIVE_SMS] == true
        if (smsOk) { stage = "scan"; rescan() } else { permDenied = true }
    }


    // Best-effort local reminder on open: nudge about bills due within 3 days.
    LaunchedEffect(stage, reminders) {
        if (stage != "app") return@LaunchedEffect
        // Daily 9 AM check runs in the background (DueReceiver); this just makes sure it's scheduled.
        DueReminders.schedule(context)
    }

    LaunchedEffect(Unit) {
        rows = withContext(Dispatchers.IO) { Storage.loadRows(context) }
        reminders = withContext(Dispatchers.IO) { Storage.loadReminders(context) }
        reminderState = withContext(Dispatchers.IO) { Storage.loadReminderState(context) }
        repaid = withContext(Dispatchers.IO) { Storage.loadRepaid(context) }
        budgets = withContext(Dispatchers.IO) { Storage.loadBudgets(context) }
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
    val q = query.trim().lowercase()
    val shown = (if (excludeEmi && cat != "EMI") list.filter { it.category != "EMI" } else list)
        .let { base ->
            if (q.isBlank()) base else base.filter { r ->
                (r.counterparty ?: "").lowercase().contains(q) ||
                    r.category.lowercase().contains(q) ||
                    r.amount.toLong().toString().contains(q)
            }
        }

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
        val accts = rows.map { it.account }.filter { it.isNotBlank() }.distinct()
        val cardsN = accts.count { it.contains(" CC") }
        OnboardFlow(stage, permDenied, Triple(rows.size, accts.size - cardsN, cardsN),
            onBack = { stage = if (stage == "perm") "privacy" else "welcome" },
            onNext = { stage = when (stage) { "welcome" -> "privacy"; "privacy" -> "perm"; "done" -> "app"; else -> stage } },
            onAllow = {
                val perms = mutableListOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
                if (Build.VERSION.SDK_INT >= 33) perms.add("android.permission.POST_NOTIFICATIONS")
                permLauncher.launch(perms.toTypedArray())
            })
        return
    }

    Box(Modifier.fillMaxSize().background(ZColor.Paper)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, top = 46.dp, bottom = 110.dp),
        ) {
            val advActiveTop = type != "all" || cat != "All" || query.isNotBlank() || account != "All" || excludeEmi
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                BrandBar()
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TopIcon(onClick = { controlsOpen = true }, dot = advActiveTop) { FunnelIcon() }
                    TopIcon(onClick = { menuOpen = true }) { MenuIcon() }
                }
            }
            Spacer(Modifier.height(14.dp))

            // filter bar: arrows + summary pill + collapsible dropdowns
            val rangeLabel = when (period) {
                "week" -> "Last 7 days"; "month" -> Dates.monthLabel(month)
                "3m" -> "Last 3 months"; "6m" -> "Last 6 months"
                "year" -> Dates.today().take(4); "all" -> "All time"
                "custom" -> "$customFrom → $customTo"; else -> ""
            }
            val advActive = type != "all" || cat != "All" || query.isNotBlank()
            val summary = rangeLabel
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                if (period == "month") NavButton("‹") { month = Dates.shiftMonth(month, -1); daySel = 0 }
                Row(
                    Modifier.weight(1f).clip(RoundedCornerShape(22.dp)).background(ZColor.Card)
                        .border(1.dp, ZColor.Line, RoundedCornerShape(22.dp))
                        .clickable { rangeOpen = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(summary, color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.width(8.dp))
                    Text("▾", color = ZColor.Muted, fontSize = 12.sp)
                }
                if (period == "month") NavButton("›") { month = Dates.shiftMonth(month, 1); daySel = 0 }
            }
            if (rangeOpen) Dialog(onDismissRequest = { rangeOpen = false }) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(vertical = 10.dp)) {
                    Text("Date range", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp))
                    RANGE_OPTS.forEach { (k, label) ->
                        Row(
                            Modifier.fillMaxWidth().clickable { period = k; daySel = 0; if (k != "custom") rangeOpen = false }
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(label, color = ZColor.Ink, fontSize = 15.sp,
                                fontWeight = if (k == period) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
                            if (k == period) Text("✓", color = ZColor.Accent, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (period == "custom") {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DateChip(customFrom) { pickDate(context, customFrom) { customFrom = it } }
                            DateChip(customTo) { pickDate(context, customTo) { customTo = it } }
                        }
                        Box(
                            Modifier.align(Alignment.End).padding(end = 20.dp, top = 4.dp).clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                                .clickable { rangeOpen = false }.padding(horizontal = 22.dp, vertical = 10.dp),
                        ) { Text("Done", color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
                    }
                }
            }
            if (controlsOpen) Dialog(onDismissRequest = { controlsOpen = false }) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(18.dp)
                    .heightIn(max = 640.dp).verticalScroll(rememberScrollState())) {
                    Text("Filters", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    MutedText("Account", 12)
                    Spacer(Modifier.height(6.dp))
                    AccountChips(bankAccounts, account) { account = it; daySel = 0 }
                    Spacer(Modifier.height(6.dp))
                    DropdownField("Show", TYPE_OPTS.first { it.first == type }.second, TYPE_OPTS) { type = it; daySel = 0 }
                    DropdownField("Category", if (cat == "All") "All categories" else cat, CAT_OPTS) { cat = it; daySel = 0 }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = query, onValueChange = { query = it }, singleLine = true,
                        placeholder = { Text("Search payee or amount") },
                        modifier = Modifier.fillMaxWidth(),
                    )
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.clip(RoundedCornerShape(20.dp)).border(1.dp, ZColor.Line, RoundedCornerShape(20.dp))
                                .clickable {
                                    val ok = Exporter.exportCsv(context, shown, "zelu-${Dates.today()}.csv")
                                    Toast.makeText(context,
                                        if (ok) "Saved to Downloads/Zelu" else "Export needs Android 10+",
                                        Toast.LENGTH_SHORT).show()
                                }.padding(horizontal = 16.dp, vertical = 10.dp),
                        ) { Text("Export CSV", color = ZColor.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                        Spacer(Modifier.width(10.dp))
                        Box(
                            Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                                .clickable { controlsOpen = false }.padding(horizontal = 22.dp, vertical = 10.dp),
                        ) { Text("Done", color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
                    }
                }
            }

            if (loading) {
                Spacer(Modifier.height(10.dp)); MutedText("Reading messages…")
            }

            Spacer(Modifier.height(8.dp))
            when (view) {
                "ledger" -> LedgerView(rows, shown, f, accounts, month, account, daySel,
                    { daySel = if (daySel == it) 0 else it }, askOpen, { askOpen = it }, ::decide,
                    { detailRow = it }, { title, r -> drill = title to r }, excludeEmi,
                    repaid, { lendPerson = it }, { borrowPerson = it }, fixed, { budgetsOpen = true }, balances, { cb -> newCatCallback = cb }, limits, reminders, { cardOpen = it }, { excludeEmi = !excludeEmi }, { budgetDlg = true }, { view = "dues" }, reminderState, Refunds.suggestions, { id, yes -> answerRefund(id, yes) })
                "spend" -> {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Row(
                            Modifier.clip(RoundedCornerShape(20.dp)).border(1.dp, ZColor.Line, RoundedCornerShape(20.dp))
                                .clickable { trendsOpen = true }.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            androidx.compose.material3.Icon(Icons.Rounded.BarChart, null, tint = ZColor.Accent, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Trends", color = ZColor.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    val prevByCat = if (period == "month")
                        Analytics.categoryTotals(rows, Dates.shiftMonth(month, -1), account) else emptyMap()
                    BucketsView(shown, prevByCat, budgets, { budgetsOpen = true }) { title, rows -> drill = title to rows }
                    PaceView(rows, shown, f, month, account, excludeEmi)
                    if (f.period == "month") DailyCalendarBlock(rows, shown, f, month, account, daySel,
                        { daySel = if (daySel == it) 0 else it }, { detailRow = it })
                }
                "buckets" -> {
                    val prevByCat = if (period == "month")
                        Analytics.categoryTotals(rows, Dates.shiftMonth(month, -1), account) else emptyMap()
                    BucketsView(shown, prevByCat, budgets, { budgetsOpen = true }) { title, rows -> drill = title to rows }
                }
                "payees" -> PayeesView(shown) { title, rows -> drill = title to rows }
                "invest" -> InvestView(rows) { title, r -> drill = title to r }
                "pace" -> PaceView(rows, shown, f, month, account, excludeEmi)
                "dues" -> RemindersView(reminders, rows, reminderState, ::markReminder)
            }
        }
        BottomNav(view, Modifier.align(Alignment.BottomCenter)) { view = it; daySel = 0 }
        androidx.activity.compose.BackHandler(trendsOpen) { trendsOpen = false }
        if (trendsOpen) TrendsScreen(rows, accounts, onBack = { trendsOpen = false }, onReview = { t, r -> drill = t to r })
        cardOpen?.let { a ->
            CardDetailScreen(
                account = a, allRows = rows, limit = limits[a]?.first, reminders = reminders,
                reminderState = reminderState, pref = cardPrefs[a],
                onSavePref = { d, b -> setCardPref(a, d, b) },
                onMarkPaid = { id -> markReminder(id, true) },
                onRowClick = { detailRow = it }, onBack = { cardOpen = null },
            )
        }
    }

    if (budgetDlg) {
        var text by remember { mutableStateOf(fmtPlain(monthlyBudget)) }
        Dialog(onDismissRequest = { budgetDlg = false }) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp)) {
                Text("Monthly budget", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                MutedText("Your overall spending limit per month. Shown on Home and used for the pace chart.", 12)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = text, onValueChange = { v -> text = v.filter { it.isDigit() || it == '.' } },
                    singleLine = true, placeholder = { Text("₹ per month") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(14.dp))
                Box(
                    Modifier.align(Alignment.End).clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                        .clickable {
                            val v = text.toDoubleOrNull() ?: 0.0
                            if (v > 0) scope.launch {
                                withContext(Dispatchers.IO) { Storage.saveMonthlyBudget(context, v) }
                                monthlyBudget = v
                            }
                            budgetDlg = false
                        }.padding(horizontal = 22.dp, vertical = 10.dp),
                ) { Text("Save", color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
    if (menuOpen) {
        Dialog(onDismissRequest = { menuOpen = false }) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(vertical = 10.dp)) {
                Text("Menu", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp))
                MenuRow("Monthly budget", "Overall spending limit for the month") { menuOpen = false; budgetDlg = true }
                MenuRow("Budgets", "Monthly caps per category · mark fixed costs") { menuOpen = false; budgetsOpen = true }
                MenuRow("Savings plan", "What's left each month · SIP, EMI and goal planner") { menuOpen = false; savingsOpen = true }
                MenuRow("Investments", "EPF, mutual funds, SIPs") { menuOpen = false; view = "invest"; daySel = 0 }
                MenuRow("Export CSV", "Save transactions to Downloads/Zelu") {
                    menuOpen = false
                    val ok = Exporter.exportCsv(context, shown, "zelu-${Dates.today()}.csv")
                    Toast.makeText(context, if (ok) "Saved to Downloads/Zelu" else "Export needs Android 10+", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    drill?.let { (title, rows) ->
        DrillDialog(title, rows, onRowClick = { detailRow = it }, onDismiss = { drill = null })
    }

    if (savingsOpen) SavingsDialog(rows, onDismiss = { savingsOpen = false })
    if (budgetsOpen) {
        val spendCats = Analytics.byCategory(list).map { it.first }
        val allCats = (fixed.keys + spendCats + budgets.keys + Classifier.CATEGORIES.keys + CustomCats.names).distinct()
        BudgetsDialog(allCats, budgets, fixed, onSet = { c, a -> setBudget(c, a) }, onFixed = { c, a -> setFixed(c, a) },
            onDismiss = { budgetsOpen = false })
    }

    lendPerson?.let { person ->
        val pRows = rows.filter { it.kind == "lent" && (it.counterparty ?: "Someone") == person }
        LendingDialog(
            person = person,
            lentRows = pRows,
            repaidSoFar = repaid[person.lowercase().trim()] ?: 0.0,
            onRowClick = { detailRow = it },
            onSave = { amt -> setRepayment(person.lowercase().trim(), amt) },
            onDismiss = { lendPerson = null },
        )
    }

    borrowPerson?.let { person ->
        val pRows = rows.filter { it.kind == "borrowed" && (it.counterparty ?: "Someone") == person }
        LendingDialog(
            person = person,
            lentRows = pRows,
            repaidSoFar = repaid["b:" + person.lowercase().trim()] ?: 0.0,
            onRowClick = { detailRow = it },
            onSave = { amt -> setRepayment("b:" + person.lowercase().trim(), amt) },
            onDismiss = { borrowPerson = null },
            youOwe = true,
        )
    }

    detailRow?.let { r ->
        TxnDetailDialog(
            r,
            onReclassify = { kind, cat ->
                decide(r.id, kind, cat, r.counterparty, kind != "income" && kind != "ignore")
                detailRow = null
            },
            onNewCat = { cb -> newCatCallback = cb },
            onDismiss = { detailRow = null },
        )
    }
    newCatCallback?.let { cb ->
        NewCategoryDialog(
            onCreate = { name, emoji, color ->
                scope.launch {
                    withContext(Dispatchers.IO) { Storage.addCustomCat(context, name, emoji, color) }
                    CustomCats.map = withContext(Dispatchers.IO) { Storage.loadCustomCats(context) }
                    cb(name)
                    newCatCallback = null
                }
            },
            onDismiss = { newCatCallback = null },
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

private fun fmtPlain(n: Double): String =
    if (n == n.toLong().toDouble()) n.toLong().toString() else n.toString()

@Composable private fun BudgetsDialog(
    cats: List<String>, budgets: Map<String, Double>, fixed: Map<String, Double>,
    onSet: (String, Double) -> Unit, onFixed: (String, Double) -> Unit, onDismiss: () -> Unit,
) {
    val names = remember { mutableStateListOf<String>().apply { addAll(cats) } }
    val drafts = remember {
        mutableStateMapOf<String, String>().apply { cats.forEach { c -> (budgets[c] ?: fixed[c])?.let { put(c, fmtPlain(it)) } } }
    }
    val flags = remember { mutableStateMapOf<String, Boolean>().apply { fixed.keys.forEach { put(it, true) } } }
    var newName by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp)) {
            Text("Budgets", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            MutedText("Monthly amount per category; blank for none. Tap Fixed on costs that are the same every month (maid, milk, electricity).", 12)
            Spacer(Modifier.height(12.dp))
            Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
                names.forEach { c ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        MerchantTile("", c, 30.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(c, color = ZColor.Ink, fontSize = 14.sp, modifier = Modifier.weight(1f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Chip("Fixed", flags[c] == true) { flags[c] = flags[c] != true }
                        Spacer(Modifier.width(6.dp))
                        OutlinedTextField(
                            value = drafts[c] ?: "",
                            onValueChange = { v -> drafts[c] = v.filter { it.isDigit() || it == '.' } },
                            singleLine = true, placeholder = { Text("—") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(100.dp),
                        )
                    }
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newName, onValueChange = { newName = it }, singleLine = true,
                        placeholder = { Text("Add your own (e.g. Maid)") }, modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Chip("Add", false, filled = true) {
                        val n = newName.trim()
                        if (n.isNotEmpty() && n !in names) { names.add(0, n); flags[n] = true; newName = "" }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier.align(Alignment.End).clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                    .clickable {
                        names.forEach { c ->
                            val a = drafts[c]?.replace(",", "")?.toDoubleOrNull() ?: 0.0
                            if ((budgets[c] ?: 0.0) != a) onSet(c, a)
                            val fx = flags[c] == true && a > 0
                            if (fx && (fixed[c] ?: 0.0) != a) onFixed(c, a)
                            if (!fx && fixed.containsKey(c)) onFixed(c, 0.0)
                        }
                        onDismiss()
                    }.padding(horizontal = 22.dp, vertical = 10.dp),
            ) { Text("Done", color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable private fun SavingsDialog(rows: List<Row>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val saved = remember { Storage.loadPlan(context) }
    var sip by remember { mutableStateOf(saved["sip"] ?: "") }
    var emi by remember { mutableStateOf(saved["emi"] ?: "") }
    var goal by remember { mutableStateOf(saved["goal"] ?: "") }
    var years by remember { mutableStateOf(saved["years"] ?: "5") }
    var ret by remember { mutableStateOf(saved["ret"] ?: "12") }
    val months = (1..3).map { Dates.shiftMonth(Dates.todayMonth(), -it) }
    val mi = Analytics.monthlySpendIncome(rows, months, "All")
    val avgIn = mi.sumOf { it.third } / 3.0
    val avgSpent = mi.sumOf { it.second } / 3.0
    val left = avgIn - avgSpent
    fun num(t: String) = t.replace(",", "").toDoubleOrNull() ?: 0.0
    val sipN = num(sip); val emiN = num(emi); val goalN = num(goal)
    val yrs = num(years).coerceIn(1.0, 40.0); val rate = num(ret).coerceIn(0.0, 30.0)
    val free = left - sipN - emiN
    val n = (yrs * 12).toInt(); val r = rate / 1200.0
    // Future value of a monthly investment of 1 over n months (paid at month start).
    val factor = if (r == 0.0) n.toDouble() else ((Math.pow(1 + r, n.toDouble()) - 1) / r) * (1 + r)
    val fv = sipN * factor
    val needed = if (goalN > 0 && factor > 0) goalN / factor else 0.0
    @Composable fun field(label: String, v: String, set: (String) -> Unit) {
        OutlinedTextField(
            value = v, onValueChange = { x -> set(x.filter { it.isDigit() || it == '.' }) }, singleLine = true,
            label = { Text(label) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
    }
    Dialog(onDismissRequest = {
        Storage.savePlan(context, mapOf("sip" to sip, "emi" to emi, "goal" to goal, "years" to years, "ret" to ret)); onDismiss()
    }) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp)
            .heightIn(max = 640.dp).verticalScroll(rememberScrollState())) {
            Text("Savings plan", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            MutedText("Based on your last 3 full months.", 12)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Fact("Money in / month", money(avgIn), ZColor.Income)
                Fact("Spent / month", money(avgSpent))
                Fact("Left", money(left), if (left >= 0) ZColor.Income else ZColor.Warn)
            }
            Spacer(Modifier.height(14.dp))
            field("Monthly SIP (₹)", sip) { sip = it }
            Spacer(Modifier.height(8.dp))
            field("New EMI to plan for (₹ / month)", emi) { emi = it }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { field("Years", years) { years = it } }
                Box(Modifier.weight(1f)) { field("Return % / year", ret) { ret = it } }
            }
            Spacer(Modifier.height(8.dp))
            field("Goal amount (₹, e.g. down payment)", goal) { goal = it }
            Spacer(Modifier.height(14.dp))
            Panel {
                Fact("Free each month after SIP and EMI", (if (free < 0) "−" else "") + money(kotlin.math.abs(free)),
                    if (free >= 0) ZColor.Income else ZColor.Warn)
                if (sipN > 0) {
                    Spacer(Modifier.height(8.dp))
                    Fact("SIP in ${fmtPlain(yrs)} years at ${fmtPlain(rate)}%", money(fv))
                }
                if (goalN > 0) {
                    Spacer(Modifier.height(8.dp))
                    Fact("Needed each month for the goal", money(needed))
                    MutedText(if (needed <= left - emiN) "Within what you have left each month." else
                        "More than you have left each month — raise the years or lower the goal.", 12)
                }
                Spacer(Modifier.height(6.dp))
                MutedText("An estimate: returns are an assumption, not a promise.", 11)
            }
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier.align(Alignment.End).clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                    .clickable {
                        Storage.savePlan(context, mapOf("sip" to sip, "emi" to emi, "goal" to goal, "years" to years, "ret" to ret))
                        onDismiss()
                    }.padding(horizontal = 22.dp, vertical = 10.dp),
            ) { Text("Done", color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable private fun PersonCard(person: String, total: Double, bal: Double, color: Color, verb: String, onClick: () -> Unit) {
    val settled = bal <= 0.0
    Column(
        Modifier.width(150.dp).clip(RoundedCornerShape(16.dp)).background(ZColor.Paper)
            .border(1.dp, ZColor.Line, RoundedCornerShape(16.dp)).clickable { onClick() }.padding(12.dp),
    ) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(17.dp)).background(color.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            Text(person.trim().take(1).uppercase(), color = color, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text(person, color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        MutedText(verb, 11)
        Spacer(Modifier.height(4.dp))
        Text(if (settled) "Settled ✓" else money(bal), color = if (settled) ZColor.Muted else color,
            fontSize = 16.sp, fontWeight = FontWeight.Bold)
        if (!settled && bal < total) MutedText("of ${money(total)}", 11)
    }
}

@Composable private fun NewCategoryDialog(onCreate: (String, String, Long) -> Unit, onDismiss: () -> Unit) {
    val colors = listOf(0xFFE57373, 0xFFF4A261, 0xFFE9C46A, 0xFF81B29A, 0xFF4DB6AC, 0xFF64B5F6, 0xFF7986CB, 0xFFBA68C8, 0xFFA1887F, 0xFF90A4AE)
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("cart") }
    var color by remember { mutableStateOf(colors[5]) }
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp)
            .heightIn(max = 640.dp).verticalScroll(rememberScrollState())) {
            Text("New category", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Color(color).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) { androidx.compose.material3.Icon(CatIcons.forKey(icon), null, tint = Color(color), modifier = Modifier.size(24.dp)) }
                Spacer(Modifier.width(12.dp))
                OutlinedTextField(
                    value = name, onValueChange = { name = it }, singleLine = true,
                    placeholder = { Text("Category name") }, modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(12.dp))
            MutedText("Icon", 12)
            Spacer(Modifier.height(6.dp))
            CatIcons.picker.keys.toList().chunked(6).forEach { rowKeys ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    rowKeys.forEach { k ->
                        Box(
                            Modifier.size(42.dp).clip(RoundedCornerShape(12.dp))
                                .background(if (k == icon) Color(color).copy(alpha = 0.2f) else ZColor.Paper)
                                .border(if (k == icon) 2.dp else 0.dp, Color(color), RoundedCornerShape(12.dp))
                                .clickable { icon = k },
                            contentAlignment = Alignment.Center,
                        ) { androidx.compose.material3.Icon(CatIcons.forKey(k), null,
                            tint = if (k == icon) Color(color) else ZColor.InkSoft, modifier = Modifier.size(22.dp)) }
                    }
                    repeat(6 - rowKeys.size) { Spacer(Modifier.size(42.dp)) }
                }
            }
            Spacer(Modifier.height(10.dp))
            MutedText("Colour", 12)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                colors.forEach { c ->
                    Box(
                        Modifier.size(30.dp).clip(RoundedCornerShape(15.dp)).background(Color(c))
                            .border(if (c == color) 3.dp else 0.dp, ZColor.Ink, RoundedCornerShape(15.dp))
                            .clickable { color = c },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier.align(Alignment.End).clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                    .clickable { if (name.trim().isNotEmpty()) onCreate(name.trim(), "i:$icon", color) }
                    .padding(horizontal = 22.dp, vertical = 10.dp),
            ) { Text("Create", color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable private fun LendRow(
    person: String, total: Double, bal: Double, color: Color, verb: String, onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onClick() }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(person, color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            MutedText(
                if (bal <= 0.0) "Settled · ${money(total)}"
                else if (bal < total) "${money(total - bal)} of ${money(total)} cleared"
                else verb,
                11,
            )
        }
        Text(money(bal), color = if (bal <= 0.0) ZColor.Muted else color, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(6.dp))
        Text("›", color = ZColor.Muted, fontSize = 16.sp)
    }
}

@Composable private fun LendingDialog(
    person: String, lentRows: List<Row>, repaidSoFar: Double,
    onRowClick: (Row) -> Unit, onSave: (Double) -> Unit, onDismiss: () -> Unit,
    youOwe: Boolean = false,
) {
    val total = lentRows.sumOf { it.amount }
    var text by remember { mutableStateOf(if (repaidSoFar > 0) fmtPlain(repaidSoFar) else "") }
    val entered = text.replace(",", "").toDoubleOrNull() ?: 0.0
    val balance = (total - entered).coerceAtLeast(0.0)
    val headWord = if (youOwe) "Borrowed" else "Lent"
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp)) {
            Text(person, color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            MutedText("$headWord ${money(total)} in ${lentRows.size} ${if (lentRows.size == 1) "transfer" else "transfers"}", 12)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Fact(headWord, money(total))
                Fact(if (youOwe) "You repaid" else "Repaid", money(entered), ZColor.Income)
                Fact("Balance", money(balance), if (balance <= 0.0) ZColor.Muted else ZColor.Warn)
            }
            Spacer(Modifier.height(14.dp))
            Eyebrow("Repaid so far")
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { v -> text = v.filter { it.isDigit() || it == '.' } },
                singleLine = true,
                placeholder = { Text("0") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Mark fully paid", false) { text = fmtPlain(total) }
                if (entered > 0) Chip("Clear", false) { text = "" }
            }
            if (lentRows.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Eyebrow("Transfers")
                Spacer(Modifier.height(4.dp))
                Column(Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState())) {
                    lentRows.sortedByDescending { it.date }.forEach { TxnRow(it, false, onRowClick) }
                }
                MutedText("Tap a transfer to see its message.", 11)
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.clip(RoundedCornerShape(20.dp)).clickable { onDismiss() }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) { Text("Cancel", color = ZColor.Muted, fontWeight = FontWeight.SemiBold) }
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                        .clickable { onSave(entered); onDismiss() }.padding(horizontal = 22.dp, vertical = 10.dp),
                ) { Text("Save", color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable private fun TxnDetailDialog(t: Row, onReclassify: (String, String) -> Unit, onNewCat: (((String) -> Unit)) -> Unit, onDismiss: () -> Unit) {
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
                modifier = Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState()),
            )
            Spacer(Modifier.height(14.dp))
            Eyebrow("Why this label")
            Spacer(Modifier.height(4.dp))
            MutedText(Classifier.explain(t.counterparty, t.raw, t.kind, t.category), 12)
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
                    CustomCats.names.forEach { c ->
                        Chip(customLabel(c), false) { onReclassify("expense", c) }
                    }
                    Chip("＋ New", false, filled = true) { onNewCat { c -> onReclassify("expense", c) } }
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

@Composable private fun AccountCard(account: String, bal: Pair<Double, Long>?, spent: Double, modifier: Modifier) {
    val parts = account.trim().split(" ")
    val last4 = parts.lastOrNull()?.takeIf { it.length in 3..4 && it.all { c -> c.isDigit() } }
    val name = if (last4 != null) parts.dropLast(1).joinToString(" ") else account
    val tint = bankColor(account)
    val logo = bankLogoRes(account)
    val fmt = java.text.SimpleDateFormat("d MMM", java.util.Locale.US)
    Row(modifier.height(112.dp).clip(RoundedCornerShape(16.dp)).border(1.dp, ZColor.Line, RoundedCornerShape(16.dp))) {
        Column(
            Modifier.weight(0.42f).fillMaxHeight().background(tint.copy(alpha = 0.22f)).padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                if (logo != null) androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(logo), contentDescription = null,
                    modifier = Modifier.size(24.dp),
                ) else Text(name.take(1).uppercase(), color = tint, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Column {
                Text(name, color = ZColor.Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (last4 != null) MutedText("xx$last4", 11)
            }
        }
        Column(
            Modifier.weight(0.58f).fillMaxHeight().background(ZColor.Card).padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(if (bal != null) money(bal.first) else "—", color = ZColor.Ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                MutedText(if (bal != null && bal.second > 0) "as of " + fmt.format(java.util.Date(bal.second)) else "no balance alert", 11)
            }
            MutedText("Spent  " + money(spent), 12)
        }
    }
}

// ---------- credit card detail ----------

private fun cycleYmd(idx: Int, d: Int): String {
    val y = idx / 12; val mo = idx % 12
    val cal = java.util.Calendar.getInstance().apply { clear(); set(y, mo, 1) }
    val maxd = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    return String.format(java.util.Locale.US, "%04d-%02d-%02d", y, mo + 1, minOf(d, maxd))
}

// Billing cycle [start, end] for a cycle-start day; offset 0 = the cycle containing today.
private fun cycleRange(day: Int, offset: Int): Pair<String, String> {
    val now = java.util.Calendar.getInstance()
    val idxNow = now.get(java.util.Calendar.YEAR) * 12 + now.get(java.util.Calendar.MONTH)
    val startIdx = (if (now.get(java.util.Calendar.DAY_OF_MONTH) >= day) idxNow else idxNow - 1) + offset
    return cycleYmd(startIdx, day) to Dates.shiftDays(cycleYmd(startIdx + 1, day), -1)
}

private fun prettyDay(ymd: String): String {
    val m = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    return ymd.substring(8, 10).toInt().toString() + " " + m[ymd.substring(5, 7).toInt() - 1]
}

@Composable private fun CardDetailScreen(
    account: String, allRows: List<Row>, limit: Double?, reminders: List<Reminder>,
    reminderState: Map<String, Boolean>, pref: Pair<Int, Double>?,
    onSavePref: (Int, Double) -> Unit, onMarkPaid: (String) -> Unit,
    onRowClick: (Row) -> Unit, onBack: () -> Unit,
) {
    androidx.activity.compose.BackHandler { onBack() }
    var tab by remember { mutableStateOf(0) }
    var offset by remember { mutableStateOf(0) }
    var dayDialog by remember { mutableStateOf(false) }
    var budgetDialog by remember { mutableStateOf(false) }
    val day = pref?.first ?: 1
    val budget = pref?.second ?: 0.0
    val (from, to) = cycleRange(day, offset)
    val rows = allRows.filter { it.account == account && it.date >= from && it.date <= to }
        .sortedByDescending { it.date }
    val spendRows = rows.filter { it.kind == "expense" }
    val spent = spendRows.sumOf { it.amount }
    val parts = account.trim().split(" ")
    val last4 = parts.lastOrNull()?.takeIf { it.length in 3..4 && it.all { c -> c.isDigit() } } ?: account.takeLast(4)
    val bank = parts.firstOrNull() ?: account
    val logo = bankLogoRes(account)
    val today = Dates.today()
    val due = reminders.filter { r ->
        r.account.takeLast(4) == account.takeLast(4) && !(reminderState[r.id] ?: r.done)
    }.sortedBy { it.dueDate }.firstOrNull { it.dueDate >= today }

    Box(Modifier.fillMaxSize().background(ZColor.Paper)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, top = 46.dp, bottom = if (due != null) 130.dp else 40.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TopIcon(onClick = onBack) { Text("‹", color = ZColor.Ink, fontSize = 22.sp) }
                Spacer(Modifier.width(12.dp))
                Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                    if (logo != null) androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(logo), contentDescription = null, modifier = Modifier.size(28.dp),
                    ) else Text(bank.take(1).uppercase(), color = bankColor(account), fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("$bank Credit Card", color = ZColor.Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    MutedText("•••• $last4", 12)
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                NavButton("‹") { offset -= 1 }
                Row(
                    Modifier.weight(1f).clip(RoundedCornerShape(22.dp)).background(ZColor.Card)
                        .border(1.dp, ZColor.Line, RoundedCornerShape(22.dp)).clickable { dayDialog = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(prettyDay(from) + " – " + prettyDay(to) + "  ▾", color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                if (offset < 0) NavButton("›") { offset += 1 }
            }
            Spacer(Modifier.height(20.dp))
            val track = ZColor.Line; val arc = if (budget > 0 && spent > budget) ZColor.Warn else ZColor.Accent
            val frac = if (budget > 0) (spent / budget).coerceIn(0.0, 1.0).toFloat() else if (spent > 0) 1f else 0f
            Box(Modifier.align(Alignment.CenterHorizontally).size(190.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val w = 16f
                    val sz = androidx.compose.ui.geometry.Size(size.width - w, size.height - w)
                    val tl = androidx.compose.ui.geometry.Offset(w / 2, w / 2)
                    drawArc(track, 0f, 360f, false, tl, sz, style = Stroke(width = w))
                    if (frac > 0f) drawArc(arc, -90f, 360f * frac, false, tl, sz, style = Stroke(width = w, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MutedText("Spent", 12)
                    Text(money(spent), color = ZColor.Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    if (budget > 0) MutedText("of " + money(budget), 12)
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f).clickable { budgetDialog = true }) {
                    MutedText("Monthly budget", 12)
                    Text(if (budget > 0) money(budget) else "Set budget ›", color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    MutedText("Available credit", 12)
                    Text(if (limit != null) money(limit) else "—", color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(16.dp))
            ChipScroll {
                listOf("Transactions", "Categories", "Merchants").forEachIndexed { i, t -> Chip(t, tab == i, filled = true) { tab = i } }
            }
            Spacer(Modifier.height(6.dp))
            when (tab) {
                0 -> Panel {
                    if (rows.isEmpty()) MutedText("No transactions in this cycle.")
                    else rows.forEach { TxnRow(it, false, onRowClick) }
                }
                1 -> Panel {
                    val items = Analytics.byCategory(spendRows)
                    if (items.isEmpty()) MutedText("Nothing spent in this cycle.")
                    val mx = items.maxOfOrNull { it.second }?.coerceAtLeast(1.0) ?: 1.0
                    items.forEach { (c, v) ->
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MerchantTile("", c, 30.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(c, color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Meter((v / mx).toFloat(), catColor(c))
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(money(v), color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                else -> Panel {
                    val items = spendRows.groupBy { it.counterparty?.takeIf { c -> c.isNotBlank() } ?: "Unknown" }
                        .map { (k, v) -> Triple(k, v.sumOf { it.amount }, v.first().category) }.sortedByDescending { it.second }
                    if (items.isEmpty()) MutedText("Nothing spent in this cycle.")
                    val mx = items.maxOfOrNull { it.second }?.coerceAtLeast(1.0) ?: 1.0
                    items.forEach { (k, v, c) ->
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MerchantTile(k, c, 30.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(k, color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Meter((v / mx).toFloat(), catColor(c))
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(money(v), color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        if (due != null) {
            Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(ZColor.Card).border(1.dp, ZColor.Line, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 26.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Pending bill · due " + prettyDay(due.dueDate), color = ZColor.Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(money(due.amount), color = ZColor.Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    due.minAmount?.let { MutedText("Min payable " + money(it), 12) }
                }
                Box(
                    Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                        .clickable { onMarkPaid(due.id) }.padding(horizontal = 18.dp, vertical = 12.dp),
                ) { Text("Mark as paid", color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
            }
        }
    }

    if (dayDialog) {
        Dialog(onDismissRequest = { dayDialog = false }) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(18.dp)) {
                Text("Billing cycle starts on", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                MutedText("For a cycle 1 Apr – 30 Apr choose 1. Check your card bill for the exact day.", 12)
                Spacer(Modifier.height(12.dp))
                (1..31).chunked(7).forEach { weekDays ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        weekDays.forEach { d ->
                            Box(
                                Modifier.size(38.dp).clip(RoundedCornerShape(19.dp))
                                    .background(if (d == day) ZColor.Ink else ZColor.Card)
                                    .border(1.dp, ZColor.Line, RoundedCornerShape(19.dp))
                                    .clickable { onSavePref(d, budget); offset = 0; dayDialog = false },
                                contentAlignment = Alignment.Center,
                            ) { Text("$d", color = if (d == day) ZColor.Paper else ZColor.Ink, fontSize = 13.sp) }
                        }
                        repeat(7 - weekDays.size) { Spacer(Modifier.size(38.dp)) }
                    }
                }
            }
        }
    }
    if (budgetDialog) {
        var text by remember { mutableStateOf(if (budget > 0) fmtPlain(budget) else "") }
        Dialog(onDismissRequest = { budgetDialog = false }) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp)) {
                Text("Monthly card budget", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = text, onValueChange = { v -> text = v.filter { it.isDigit() || it == '.' } },
                    singleLine = true, placeholder = { Text("₹ per cycle") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(14.dp))
                Box(
                    Modifier.align(Alignment.End).clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                        .clickable { onSavePref(day, text.toDoubleOrNull() ?: 0.0); budgetDialog = false }
                        .padding(horizontal = 22.dp, vertical = 10.dp),
                ) { Text("Save", color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable private fun TopIcon(onClick: () -> Unit, dot: Boolean = false, content: @Composable () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(RoundedCornerShape(20.dp)).background(ZColor.Card)
            .border(1.dp, ZColor.Line, RoundedCornerShape(20.dp)).clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        content()
        if (dot) Box(Modifier.align(Alignment.TopEnd).padding(9.dp).size(8.dp).clip(RoundedCornerShape(4.dp)).background(ZColor.Accent))
    }
}

@Composable private fun FunnelIcon() {
    val c = ZColor.Ink
    Canvas(Modifier.size(18.dp)) {
        val w = size.width; val h = size.height
        val path = Path().apply {
            moveTo(w * 0.05f, h * 0.1f); lineTo(w * 0.95f, h * 0.1f); lineTo(w * 0.6f, h * 0.5f)
            lineTo(w * 0.6f, h * 0.9f); lineTo(w * 0.4f, h * 0.78f); lineTo(w * 0.4f, h * 0.5f); close()
        }
        drawPath(path, c, style = Stroke(width = 2.2f, cap = StrokeCap.Round))
    }
}

@Composable private fun MenuIcon() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(3) { Box(Modifier.width(16.dp).height(2.dp).clip(RoundedCornerShape(1.dp)).background(ZColor.Ink)) }
    }
}

@Composable private fun MenuRow(title: String, sub: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text(title, color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        MutedText(sub, 12)
    }
}

@Composable private fun BottomNav(view: String, modifier: Modifier, onPick: (String) -> Unit) {
    val items = listOf("ledger" to "Home", "spend" to "Spend", "payees" to "Payees", "dues" to "Dues")
    Row(
        modifier.fillMaxWidth().background(ZColor.Card).border(1.dp, ZColor.Line)
            .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 22.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        items.forEach { (k, label) ->
            val on = view == k
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                    .background(if (on) ZColor.Ink else ZColor.Card)
                    .clickable { onPick(k) }.padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = if (on) ZColor.Paper else ZColor.Muted, fontSize = 13.sp,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1)
            }
        }
    }
}

@Composable private fun ViewSwitcher(view: String, onPick: (String) -> Unit) {
    val items = listOf("ledger" to "Ledger", "pace" to "Pace", "buckets" to "Buckets", "payees" to "Payees", "invest" to "Invest", "dues" to "Dues")
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { (k, lbl) ->
            val sel = view == k
            Box(
                Modifier.clip(RoundedCornerShape(12.dp))
                    .background(if (sel) ZColor.Ink else ZColor.Card)
                    .border(1.dp, if (sel) ZColor.Ink else ZColor.Line, RoundedCornerShape(12.dp))
                    .clickable { onPick(k) }.padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(lbl, color = if (sel) ZColor.Paper else ZColor.Muted,
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
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
    "refund" -> if (t.category != "Refund") "Refund · ${t.category}" else "Refund"
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

// A rotating pool of "At a glance" one-liners, so the panel feels alive rather than
// repeating one line. The caller picks one by a seed that shifts with day/category/month.
private fun glanceTips(ins: Analytics.Insight, spent: Double, income: Double): List<String> {
    val cat = ins.topCat
    val pct = ins.topCatPct
    val big = ins.biggestName
    val ob = ins.overBy
    val over = ob != null && ob > 0
    val under = ob != null && ob < 0
    val savings = if (income > 0) Math.round((income - spent) / income * 100).toInt() else null
    return buildList {
        add("$cat is your biggest bucket at $pct% — trimming it is the quickest win.")
        add("$pct% of your spending went to $cat this month.")
        add("Your largest single spend was $big.")
        add("A small trim to $cat moves the needle more than anything else.")
        add("Most of the money flowed into $cat — the rest is spread thin.")
        add("$cat leads the month; everything else trails well behind.")
        add("Spending is concentrated: $cat alone is $pct%.")
        add("$big was the heaviest hitter this month.")
        add("If you trimmed $cat by a tenth, you'd barely notice the rest.")
        add("Keep an eye on $cat — it's doing most of the work here.")
        add("$cat is where a budget would bite first.")
        add("Your money has a clear favourite this month: $cat.")
        add("$pct% in one bucket ($cat); the rest is background noise.")
        add("The 80/20 here is simple — $cat is the 80.")
        add("One bucket, $cat, carries $pct% of the load.")
        add("Trim $cat, cap $big, and the month looks lean.")
        add("$cat and $big tell most of this month's story.")
        add("Nothing wild this month — $cat just leads as usual.")
        add("Small leaks in $cat add up fast — worth a glance.")
        add("If you only tracked one thing, track $cat.")
        add("$cat tops the list again — it's your steady number one.")
        add("Curious where it goes? $cat is the headline at $pct%.")
        if (savings != null && savings > 0) {
            add("You held on to about $savings% of what came in this month.")
            add("Roughly $savings% of your income stayed put — not bad.")
        }
        if (savings != null && savings <= 0 && savings > -15) {
            add("You spent about as much as came in this month.")
            add("Money in and money out are running neck and neck.")
        }
        if (savings != null && savings <= -15 && income > 0) {
            add("You've spent about " + String.format(java.util.Locale.US, "%.1f", spent / income) + "× what came in so far.")
            add("Spending is well ahead of income — large EMIs or one-off payments may be the reason.")
        }
        if (over) {
            add("You're ahead of a steady pace — easing $cat helps most.")
            add("Running a little hot this month; $cat is the lever to pull.")
        }
        if (under) {
            add("You're under a steady pace — comfortably on track.")
            add("Spending's calmer than a steady pace allows. Nice work.")
        }
    }
}

@Composable private fun LedgerView(
    allRows: List<Row>, list: List<Row>, f: Analytics.Filter, accounts: List<String>,
    month: String, account: String, daySel: Int, onDay: (Int) -> Unit,
    askOpen: Boolean, setAskOpen: (Boolean) -> Unit,
    decide: (String, String, String, String?, Boolean) -> Unit,
    onRowClick: (Row) -> Unit,
    onDrill: (String, List<Row>) -> Unit,
    excludeEmi: Boolean = false,
    repaid: Map<String, Double> = emptyMap(),
    onOpenLend: (String) -> Unit = {},
    onOpenBorrow: (String) -> Unit = {},
    fixed: Map<String, Double> = emptyMap(),
    onOpenFixed: () -> Unit = {},
    balances: Map<String, Pair<Double, Long>> = emptyMap(),
    onNewCat: (((String) -> Unit)) -> Unit = {},
    limits: Map<String, Pair<Double, Long>> = emptyMap(),
    reminders: List<Reminder> = emptyList(),
    onOpenCard: (String) -> Unit = {},
    onToggleEmi: () -> Unit = {},
    onEditBudget: () -> Unit = {},
    onOpenDues: () -> Unit = {},
    reminderState: Map<String, Boolean> = emptyMap(),
    refundSuggestions: List<Pair<Row, Row>> = emptyList(),
    onRefundAnswer: (String, Boolean) -> Unit = { _, _ -> },
) {
    val s = Analytics.summarize(list)
    val ins = Analytics.insight(list, f)
    val bigIds = Analytics.bigSpendIds(list)
    val pct = (s.spent / Analytics.BUDGET).toFloat()
    val budgetLeft = (Analytics.BUDGET - s.spent).coerceAtLeast(0.0)

    // Spent + budget
    Panel {
        val overBudget = s.spent > Analytics.BUDGET
        val emiAmt = list.filter { it.kind == "expense" && it.category == "EMI" }.sumOf { it.amount }
        Eyebrow("Spent in " + Analytics.periodLabel(f) + if (excludeEmi) " · after EMI" else "")
        Text(money(s.spent), color = ZColor.Ink, fontSize = 38.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp)); Meter(pct.coerceIn(0f, 1f), if (overBudget) ZColor.Warn else ZColor.Accent)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().clickable { onEditBudget() }, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                if (overBudget) money(s.spent - Analytics.BUDGET) + " over budget" else money(budgetLeft) + " left",
                color = if (overBudget) ZColor.Warn else ZColor.Income, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            )
            MutedText("of " + money(Analytics.BUDGET) + " ›", 13)
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Fact("Money in", money(s.income), ZColor.Income)
                if (s.income > s.spent) MutedText("Saved " + money(s.income - s.spent) + " (" + Math.round((s.income - s.spent) / s.income * 100) + "%)", 12)
            }
            if (emiAmt > 0 || excludeEmi) {
                Column(horizontalAlignment = Alignment.End) {
                    if (!excludeEmi) MutedText("EMI included " + money(emiAmt), 12)
                    Spacer(Modifier.height(4.dp))
                    Chip(if (excludeEmi) "EMI excluded ✓" else "Exclude EMI", excludeEmi, filled = true) { onToggleEmi() }
                }
            }
        }
        if (fixed.isNotEmpty()) {
            val fixedTotal = fixed.values.sum()
            Spacer(Modifier.height(10.dp))
            MutedText("Fixed " + money(fixedTotal) + " · free " + money((s.income - fixedTotal).coerceAtLeast(0.0)) +
                " · beyond fixed " + money((s.spent - fixedTotal).coerceAtLeast(0.0)), 12)
        }
    }

    // Heads up — categories or single spends well above your usual (current month only)
    run {
        if (f.period == "month" && f.month == Dates.todayMonth()) {
            val alerts = Analytics.unusual(allRows, f.month)
            if (alerts.isNotEmpty()) Panel {
                Eyebrow("Heads up")
                alerts.forEach { (t, d) ->
                    Spacer(Modifier.height(8.dp))
                    Text(t, color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    MutedText(d, 12)
                }
            }
        }
    }

    // Upcoming bill — the next unpaid reminder
    run {
        val today = Dates.today()
        val next = reminders.filter { r -> !(reminderState[r.id] ?: r.done) && r.dueDate >= today }.minByOrNull { it.dueDate }
        if (next != null) {
            Panel {
                Row(Modifier.fillMaxWidth().clickable { onOpenDues() }, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Eyebrow("Upcoming bill")
                        Spacer(Modifier.height(4.dp))
                        Text(next.counterparty?.takeIf { it.isNotBlank() } ?: "Bill", color = ZColor.Ink, fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        MutedText("due " + prettyDay(next.dueDate), 12)
                    }
                    Text(money(next.amount), color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("  ›", color = ZColor.Muted, fontSize = 16.sp)
                }
            }
        }
    }

    // Refund that looks like it belongs to an earlier spend — confirm before it nets against it.
    refundSuggestions.firstOrNull()?.let { (rf, sp) ->
        Panel {
            Eyebrow("Needs you · refund")
            Spacer(Modifier.height(6.dp))
            Text(money(rf.amount) + " refund on " + rf.date, color = ZColor.Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            MutedText("Is it for " + (sp.counterparty?.takeIf { it.isNotBlank() } ?: sp.category) + " " + money(sp.amount) + " on " + sp.date + "?", 13)
            Spacer(Modifier.height(10.dp))
            ChipScroll {
                Chip("Yes, same spend", false, filled = true) { onRefundAnswer(rf.id, true) }
                Chip("No", false) { onRefundAnswer(rf.id, false) }
            }
        }
    }

    // Ask-me — tackle the biggest unknowns first.
    val pending = list.filter { it.kind == "expense" && it.category == "Unknown" }.sortedByDescending { it.amount }
    val next = pending.firstOrNull()
    if (next != null) {
        Panel {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onDrill("Needs a category", pending) },
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Eyebrow("Needs you · ${pending.size} to go")
                if (pending.size > 1) Text("see all ›", color = ZColor.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
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
                    CustomCats.names.forEach { c ->
                        Chip(customLabel(c), false) { decide(next.id, "expense", c, next.counterparty, true) }
                    }
                    Chip("＋ New", false, filled = true) { onNewCat { c -> decide(next.id, "expense", c, next.counterparty, true) } }
                }
            }
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
            MutedText("Biggest spend", 12)
            Text("${ins.biggestName} · ${money(ins.biggestAmt)}", color = ZColor.Ink,
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val tips = glanceTips(ins, s.spent, s.income)
            if (tips.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                val seed = kotlin.math.abs(Dates.dayOfMonth() * 31 + ins.topCat.hashCode() + month.hashCode())
                Text(tips[seed % tips.size], color = ZColor.InkSoft, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }

    // Recent — horizontal strip
    Panel {
        val recent = list.filter { it.kind != "invest" }.sortedByDescending { it.date }
        Row(Modifier.fillMaxWidth().clickable { onDrill("All transactions", recent) },
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Eyebrow("Recent")
            Text("see all ›", color = ZColor.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(8.dp))
        if (recent.isEmpty()) MutedText("No transactions here.")
        else Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            recent.take(10).forEach { t ->
                Column(
                    Modifier.width(150.dp).clip(RoundedCornerShape(14.dp)).background(ZColor.Paper)
                        .border(1.dp, ZColor.Line, RoundedCornerShape(14.dp))
                        .clickable { onRowClick(t) }.padding(12.dp),
                ) {
                    MerchantTile(t.counterparty ?: "", t.category, 30.dp, t.kind)
                    Spacer(Modifier.height(8.dp))
                    Text(t.counterparty?.takeIf { it.isNotBlank() } ?: t.category, color = ZColor.Ink, fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(money(t.amount), color = if (t.direction == "credit") ZColor.Income else ZColor.Ink,
                        fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    MutedText(prettyDay(t.date), 11)
                }
            }
        }
    }
    
    // Accounts — latest balance + spend this period per account, swipeable
    val spentBy = Analytics.spentByAccount(list, accounts).toMap()
    val acctNames = (balances.keys + spentBy.keys).distinct().filter { !(it.contains(" CC ") || it.startsWith("CC ") || it.endsWith(" CC")) }
        .sortedByDescending { balances[it]?.first ?: -1.0 }
    var allAccountsOpen by remember { mutableStateOf(false) }
    if (acctNames.isNotEmpty()) {
        Panel {
            Row(Modifier.fillMaxWidth().clickable { allAccountsOpen = true },
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Eyebrow("Accounts")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (balances.isNotEmpty())
                        Text(money(balances.values.sumOf { it.first }), color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text("see all ›", color = ZColor.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                acctNames.forEach { a ->
                    AccountCard(a, balances[a], spentBy[a] ?: 0.0, Modifier.width(280.dp))
                }
            }
        }
    }
    if (allAccountsOpen) {
        Dialog(onDismissRequest = { allAccountsOpen = false }) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Paper).padding(16.dp)
                .heightIn(max = 640.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("All accounts", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                acctNames.forEach { a -> AccountCard(a, balances[a], spentBy[a] ?: 0.0, Modifier.fillMaxWidth()) }
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
                    val today = Dates.today()
                    val due = reminders.filter { r -> !r.done && r.dueDate >= today && r.account.takeLast(4) == a.takeLast(4) }
                        .minByOrNull { it.dueDate }
                    CardTile(a, spent, limits[a], limits["$a#total"], due) { onOpenCard(a) }
                }
            }
            Spacer(Modifier.height(10.dp))
            MutedText("Tap a card to see its spends. Card spends count in your totals; paying the bill is a transfer.", 11)
        }
    }

    // Lending — all-time so repayments and balances stay correct across months.
    val lentAll = allRows.filter { it.kind == "lent" }
    val borrowedAll = allRows.filter { it.kind == "borrowed" }
    if (lentAll.isNotEmpty() || borrowedAll.isNotEmpty()) {
        val byPerson = lentAll.groupBy { it.counterparty?.takeIf { c -> c.isNotBlank() } ?: "Someone" }
            .map { (person, rs) ->
                val total = rs.sumOf { it.amount }
                val paid = repaid[person.lowercase().trim()] ?: 0.0
                Triple(person, total, (total - paid).coerceAtLeast(0.0))
            }.sortedByDescending { it.third }
        val owedToYou = byPerson.sumOf { it.third }
        val borrowByPerson = borrowedAll.groupBy { it.counterparty?.takeIf { c -> c.isNotBlank() } ?: "Someone" }
            .map { (person, rs) ->
                val tot = rs.sumOf { it.amount }
                val paid = repaid["b:" + person.lowercase().trim()] ?: 0.0
                Triple(person, tot, (tot - paid).coerceAtLeast(0.0))
            }.sortedByDescending { it.third }
        Panel {
            Eyebrow("People · you get / you pay")
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Fact("Owed to you", money(owedToYou), ZColor.Income)
                Fact("You owe", money(borrowByPerson.sumOf { it.third }), ZColor.Warn)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Open balances first, settled ones at the end.
                byPerson.sortedBy { it.third <= 0.0 }.forEach { (person, total, bal) ->
                    PersonCard(person, total, bal, ZColor.Income, "owes you") { onOpenLend(person) }
                }
                borrowByPerson.sortedBy { it.third <= 0.0 }.forEach { (person, total, bal) ->
                    PersonCard(person, total, bal, ZColor.Warn, "you owe") { onOpenBorrow(person) }
                }
            }
        }
    }

    Spacer(Modifier.height(6.dp))
    MutedText("${allRows.size} transactions read · Private — everything stays on your phone.", 11)
}

@Composable private fun DailyCalendarBlock(
    allRows: List<Row>, list: List<Row>, f: Analytics.Filter, month: String, account: String,
    daySel: Int, onDay: (Int) -> Unit, onRowClick: (Row) -> Unit,
) {
    val bigIds = Analytics.bigSpendIds(list)
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

@Composable private fun Fact(k: String, v: String, color: Color = ZColor.Ink) {
    Column {
        MutedText(k, 12)
        Text(v, color = color, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable private fun CardTile(account: String, spent: Double, limit: Pair<Double, Long>?, total: Pair<Double, Long>?, due: Reminder?, onClick: () -> Unit = {}) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val parts = account.trim().split(" ")
    val last4 = parts.lastOrNull()?.takeIf { it.length in 3..4 && it.all { c -> c.isDigit() } } ?: account.takeLast(4)
    val bank = parts.firstOrNull() ?: account
    val base = bankColor(account)
    val logo = bankLogoRes(account)
    val shown = limit ?: total
    val asOf = shown?.let { java.text.SimpleDateFormat("d MMM", java.util.Locale.US).apply { timeZone = Dates.IST }.format(java.util.Date(it.second)) }
    val enquiry = when (bank.uppercase()) {
        "HDFC" -> "5676712" to "CCBAL $last4"
        "ICICI" -> "5676766" to "IBALCC ${last4}XX"
        else -> null
    }
    val soft = Color.White.copy(alpha = 0.72f)
    Column(Modifier.width(290.dp)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1.6f).clip(RoundedCornerShape(18.dp))
                .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(base, base.copy(alpha = 0.7f))))
                .clickable { onClick() },
        ) {
            // Soft circles for depth.
            Canvas(Modifier.matchParentSize()) {
                drawCircle(Color.White.copy(alpha = 0.07f), radius = size.width * 0.38f, center = androidx.compose.ui.geometry.Offset(size.width * 0.92f, size.height * 0.08f))
                drawCircle(Color.White.copy(alpha = 0.05f), radius = size.width * 0.30f, center = androidx.compose.ui.geometry.Offset(size.width * 0.78f, size.height * 1.02f))
            }
            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                        if (logo != null) androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(logo), contentDescription = null, modifier = Modifier.size(22.dp),
                        ) else Text(bank.take(1).uppercase(), color = base, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("CREDIT CARD", color = soft, fontSize = 11.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Medium)
                        if (enquiry != null) Text("  ↻", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                try {
                                    ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_SENDTO,
                                        android.net.Uri.parse("smsto:${enquiry.first}")).putExtra("sms_body", enquiry.second))
                                } catch (_: Exception) {}
                            })
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(34.dp).height(25.dp).clip(RoundedCornerShape(5.dp)).background(
                        androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFFE8C766), Color(0xFFB99434)))))
                    Spacer(Modifier.width(14.dp))
                    Text("••••  ••••  ••••  $last4", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Column {
                        Text(if (limit != null || shown == null) "Available limit" else "Credit limit", color = soft, fontSize = 10.sp)
                        Text(if (shown != null) money(shown.first) else "limit not in SMS", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        if (asOf != null) Text("as of $asOf", color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(if (due != null) "Due " + due.dueDate.takeLast(5) else "No bill due", color = soft, fontSize = 10.sp)
                        if (due != null) Text(money(due.amount), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        MutedText("Spent this period  ${money(spent)}", 12)
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

@Composable private fun BucketsView(
    list: List<Row>, prevByCat: Map<String, Double> = emptyMap(),
    budgets: Map<String, Double> = emptyMap(), onEditBudgets: () -> Unit = {},
    onDrill: (String, List<Row>) -> Unit,
) {
    val cats = Analytics.byCategory(list)
    val total = cats.sumOf { it.second }.coerceAtLeast(1.0)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Where it went", color = ZColor.Ink, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Box(
            Modifier.clip(RoundedCornerShape(20.dp)).border(1.dp, ZColor.Line, RoundedCornerShape(20.dp))
                .clickable { onEditBudgets() }.padding(horizontal = 14.dp, vertical = 8.dp),
        ) { Text("Budgets", color = ZColor.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
    }
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
                val prev = prevByCat[c]
                if (prev != null && prev > 0) {
                    val ch = ((v - prev) / prev * 100).toInt()
                    val up = ch > 0
                    Spacer(Modifier.height(3.dp))
                    Text(
                        (if (up) "▲ " else if (ch < 0) "▼ " else "• ") + "${kotlin.math.abs(ch)}% vs last month",
                        color = if (up) ZColor.Warn else if (ch < 0) ZColor.Income else ZColor.Muted,
                        fontSize = 11.sp,
                    )
                } else if (prev == null || prev == 0.0) {
                    Spacer(Modifier.height(3.dp))
                    MutedText("new this month", 11)
                }
                val budget = budgets[c]
                if (budget != null && budget > 0) {
                    val overB = v > budget
                    Spacer(Modifier.height(3.dp))
                    Text("${money(v)} of ${money(budget)} budget · ${(v / budget * 100).toInt()}%",
                        color = if (overB) ZColor.Warn else ZColor.Muted, fontSize = 11.sp)
                    Spacer(Modifier.height(5.dp))
                    Meter((v / budget).toFloat().coerceIn(0f, 1f), if (overB) ZColor.Warn else ZColor.Income)
                } else {
                    Spacer(Modifier.height(7.dp))
                    Meter((v / top).toFloat(), catColor(c))
                }
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

@Composable private fun RemindersView(
    reminders: List<Reminder>, rows: List<Row>, state: Map<String, Boolean>, onMark: (String, Boolean) -> Unit,
) {
    var detail by remember { mutableStateOf<Reminder?>(null) }
    val today = Dates.today()
    val curMonth = Dates.todayMonth()
    // Auto-settle: a real debit to the SAME merchant for the SAME amount within ±5 days of
    // the due date. Each debit is used at most once, so one payment can't tick two reminders.
    val autoSet = remember(reminders, rows) {
        val used = HashSet<String>()
        val matched = HashSet<String>()
        reminders.sortedBy { it.dueDate }.forEach { r ->
            val cp = r.counterparty?.lowercase()?.trim()
            if (cp.isNullOrEmpty()) return@forEach
            val window = (-5..5).map { Dates.shiftDays(r.dueDate, it) }.toSet()
            val debit = rows.firstOrNull { row ->
                row.id !in used &&
                    (row.kind == "expense" || row.kind == "transfer" || row.kind == "invest") &&
                    kotlin.math.abs(row.amount - r.amount) < 1.0 &&
                    window.contains(row.date) && run {
                        val rc = row.counterparty?.lowercase()?.trim()
                        !rc.isNullOrEmpty() && (rc == cp || rc.contains(cp) || cp.contains(rc))
                    }
            }
            if (debit != null) { used.add(debit.id); matched.add(r.id) }
        }
        matched
    }
    // An explicit paid/unpaid tap always wins; otherwise a previous-month due or a debit-matched
    // one is assumed paid.
    fun settled(r: Reminder) = state[r.id] ?: (r.dueDate.take(7) < curMonth || r.id in autoSet)

    val pending = reminders.filter { !settled(it) }.sortedBy { it.dueDate }
    val paid = reminders.filter { settled(it) }.sortedByDescending { it.dueDate }

    Spacer(Modifier.height(4.dp))
    Text("Dues & reminders", color = ZColor.Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    if (pending.isEmpty()) {
        Panel {
            Eyebrow("All clear")
            Spacer(Modifier.height(8.dp))
            MutedText("No bills to pay. Bill and EMI reminders from your SMS appear here — tap the circle to mark one paid.")
        }
    } else {
        Spacer(Modifier.height(4.dp))
        MutedText("Tap the circle to mark a bill paid. Overdue ones are in red.", 12)
        pending.forEach { r -> ReminderCard(r, overdue = r.dueDate < today, onOpen = { detail = r }) { onMark(r.id, true) } }
    }
    if (paid.isNotEmpty()) {
        Spacer(Modifier.height(18.dp))
        Eyebrow("Paid")
        paid.take(25).forEach { r -> ReminderCard(r, overdue = false, paid = true, onOpen = { detail = r }) { onMark(r.id, false) } }
    }
    Spacer(Modifier.height(6.dp))
    MutedText("Reminders are never counted as spending — only money that actually leaves your bank is.", 11)

    detail?.let { r -> ReminderDetailDialog(r) { detail = null } }
}

@Composable private fun ReminderDetailDialog(r: Reminder, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MerchantTile(r.counterparty ?: "Bill", "Bills", 40.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(r.counterparty?.takeIf { it.isNotBlank() } ?: r.account.ifBlank { "Bill" },
                        color = ZColor.Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    MutedText("Due ${prettyDate(r.dueDate)}" + if (r.recurring) " · Monthly" else "", 12)
                }
                Spacer(Modifier.width(10.dp))
                Text(money(r.amount), color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            if (r.minAmount != null) { Spacer(Modifier.height(6.dp)); MutedText("Minimum due: ${money(r.minAmount)}", 12) }
            if (r.account.isNotBlank()) { Spacer(Modifier.height(4.dp)); MutedText("Account: ${r.account}", 12) }
            Spacer(Modifier.height(16.dp))
            Eyebrow("From this message")
            Spacer(Modifier.height(6.dp))
            Text(
                r.raw.ifBlank { "(no message text stored)" }, color = ZColor.InkSoft, fontSize = 13.sp,
                modifier = Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState()),
            )
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier.align(Alignment.End).clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                    .clickable { onDismiss() }.padding(horizontal = 22.dp, vertical = 10.dp),
            ) { Text("Close", color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable private fun ReminderCard(r: Reminder, overdue: Boolean, paid: Boolean = false, onOpen: () -> Unit = {}, onTick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 10.dp).clip(RoundedCornerShape(16.dp))
            .background(ZColor.Card).border(1.dp, ZColor.Line, RoundedCornerShape(16.dp))
            .clickable { onOpen() }.padding(14.dp),
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


// ---------- Account chips with bank logos ----------

private fun accountLabel(a: String): String {
    if (a == "All") return "All accounts"
    val p = a.trim().split(" ")
    val last = p.last()
    return if (p.size > 1 && last.all { it.isDigit() })
        (if (a.contains(" CC")) "${p.first()} card ••$last" else "${p.first()} ••$last") else a
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun AccountChips(options: List<String>, selected: String, onPick: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { a ->
            val sel = a == selected
            Row(
                Modifier.clip(RoundedCornerShape(20.dp)).background(if (sel) ZColor.Ink else ZColor.Card)
                    .border(1.dp, if (sel) ZColor.Ink else ZColor.Line, RoundedCornerShape(20.dp))
                    .clickable { onPick(a) }.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(22.dp).clip(RoundedCornerShape(11.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                    val logo = if (a == "All") null else bankLogoRes(a)
                    when {
                        a == "All" -> androidx.compose.material3.Icon(Icons.Rounded.AccountBalance, null, tint = ZColor.Ink, modifier = Modifier.size(14.dp))
                        logo != null -> androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(logo), null, modifier = Modifier.size(16.dp))
                        else -> Text(a.take(1).uppercase(), color = bankColor(a), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(7.dp))
                Text(accountLabel(a), color = if (sel) ZColor.Paper else ZColor.Ink, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ---------- Trends ----------

private data class TrendBucket(
    val key: String, val label: String, val title: String,
    val rows: List<Row>, val spent: Double, val income: Double,
)

private fun dayMonth(d: String) = "${d.substring(8, 10).toInt()} ${MONTH_ABBR[d.substring(5, 7).toInt() - 1]}"

private fun weekStart(d: String): String {
    val dow = Dates.parse(d).get(java.util.Calendar.DAY_OF_WEEK) // Sun = 1
    return Dates.shiftDays(d, -((dow + 5) % 7))
}

private fun buildBuckets(rows: List<Row>, period: String): List<TrendBucket> {
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

private fun shortAmt(v: Double): String {
    val a = kotlin.math.abs(v)
    val t = when {
        a >= 100000 -> String.format(java.util.Locale.US, "%.1fL", a / 100000)
        a >= 1000 -> String.format(java.util.Locale.US, "%.0fK", a / 1000)
        else -> String.format(java.util.Locale.US, "%.0f", a)
    }
    return if (v < 0) "-$t" else t
}

private fun niceCeil(x: Double): Double {
    if (x <= 0) return 1000.0
    val mag = Math.pow(10.0, Math.floor(Math.log10(x)))
    return Math.ceil(x / mag * 2) / 2 * mag
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun TrendsScreen(
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
                    .border(1.dp, ZColor.Line, RoundedCornerShape(20.dp)).clickable { onBack() }, contentAlignment = Alignment.Center) {
                    Text("‹", color = ZColor.Ink, fontSize = 24.sp)
                }
                Spacer(Modifier.width(12.dp))
                Text("Trends", color = ZColor.Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TopIcon(onClick = { filterOpen = true }, dot = acct != "All" || cat != "All") { FunnelIcon() }
            }
            Spacer(Modifier.height(14.dp))
            ChipScroll {
                listOf("day" to "Day", "week" to "Week", "month" to "Month", "quarter" to "Quarter").forEach { (k, l) ->
                    Chip(l, period == k, filled = true) { period = k; sel = -1 }
                }
            }
            Spacer(Modifier.height(8.dp))
            ChipScroll {
                listOf("both" to "Spend & income", "saved" to "Saved", "cat" to "By category").forEach { (k, l) ->
                    Chip(l, mode == k) { mode = k }
                }
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
                            .clickable { onReview(b.title, b.rows.sortedByDescending { it.date }) }.padding(vertical = 12.dp),
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
