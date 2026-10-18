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
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Warning
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

class MainActivity : ComponentActivity() {
    // App lock: asks for the phone's own PIN / pattern / fingerprint when opening, and again after the app
    // has been away for over 30 seconds. Off by default; switched on from the menu.
    private var locked by mutableStateOf(false)
    private var authShowing = false
    private var stoppedAt = 0L
    private var skipNextAsk = false
    private lateinit var authLauncher: androidx.activity.result.ActivityResultLauncher<android.content.Intent>

    private fun lockEnabled() = getSharedPreferences("zelu_security", MODE_PRIVATE).getBoolean("lock", false)

    private fun askUnlock() {
        if (authShowing) return
        val km = getSystemService(KEYGUARD_SERVICE) as android.app.KeyguardManager
        val i = if (km.isDeviceSecure) km.createConfirmDeviceCredentialIntent("Unlock Zelu", "Confirm it's you") else null
        if (i == null) { locked = false; return }
        try { authShowing = true; authLauncher.launch(i) } catch (_: Exception) { authShowing = false; locked = false }
    }

    override fun onStart() {
        super.onStart()
        if (lockEnabled() && !locked && stoppedAt != 0L &&
            android.os.SystemClock.elapsedRealtime() - stoppedAt > 30_000L) locked = true
        if (locked) { if (skipNextAsk) skipNextAsk = false else askUnlock() }
    }

    override fun onStop() {
        super.onStop()
        stoppedAt = android.os.SystemClock.elapsedRealtime()
        val app = applicationContext
        Thread { ZeluWidget.refresh(app) }.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            authShowing = false
            skipNextAsk = true
            if (res.resultCode == RESULT_OK) { locked = false; stoppedAt = 0L }
        }
        locked = lockEnabled()
        val sp = getSharedPreferences("zelu_theme", MODE_PRIVATE)
        val sysDark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        ZTheme.dark = if (sp.contains("dark")) sp.getBoolean("dark", false) else sysDark
        ZTheme.palette = sp.getInt("palette", 0).coerceIn(0, PALETTES.size - 1)
        setContent {
            val scheme = if (ZTheme.dark)
                androidx.compose.material3.darkColorScheme(primary = ZColor.Accent, onPrimary = ZColor.AccentInk,
                    background = ZColor.Paper,
                    surface = ZColor.Card, onSurface = ZColor.Ink, onBackground = ZColor.Ink, surfaceVariant = ZColor.Card,
                    onSurfaceVariant = ZColor.Muted, outline = ZColor.Line)
            else androidx.compose.material3.lightColorScheme(primary = ZColor.Accent, onPrimary = ZColor.AccentInk,
                background = ZColor.Paper, surface = ZColor.Card, onSurface = ZColor.Ink, onBackground = ZColor.Ink,
                surfaceVariant = ZColor.Card, onSurfaceVariant = ZColor.Muted, outline = ZColor.Line)
            val view = androidx.compose.ui.platform.LocalView.current
            androidx.compose.runtime.SideEffect {
                val c = androidx.core.view.WindowCompat.getInsetsController(window, view)
                c.isAppearanceLightStatusBars = !ZTheme.dark
                c.isAppearanceLightNavigationBars = !ZTheme.dark
            }
            MaterialTheme(colorScheme = scheme) { if (locked) LockScreen { askUnlock() } else ZeluApp() }
        }
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
        Modifier.size(sizeDp.dp).clip(RoundedCornerShape((sizeDp * 0.26f).dp)).background(ZColor.Accent),
        contentAlignment = Alignment.Center,
    ) { Text("Z", color = ZColor.AccentInk, fontWeight = FontWeight.Bold, fontSize = (sizeDp * 0.5f).sp) }
}

private fun acctTypeLabel(t: String) = when (t) { "card" -> "Credit card"; "loan" -> "Loan"; "other" -> "Wallet"; else -> "Savings account" }

private fun rowsOfAccount(rows: List<Row>, key: String) = rows.filter { it.account == key || (it.counterparty ?: "") == key }

// "Not mine" on something that has payments: show them and ask once more. Removing deletes the account AND those payments from Zelu.
@Composable private fun DeclineConfirmDialog(c: Analytics.AcctCandidate, rows: List<Row>, onKeep: () -> Unit, onRemove: () -> Unit) {
    val mine = remember(rows, c.key) { rowsOfAccount(rows, c.key).sortedByDescending { it.date } }
    Dialog(onDismissRequest = onKeep) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp).heightIn(max = 620.dp)) {
            Text("Remove " + c.key + "?", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            MutedText("It has ${mine.size} ${if (mine.size == 1) "payment" else "payments"} (${money(mine.sumOf { it.amount })}). " +
                "If you remove it, these go from Zelu too: totals, payees, notifications.", 12)
            Spacer(Modifier.height(10.dp))
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                mine.take(10).forEach { r ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(r.counterparty?.takeIf { it.isNotBlank() } ?: r.category, color = ZColor.Ink, fontSize = 14.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            MutedText(prettyDate(r.date), 11)
                        }
                        Text(money(r.amount), color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (mine.size > 10) MutedText("and ${mine.size - 10} more", 12)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                Text("Remove all", color = ZColor.Warn, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onRemove() }.padding(12.dp))
                Spacer(Modifier.width(8.dp))
                Box(Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink).clickable { onKeep() }
                    .padding(horizontal = 22.dp, vertical = 10.dp)) { Text("Keep it", color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

private fun plural(n: Int, one: String, many: String) = if (n == 1) "$n $one" else "$n $many"

@Composable private fun AccountSetupScreen(
    cands: List<Analytics.AcctCandidate>, rows: List<Row>, onDone: (Map<String, Pair<String, String>>) -> Unit,
) {
    val checked = remember { mutableStateMapOf<String, Boolean>() }
    val dup = remember { mutableStateMapOf<String, String>() }      // account -> the one you said it duplicates
    val notDup = remember { mutableStateListOf<String>() }          // "different account" answers
    fun isOn(c: Analytics.AcctCandidate) = !dup.containsKey(c.key) && (checked[c.key] ?: c.sure)
    var expanded by remember { mutableStateOf<String?>(null) }
    var queue by remember { mutableStateOf<List<Analytics.AcctCandidate>>(emptyList()) }
    fun finish() = onDone(cands.associate { c ->
        c.key to (if (dup.containsKey(c.key)) ("duplicate" to dup.getValue(c.key))
        else (if (isOn(c)) "accepted" else "declined") to c.type)
    })
    fun next() { queue = queue.drop(1); if (queue.isEmpty()) finish() }
    Box(Modifier.fillMaxSize().background(ZColor.Paper)) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 18.dp, end = 18.dp, top = 46.dp, bottom = 16.dp)) {
            Text("Your accounts", color = ZColor.Ink, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            MutedText("Zelu found these in your messages. Keep the ones that are yours.", 13)
            Spacer(Modifier.height(12.dp))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                @Composable fun group(label: String, list: List<Analytics.AcctCandidate>) {
                    if (list.isEmpty()) return
                    Spacer(Modifier.height(10.dp))
                    Eyebrow(label)
                    list.forEach { c ->
                        val on = isOn(c)
                        val twin = c.dupOf?.takeIf { c.key !in notDup }
                        Column(Modifier.fillMaxWidth().padding(top = 10.dp).clip(RoundedCornerShape(16.dp)).background(ZColor.Card)
                            .border(1.dp, if (on) ZColor.Accent else ZColor.Line, RoundedCornerShape(16.dp)).padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val logo = bankLogoRes(c.key)
                                Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                                    if (logo != null) androidx.compose.foundation.Image(painter = androidx.compose.ui.res.painterResource(logo),
                                        contentDescription = null, modifier = Modifier.size(26.dp))
                                    else Text(c.key.take(1).uppercase(), color = ZColor.Accent, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(c.key, color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    MutedText(when (c.type) {
                                        "loan" -> plural(c.txns, "EMI", "EMIs") + " · " + money(c.amount)
                                        "card" -> plural(c.txns, "payment", "payments") + (c.limit?.let { " · limit ${money(it)}" } ?: "")
                                        else -> plural(c.txns, "payment", "payments") + (c.balance?.let { " · balance ${money(it)}" } ?: "")
                                    } + (if (!c.sure) " · not sure" else ""), 12)
                                }
                                Spacer(Modifier.width(10.dp))
                                Box(Modifier.size(34.dp).clip(RoundedCornerShape(17.dp))
                                    .background(if (on) ZColor.Accent else Color.Transparent)
                                    .border(1.5.dp, if (on) ZColor.Accent else ZColor.Line, RoundedCornerShape(17.dp))
                                    .clickable { checked[c.key] = !on; dup.remove(c.key) }, contentAlignment = Alignment.Center) {
                                    if (on) Text("✓", color = ZColor.AccentInk, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                            if (twin != null) {
                                Spacer(Modifier.height(8.dp))
                                if (dup.containsKey(c.key)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Marked as the same as $twin", color = ZColor.Muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                        Text("Undo", color = ZColor.Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                            modifier = Modifier.clickable { dup.remove(c.key) })
                                    }
                                } else {
                                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(ZColor.Paper).padding(10.dp)) {
                                        Text("Looks like the same account as $twin", color = ZColor.Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        Spacer(Modifier.height(6.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                            Text("Mark as duplicate", color = ZColor.Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                                modifier = Modifier.clickable { dup[c.key] = twin })
                                            Text("It's different", color = ZColor.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.clickable { notDup.add(c.key) })
                                        }
                                    }
                                }
                            }
                            if (c.sample.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(if (expanded == c.key) "Hide message" else "See the message", color = ZColor.Accent, fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable { expanded = if (expanded == c.key) null else c.key })
                                if (expanded == c.key) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(c.sample, color = ZColor.InkSoft, fontSize = 12.sp, maxLines = 8, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
                group("Savings accounts", cands.filter { it.type == "savings" })
                group("Credit cards", cands.filter { it.type == "card" })
                group("Loans", cands.filter { it.type == "loan" })
                group("Wallets and others", cands.filter { it.type == "other" })
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(8.dp))
            Cta("Continue") {
                val withSpend = cands.filter { !dup.containsKey(it.key) && !isOn(it) && rowsOfAccount(rows, it.key).isNotEmpty() }
                if (withSpend.isEmpty()) finish() else queue = withSpend
            }
        }
        queue.firstOrNull()?.let { c ->
            DeclineConfirmDialog(c, rows, onKeep = { checked[c.key] = true; next() }, onRemove = { next() })
        }
    }
}

@Composable private fun LockScreen(onUnlock: () -> Unit) {
    Box(Modifier.fillMaxSize().background(ZColor.Paper), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LogoMark(72f)
            Spacer(Modifier.height(18.dp))
            Text("Zelu is locked", color = ZColor.Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            MutedText("Use your phone's PIN, pattern or fingerprint.", 13)
            Spacer(Modifier.height(22.dp))
            Cta("Unlock", onUnlock)
        }
    }
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
    var menuPage by remember { mutableStateOf("") }
    var helpOpen by remember { mutableStateOf(false) }
    var reportOpen by remember { mutableStateOf(false) }
    var emailOpen by remember { mutableStateOf(false) }
    var fullConfirm by remember { mutableStateOf(false) }
    var themeOpen by remember { mutableStateOf(false) }
    var lockOn by remember { mutableStateOf(context.getSharedPreferences("zelu_security", android.content.Context.MODE_PRIVATE).getBoolean("lock", false)) }
    var undo by remember { mutableStateOf<Pair<String, Map<String, String?>>?>(null) }
    var unreadOpen by remember { mutableStateOf(false) }
    var checkOpen by remember { mutableStateOf(false) }
    var registry by remember { mutableStateOf(Storage.accountRegistry(context)) }
    var acctConfirmed by remember { mutableStateOf(Storage.accountsConfirmed(context)) }
    var declineFor by remember { mutableStateOf<Analytics.AcctCandidate?>(null) }
    var check by remember { mutableStateOf<Ingest.Check?>(null) }
    var autoBackup by remember { mutableStateOf(Backup.autoOn(context)) }
    var unreadList by remember { mutableStateOf<List<RawSms>?>(null) }
    var addFor by remember { mutableStateOf<RawSms?>(null) }
    var missedCount by remember { mutableStateOf(0) }
    var missedSeen by remember { mutableStateOf(Storage.missedSeen(context)) }
    var eraseOpen by remember { mutableStateOf(false) }
    var dupOk by remember { mutableStateOf(Storage.dupOk(context)) }
    var dupOpen by remember { mutableStateOf(false) }
    var sendersOpen by remember { mutableStateOf(false) }
    var senderList by remember { mutableStateOf<List<Ingest.UnknownSender>?>(null) }
    var catsOpen by remember { mutableStateOf(false) }
    var catOff by remember { mutableStateOf(Storage.categoriesOff(context)) }
    var splitFor by remember { mutableStateOf<Row?>(null) }
    var smsOk by remember { mutableStateOf(true) }
    val lastScan = remember { longArrayOf(0L) }
    fun noteMissed(n: Int) { missedCount = n; missedSeen = n; Storage.setMissedSeen(context, n) }
    var restoreUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) restoreUri = uri }
    var rangeOpen by remember { mutableStateOf(false) }
    var newCatCallback by remember { mutableStateOf<((String) -> Unit)?>(null) }
    var balances by remember { mutableStateOf<Map<String, Pair<Double, Long>>>(emptyMap()) }
    var limits by remember { mutableStateOf<Map<String, Pair<Double, Long>>>(emptyMap()) }
    var cardPrefs by remember { mutableStateOf<Map<String, Pair<Int, Double>>>(emptyMap()) }
    var cardOpen by remember { mutableStateOf<String?>(null) }
    var monthlyBudget by remember { mutableStateOf(0.0) }
    var budgetDlg by remember { mutableStateOf(false) }
    Analytics.BUDGET = monthlyBudget
    var lendPerson by remember { mutableStateOf<String?>(null) }
    var borrowPerson by remember { mutableStateOf<String?>(null) }
    var view by remember { mutableStateOf("ledger") }
    var period by remember { mutableStateOf("month") }
    var month by remember { mutableStateOf(Dates.todayMonth()) }
    var account by remember { mutableStateOf("All") }
    var daySel by remember { mutableStateOf(0) }
    var calOpen by remember { mutableStateOf(false) }
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
            registry = withContext(Dispatchers.IO) { Storage.accountRegistry(context) }
        }
    }

    fun saveAccountDecisions(d: Map<String, Pair<String, String>>, confirmed: Boolean) {
        scope.launch {
            withContext(Dispatchers.IO) {
                Storage.setAccountStatuses(context, d)
                if (confirmed) Storage.setAccountsConfirmed(context)
            }
            if (confirmed) acctConfirmed = true
            registry = withContext(Dispatchers.IO) { Storage.accountRegistry(context) }
            rows = withContext(Dispatchers.IO) { Storage.loadRows(context) }
            balances = withContext(Dispatchers.IO) { Storage.loadBalances(context) }
            limits = withContext(Dispatchers.IO) { Storage.loadLimits(context) }
            if (confirmed) stage = "app"
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
            val snap = withContext(Dispatchers.IO) { Storage.snapshot(context) }
            withContext(Dispatchers.IO) { Storage.setRefundLink(context, refundId, yes) }
            rows = withContext(Dispatchers.IO) { Storage.loadRows(context) }
            undo = (if (yes) "Refund matched to the spend" else "Refund kept separate") to snap
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

    fun rescan(full: Boolean = false, notify: Boolean = false, depth: Int = 5000) {
        scope.launch {
            loading = true
            lastScan[0] = System.currentTimeMillis()
            var added = 0
            var missed = 0
            withContext(Dispatchers.IO) {
                val fresh = full || Storage.dataVersion(context) < Storage.PARSER_VERSION
                if (fresh) Storage.clear(context)
                val inbox = SmsReader.readInbox(context, if (fresh) maxOf(depth, 20_000) else depth)
                added = Ingest.ingest(context, inbox).added
                Storage.setDataVersion(context, Storage.PARSER_VERSION)
                Storage.setReadStamp(context, System.currentTimeMillis(), inbox.filter { Parser.isFinancialSender(it.address) }.maxOfOrNull { it.date } ?: 0L)
                missed = Ingest.unreadable(context).size
            }
            missedCount = missed
            // Refresh everything (balances, card limits, fixed plan, prefs too), not just the rows.
            reload()
            loading = false
            if (stage == "scan") stage = "done"
            if (notify) Toast.makeText(context, if (full) "Read everything again" else if (added == 0) "Nothing new in your messages" else plural(added, "new payment", "new payments") + " found", Toast.LENGTH_LONG).show()
        }
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { res ->
        val smsOk = res[Manifest.permission.READ_SMS] == true && res[Manifest.permission.RECEIVE_SMS] == true
        if (smsOk) { stage = "scan"; rescan() } else { permDenied = true }
    }


    // Are notifications allowed? (Android 13+ needs a separate permission, and builds installed
    // before it was requested never got it.) Re-checked whenever the app comes back to the front.
    var notifBlocked by remember { mutableStateOf(false) }
    fun checkNotif() { notifBlocked = !Notifier.enabled(context) }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        checkNotif()
        if (!ok) Notifier.openSettings(context)
    }
    fun fixNotif() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, "android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED)
            notifLauncher.launch("android.permission.POST_NOTIFICATIONS")
        else Notifier.openSettings(context)
    }
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                checkNotif()
                smsOk = hasPerm()
                // Catch up on anything the phone held back while Zelu was closed.
                if (stage == "app" && hasPerm() && System.currentTimeMillis() - lastScan[0] > 60_000L) rescan()
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    // Best-effort local reminder on open: nudge about bills due within 3 days.
    LaunchedEffect(stage, reminders) {
        if (stage != "app") return@LaunchedEffect
        // Daily 9 AM check runs in the background (DueReceiver); this just makes sure it's scheduled.
        DueReminders.schedule(context)
        DailySummary.schedule(context)
    }

    LaunchedEffect(Unit) {
        rows = withContext(Dispatchers.IO) { Storage.loadRows(context) }
        reminders = withContext(Dispatchers.IO) { Storage.loadReminders(context) }
        reminderState = withContext(Dispatchers.IO) { Storage.loadReminderState(context) }
        repaid = withContext(Dispatchers.IO) { Storage.loadRepaid(context) }
        budgets = withContext(Dispatchers.IO) { Storage.loadBudgets(context) }
        delay(1200)
        if (hasPerm()) { stage = "app"; rescan() } else if (rows.isNotEmpty()) { stage = "app"; smsOk = false } else { stage = "welcome" }
    }

    val accounts = remember(rows) {
        listOf("All") + rows.map { it.account }.filter { it.isNotBlank() }.distinct().sorted()
    }
    // The account filter lists bank accounts only; credit cards are reached via their card tile.
    val bankAccounts = remember(accounts, registry) { accounts.filter { !it.contains(" CC") && (registry[it]?.first ?: "accepted") == "accepted" } }
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
            val snap = withContext(Dispatchers.IO) { Storage.snapshot(context) }
            withContext(Dispatchers.IO) {
                Storage.setOverride(context, id, kind, category)
                if (remember && !payee.isNullOrBlank()) Storage.setPayeeRule(context, payee, kind, category)
            }
            rows = withContext(Dispatchers.IO) { Storage.loadRows(context) }
            askOpen = false
            undo = (if (remember && !payee.isNullOrBlank()) "Moved $payee to $category" else "Moved to $category") to snap
        }
    }

    fun renamePayee(from: String, to: String) {
        scope.launch {
            val snap = withContext(Dispatchers.IO) { Storage.snapshot(context) }
            withContext(Dispatchers.IO) { Storage.setAlias(context, from, to) }
            rows = withContext(Dispatchers.IO) { Storage.loadRows(context) }
            undo = (if (to.isBlank()) "Name reset" else "Renamed to ${to.trim()}") to snap
        }
    }

    fun retitle(id: String, title: String) {
        scope.launch {
            val snap = withContext(Dispatchers.IO) { Storage.snapshot(context) }
            withContext(Dispatchers.IO) { Storage.setTitle(context, id, title) }
            rows = withContext(Dispatchers.IO) { Storage.loadRows(context) }
            undo = "Title changed" to snap
        }
    }

    fun undoLast() {
        val u = undo ?: return
        undo = null
        scope.launch {
            withContext(Dispatchers.IO) { Storage.restoreSnapshot(context, u.second) }
            rows = withContext(Dispatchers.IO) { Storage.loadRows(context) }
        }
    }

    // First time (or first time after this update): once messages are read, you confirm which accounts are yours.
    LaunchedEffect(stage, rows.size, acctConfirmed) {
        if (stage == "app" && !acctConfirmed && rows.isNotEmpty()) stage = "accounts"
        if (stage == "done" && acctConfirmed) stage = "app"
    }
    if (stage == "accounts") {
        val cands = remember(rows, balances, limits) { Analytics.accountCandidates(rows, balances, limits) }
        AccountSetupScreen(cands, rows) { d -> saveAccountDecisions(d, true) }
        return
    }
    if (stage != "app") {
        val accts = rows.map { it.account }.filter { it.isNotBlank() }.distinct()
        val cardsN = accts.count { it.contains(" CC") }
        OnboardFlow(stage, permDenied, Triple(rows.size, accts.size - cardsN, cardsN),
            onBack = { stage = if (stage == "perm") "privacy" else "welcome" },
            onNext = { stage = when (stage) { "welcome" -> "privacy"; "privacy" -> "perm"; "done" -> if (acctConfirmed) "app" else "accounts"; else -> stage } },
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
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 18.dp, end = 18.dp, top = 46.dp, bottom = 110.dp),
        ) {
            val advActiveTop = type != "all" || cat != "All" || query.isNotBlank() || account != "All" || excludeEmi
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                BrandBar()
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TopIcon(onClick = {
                        ZTheme.dark = !ZTheme.dark
                        context.getSharedPreferences("zelu_theme", android.content.Context.MODE_PRIVATE)
                            .edit().putBoolean("dark", ZTheme.dark).apply()
                    }) {
                        androidx.compose.material3.Icon(
                            if (ZTheme.dark) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                            null, tint = ZColor.Ink, modifier = Modifier.size(20.dp))
                    }
                    TopIcon(onClick = { controlsOpen = true }, dot = advActiveTop) { FunnelIcon() }
                    TopIcon(onClick = { menuOpen = true }, dot = missedCount > missedSeen) { MenuIcon() }
                }
            }
            Spacer(Modifier.height(14.dp))

            // A new account showed up in your messages after you set things up.
            val pendingAccts = remember(rows, balances, limits, registry, acctConfirmed) {
                if (!acctConfirmed) emptyList()
                else Analytics.accountCandidates(rows, balances, limits).filter { it.key !in registry && it.txns > 0 }
            }
            pendingAccts.firstOrNull()?.let { c ->
                Row(Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(16.dp)).background(ZColor.Card).border(1.dp, ZColor.Accent, RoundedCornerShape(16.dp))
                    .padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("New " + acctTypeLabel(c.type).lowercase() + " found", color = ZColor.Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(c.key + " · " + plural(c.txns, "payment", "payments") + (c.dupOf?.let { " · same as $it?" } ?: ""), color = ZColor.InkSoft, fontSize = 12.sp,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Text("Not mine", color = ZColor.Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { declineFor = c }.padding(horizontal = 10.dp, vertical = 10.dp))
                    if (c.dupOf != null) Text("Same", color = ZColor.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { saveAccountDecisions(mapOf(c.key to ("duplicate" to c.dupOf)), false) }.padding(horizontal = 10.dp, vertical = 10.dp))
                    Text("Add", color = ZColor.Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { saveAccountDecisions(mapOf(c.key to ("accepted" to c.type)), false) }.padding(horizontal = 12.dp, vertical = 10.dp))
                }
            }
            if (!smsOk) {
                Row(Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(16.dp)).background(ZColor.Card).border(1.dp, ZColor.Warn, RoundedCornerShape(16.dp))
                    .clickable {
                        try {
                            context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                android.net.Uri.fromParts("package", context.packageName, null)).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                        } catch (_: Exception) {}
                    }.padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Zelu can't read your messages", color = ZColor.Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Android may have switched off SMS permission for an app you open rarely. Tap, then Permissions › SMS › Allow.",
                            color = ZColor.InkSoft, fontSize = 12.sp)
                    }
                    Text("Fix", color = ZColor.Accent, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp))
                }
            }
            val dupPairs = remember(rows, dupOk) { Analytics.possibleDuplicates(rows, dupOk) }
            if (dupPairs.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(16.dp)).background(ZColor.Card).border(1.dp, ZColor.Accent, RoundedCornerShape(16.dp))
                    .clickable { dupOpen = true }.padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(plural(dupPairs.size, "possible duplicate", "possible duplicates"), color = ZColor.Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Same amount, account and day. Tap to check.", color = ZColor.InkSoft, fontSize = 12.sp)
                    }
                    Text("Review", color = ZColor.Accent, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp))
                }
            }
            declineFor?.let { c ->
                DeclineConfirmDialog(c, rows, onKeep = {
                    saveAccountDecisions(mapOf(c.key to ("accepted" to c.type)), false); declineFor = null
                }, onRemove = {
                    saveAccountDecisions(mapOf(c.key to ("declined" to c.type)), false); declineFor = null
                })
            }

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
                    OutlinedTextField(
                        value = query, onValueChange = { query = it }, singleLine = true,
                        placeholder = { Text("Search payee or amount") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    MutedText("Account", 12)
                    Spacer(Modifier.height(6.dp))
                    AccountChips(bankAccounts, account) { account = it; daySel = 0 }
                    Spacer(Modifier.height(12.dp))
                    MutedText("Show", 12)
                    Spacer(Modifier.height(6.dp))
                    ChipScroll {
                        listOf("all" to "All", "expense" to "Spent", "income" to "Received", "ignore" to "Ignored").forEach { (k, l) ->
                            val ic = when (k) {
                                "expense" -> Icons.Rounded.ArrowUpward to ZColor.Accent
                                "income" -> Icons.Rounded.ArrowDownward to ZColor.Income
                                "ignore" -> Icons.Rounded.VisibilityOff to ZColor.Muted
                                else -> Icons.Rounded.Category to ZColor.Ink
                            }
                            PickChip(l, type == k, { IconDot(ic.first, ic.second, 20.dp) }) { type = k; daySel = 0 }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    MutedText("Category", 12)
                    Spacer(Modifier.height(6.dp))
                    ChipScroll {
                        CAT_OPTS.forEach { (k, l) ->
                            PickChip(if (k == "All") "All" else l, cat == k, { CatDot(k, 20.dp) }) { cat = k; daySel = 0 }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        Text("Clear all", color = ZColor.Muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable {
                                query = ""; account = "All"; type = "all"; cat = "All"; daySel = 0
                            }.padding(horizontal = 14.dp, vertical = 10.dp))
                        Spacer(Modifier.width(8.dp))
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
            if (notifBlocked) Row(
                Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(16.dp)).background(ZColor.OrangeSoft)
                    .clickable { fixNotif() }.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.material3.Icon(Icons.Rounded.Warning, null, tint = ZColor.Accent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Alerts are off", color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("Allow notifications to get instant payment and balance alerts.", color = ZColor.InkSoft, fontSize = 12.sp)
                }
                Spacer(Modifier.width(8.dp))
                Text("Turn on", color = ZColor.Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            when (view) {
                "ledger" -> LedgerView(rows, shown, f, accounts, month, account, daySel,
                    { daySel = if (daySel == it) 0 else it }, askOpen, { askOpen = it }, ::decide,
                    { detailRow = it }, { title, r -> drill = title to r }, excludeEmi,
                    repaid, { lendPerson = it }, { borrowPerson = it }, fixed, { budgetsOpen = true }, balances, { cb -> newCatCallback = cb }, limits, reminders, { cardOpen = it }, { excludeEmi = !excludeEmi }, { budgetDlg = true }, { view = "dues" }, reminderState, Refunds.suggestions, { id, yes -> answerRefund(id, yes) }, budgets = budgets)
                "spend" -> {
                    val prevByCat = if (period == "month")
                        Analytics.categoryTotals(rows, Dates.shiftMonth(month, -1), account) else emptyMap()
                    val isMonth = f.period == "month"
                    val flexShown = shown
                    val emiSpent = Analytics.summarize(Analytics.filter(rows, f).filter { it.category == "EMI" }).spent.coerceAtLeast(0.0)
                    val fixedNote = when {
                        emiSpent <= 0 || cat == "EMI" -> null
                        excludeEmi -> "Leaves out " + money(emiSpent) + " of EMI"
                        else -> "Includes " + money(emiSpent) + " of EMI. Turn on Exclude EMI in the filter to leave it out."
                    }
                    val dayStr = if (isMonth && daySel > 0) "%s-%02d".format(month, daySel) else null
                    val dayShown = if (dayStr != null) shown.filter { it.date == dayStr } else shown
                    BucketsView(dayShown, prevByCat, budgets, { budgetsOpen = true }, { trendsOpen = true },
                        onCalendar = if (isMonth) ({ calOpen = !calOpen }) else null,
                        calActive = calOpen || daySel > 0,
                        dayLabel = if (dayStr != null) "$daySel ${Dates.monthLong(month)}" else null,
                        onClearDay = { daySel = 0 },
                        slot = {
                            if (isMonth && calOpen) DailyCalendarBlock(rows, shown, f, month, account, daySel,
                                { daySel = if (daySel == it) 0 else it }, { detailRow = it }, showList = false)
                        },
                        afterSummary = {
                            if (isMonth && month == Dates.todayMonth() && dayStr == null) {
                                val todayD = Dates.dayOfMonth()
                                val spentNow = Analytics.summarize(flexShown).spent.coerceAtLeast(0.0)
                                val prevM = Dates.shiftMonth(month, -1)
                                val prevList = Analytics.filter(rows, f.copy(month = prevM)).let { l ->
                                    if (excludeEmi && cat != "EMI") l.filter { it.category != "EMI" } else l }
                                val prevSame = Analytics.summarize(prevList.filter { (it.date.drop(8).toIntOrNull() ?: 99) <= todayD }).spent.coerceAtLeast(0.0)
                                val curSame = Analytics.summarize(flexShown.filter { (it.date.drop(8).toIntOrNull() ?: 99) <= todayD }).spent.coerceAtLeast(0.0)
                                SpendNowPanel(spentNow, Analytics.BUDGET, Dates.daysInMonth(month) - todayD + 1, todayD, curSame, prevSame, fixedNote)
                            }
                        }) { title, rows -> drill = title to rows }
                    if (dayStr == null) {
                        val wFrom: String; val wTo: String
                        if (isMonth) {
                            wFrom = "$month-01"
                            wTo = if (month == Dates.todayMonth()) Dates.today() else "$month-%02d".format(Dates.daysInMonth(month))
                        } else {
                            val ds = shown.filter { it.kind == "expense" }.map { it.date }
                            wFrom = ds.minOrNull() ?: Dates.today(); wTo = (ds.maxOrNull() ?: Dates.today()).coerceAtMost(Dates.today())
                        }
                        WeekdayPanel(flexShown, wFrom, wTo, fixedNote)
                        val recurring = remember(rows) { Analytics.recurring(rows, Dates.todayMonth()) }
                        RecurringPanel(recurring)
                    }
                    PaceView(rows, shown, f, month, account, excludeEmi)
                    if (isMonth && daySel > 0) DailyCalendarBlock(rows, shown, f, month, account, daySel,
                        { daySel = if (daySel == it) 0 else it }, { detailRow = it }, showCal = false)
                }
                "buckets" -> {
                    val prevByCat = if (period == "month")
                        Analytics.categoryTotals(rows, Dates.shiftMonth(month, -1), account) else emptyMap()
                    BucketsView(shown, prevByCat, budgets, { budgetsOpen = true }) { title, rows -> drill = title to rows }
                }
                "payees" -> PayeesView(shown, { title, rows -> drill = title to rows },
                    { payee, id, kind, cat -> decide(id, kind, cat, payee, true) }, { cb -> newCatCallback = cb },
                    ::renamePayee,
                    remember(rows, fixed) { Analytics.fixedSet(rows, Dates.todayMonth(), fixed) }, rows)
                "invest" -> InvestView(rows) { title, r -> drill = title to r }
                "pace" -> PaceView(rows, shown, f, month, account, excludeEmi)
                "dues" -> RemindersView(reminders, rows, reminderState, ::markReminder,
                    onAdd = { n, a, d, r -> scope.launch {
                        withContext(Dispatchers.IO) { Storage.addManualReminder(context, n, a, d, r) }
                        reminders = withContext(Dispatchers.IO) { Storage.loadReminders(context) } } },
                    onRemove = { id -> scope.launch {
                        withContext(Dispatchers.IO) { Storage.removeManualReminder(context, id) }
                        reminders = withContext(Dispatchers.IO) { Storage.loadReminders(context) } } })
            }
        }
        BottomNav(view, Modifier.align(Alignment.BottomCenter)) { view = it; daySel = 0 }
        undo?.let { u ->
            LaunchedEffect(u) { delay(6000); if (undo === u) undo = null }
            Row(Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = 88.dp).fillMaxWidth()
                .clip(RoundedCornerShape(14.dp)).background(ZColor.Ink).padding(start = 16.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(u.first, color = ZColor.Paper, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("UNDO", color = ZColor.Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { undoLast() }.padding(horizontal = 14.dp, vertical = 12.dp))
            }
        }
        androidx.activity.compose.BackHandler(trendsOpen) { trendsOpen = false }
        if (trendsOpen) TrendsScreen(rows, accounts, onBack = { trendsOpen = false }, onReview = { t, r -> drill = t to r })
        cardOpen?.let { a ->
            CardDetailScreen(
                account = a, allRows = rows, limit = limits[a]?.let { it.first - Analytics.cardUsedSince(rows, a, it) }, reminders = reminders,
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
                MutedText("Your overall spending limit per month. Shown on Home and used for the pace chart. Enter 0 for no budget.", 12)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = text, onValueChange = { v -> text = v.filter { it.isDigit() || it == '.' } },
                    singleLine = true, placeholder = { Text("₹ per month (0 = none)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(14.dp))
                Box(
                    Modifier.align(Alignment.End).clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                        .clickable {
                            val v = text.toDoubleOrNull() ?: 0.0
                            scope.launch {
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
        Dialog(onDismissRequest = { menuOpen = false; menuPage = "" }) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(ZColor.Card).padding(vertical = 8.dp)
                .heightIn(max = 640.dp).verticalScroll(rememberScrollState())) {
                fun shut() { menuOpen = false; menuPage = "" }
                if (menuPage.isEmpty()) {
                    Text("Menu", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
                    // Theme: one slim row with the palette's own colours as dots.
                    Row(Modifier.fillMaxWidth().clickable { shut(); themeOpen = true }.padding(horizontal = 18.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(ZColor.Accent.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                            androidx.compose.material3.Icon(Icons.Rounded.Palette, null, tint = ZColor.Accent, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Theme", color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text(PALETTES[ZTheme.palette].name, color = ZColor.Muted, fontSize = 12.sp)
                        }
                        val set = PALETTES[ZTheme.palette].let { if (ZTheme.dark) it.dark else it.light }
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            listOf(set.paper, set.card, set.ink, set.accent).forEach { c ->
                                Box(Modifier.size(12.dp).clip(RoundedCornerShape(6.dp)).background(Color(c)).border(1.dp, ZColor.Line, RoundedCornerShape(6.dp)))
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        androidx.compose.material3.Icon(Icons.Rounded.ChevronRight, null, tint = ZColor.Muted, modifier = Modifier.size(20.dp))
                    }
                    MenuRow("Money", "Budget, savings plan, investments", Icons.Rounded.AccountBalanceWallet) { menuPage = "money" }
                    MenuRow("Accounts", "Review, bring back removed ones", Icons.Rounded.AccountBalance) { menuPage = "accounts" }
                    MenuRow("Messages", if (missedCount > missedSeen) (missedCount - missedSeen).toString() + " new missed" else "Rescan, missed SMS, data check", Icons.Rounded.Receipt, ZColor.Warn) { menuPage = "messages" }
                    MenuRow("Backup and security", "Back up, restore, app lock", Icons.Rounded.Lock, ZColor.Income) { menuPage = "backup" }
                    MenuRow("Export CSV", "Save to Downloads/Zelu", Icons.Rounded.FileDownload, ZColor.Muted) {
                        shut()
                        val ok = Exporter.exportCsv(context, shown, "zelu-${Dates.today()}.csv")
                        Toast.makeText(context, if (ok) "Saved to Downloads/Zelu" else "Export needs Android 10+", Toast.LENGTH_SHORT).show()
                    }
                    MenuRow("Help", "How Zelu works, report a problem", Icons.Rounded.Lightbulb, ZColor.Accent) { menuPage = "help" }
                } else {
                    Row(Modifier.fillMaxWidth().clickable { menuPage = "" }.padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("‹", color = ZColor.Ink, fontSize = 26.sp)
                        Spacer(Modifier.width(12.dp))
                        Text(when (menuPage) { "money" -> "Money"; "accounts" -> "Accounts"; "messages" -> "Messages"; "backup" -> "Backup and security"; else -> "Help" },
                            color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    when (menuPage) {
                        "money" -> {
                            MenuRow("Budget", if (monthlyBudget > 0) money(monthlyBudget) + " a month" else "Not set", Icons.Rounded.AccountBalanceWallet) { shut(); budgetDlg = true }
                            MenuRow("Categories", "Choose which count as spending", Icons.Rounded.Category, ZColor.Accent) { shut(); catsOpen = true }
                            MenuRow("Savings plan", "What's left · SIP, EMI, goals", Icons.Rounded.Savings, ZColor.Income) { shut(); savingsOpen = true }
                            MenuRow("Investments", "EPF, mutual funds, SIPs", Icons.Rounded.TrendingUp, ZColor.Income) { shut(); view = "invest"; daySel = 0 }
                        }
                        "accounts" -> {
                            MenuRow("Review accounts", "Mark accounts as yours, a duplicate, or not mine", Icons.Rounded.AccountBalance) { shut(); stage = "accounts" }
                            val removed = registry.count { it.value.first == "declined" } + Storage.hiddenAccounts(context).size
                            MenuRow("Bring back removed accounts", if (removed > 0) "$removed removed or hidden" else "Nothing removed", Icons.Rounded.Restore, ZColor.Muted) {
                                if (removed > 0) {
                                    shut()
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            Storage.forgetDeclined(context)
                                            Storage.hiddenAccounts(context).forEach { Storage.setAccountHidden(context, it, false) }
                                        }
                                        registry = withContext(Dispatchers.IO) { Storage.accountRegistry(context) }
                                        reload()
                                        Toast.makeText(context, "Zelu will ask about them again", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }
                        "messages" -> {
                            val lr = Storage.lastRead(context)
                            val nb = Storage.newestBank(context)
                            val stale = nb > 0 && System.currentTimeMillis() - nb > 7L * 86_400_000L
                            MenuRow("Rescan messages", if (lr == 0L) "Look for anything new in your inbox" else "Last read " + agoText(lr) + (if (nb > 0) " · newest bank SMS " + agoText(nb) else ""),
                                Icons.Rounded.Restore, if (stale) ZColor.Warn else ZColor.Accent) { shut(); rescan(notify = true) }
                            MenuRow("Unrecognised senders", "A bank Zelu skipped? Tell it here", Icons.Rounded.AccountBalance, ZColor.Muted) {
                                shut(); sendersOpen = true; senderList = null
                                scope.launch { senderList = withContext(Dispatchers.IO) { Ingest.unrecognisedSenders(context) } }
                            }
                            MenuRow("Read older messages", "Goes back through your whole inbox. Can take a minute.", Icons.Rounded.Restore, ZColor.Muted) { shut(); rescan(notify = true, depth = 100_000) }
                            MenuRow("Missed SMS", if (missedCount > 0) "$missedCount waiting · add by hand or teach Zelu" else "Money messages Zelu couldn't read", Icons.Rounded.Receipt, ZColor.Warn) {
                                shut(); unreadOpen = true; unreadList = null
                                scope.launch { val l = withContext(Dispatchers.IO) { Ingest.unreadable(context) }; unreadList = l; noteMissed(l.size) }
                            }
                            MenuRow("Data check", "What was read, and whether balances add up", Icons.Rounded.Receipt) {
                                shut(); checkOpen = true; check = null
                                scope.launch { check = withContext(Dispatchers.IO) { Ingest.diagnose(context) } }
                            }
                            MenuRow("Read everything again", "Clears payments and reads all messages afresh. Your edits stay.", Icons.Rounded.Warning, ZColor.Muted) { shut(); fullConfirm = true }
                        }
                        "backup" -> {
                            val lb = Backup.lastAny(context)
                            MenuRow("Back up now", if (lb > 0) "Last saved " + java.text.SimpleDateFormat("d MMM", java.util.Locale.US).format(java.util.Date(lb)) + " · Downloads/Zelu" else "Save everything to Downloads/Zelu", Icons.Rounded.CloudUpload) {
                                shut()
                                scope.launch {
                                    val ok = withContext(Dispatchers.IO) {
                                        Exporter.exportText(context, "zelu-backup-${Dates.today()}.json", "application/json", Backup.export(context))
                                    }
                                    if (ok) Backup.markManual(context)
                                    Toast.makeText(context, if (ok) "Backup saved to Downloads/Zelu" else "Backup needs Android 10+", Toast.LENGTH_LONG).show()
                                }
                            }
                            MenuRow("Restore data", "Load a backup file", Icons.Rounded.Restore, ZColor.Muted) { shut(); restoreLauncher.launch(arrayOf("*/*")) }
                            MenuRow("Weekly backup", "Saved to Downloads/Zelu every week", Icons.Rounded.CloudUpload, if (autoBackup) ZColor.Income else ZColor.Muted,
                                trailing = { Text(if (autoBackup) "ON" else "OFF", color = if (autoBackup) ZColor.Income else ZColor.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold) }) {
                                autoBackup = !autoBackup; Backup.setAutoOn(context, autoBackup)
                            }
                            MenuRow("App lock", "Ask for your phone lock when opening Zelu", Icons.Rounded.Lock, if (lockOn) ZColor.Income else ZColor.Muted,
                                trailing = { Text(if (lockOn) "ON" else "OFF", color = if (lockOn) ZColor.Income else ZColor.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold) }) {
                                val km = context.getSystemService(android.content.Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
                                if (!lockOn && !km.isDeviceSecure) {
                                    Toast.makeText(context, "Set a screen lock (PIN, pattern or fingerprint) in your phone settings first", Toast.LENGTH_LONG).show()
                                } else {
                                    lockOn = !lockOn
                                    context.getSharedPreferences("zelu_security", android.content.Context.MODE_PRIVATE).edit().putBoolean("lock", lockOn).apply()
                                }
                            }
                            MenuRow("Erase everything", "Delete all Zelu data from this phone", Icons.Rounded.Warning, ZColor.Warn) { shut(); eraseOpen = true }
                        }
                        "erase" -> {}
                        else -> {
                            MenuRow("How Zelu works", "Reading messages, dues, accounts, backup", Icons.Rounded.Lightbulb) { shut(); helpOpen = true }
                            MenuRow("Report a problem", "Email " + Storage.reportEmail(context), Icons.Rounded.Warning, ZColor.Warn) { shut(); reportOpen = true }
                            MenuRow("Report address", Storage.reportEmail(context), Icons.Rounded.Palette, ZColor.Muted) { shut(); emailOpen = true }
                            MenuRow("Test alert", if (notifBlocked) "Alerts are off · tap to turn on" else "Send a sample payment alert",
                                Icons.Rounded.Notifications, if (notifBlocked) ZColor.Warn else ZColor.Accent) {
                                shut()
                                if (notifBlocked) fixNotif() else Notifier.notifyTest(context)
                            }
                            MenuRow("Keep Zelu alive", "Stop battery saver blocking SMS", Icons.Rounded.BatteryChargingFull) { shut(); Notifier.openBatterySettings(context) }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }

    if (sendersOpen) Dialog(onDismissRequest = { sendersOpen = false }) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp).heightIn(max = 620.dp)) {
            Text("Unrecognised senders", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            MutedText("Senders Zelu doesn't know as a bank, whose messages read like payments. If one is your bank, tap This is my bank and its messages are read, old ones too. Only add senders you trust.", 12)
            Spacer(Modifier.height(10.dp))
            val sl = senderList
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                when {
                    sl == null -> MutedText("Looking through your messages…", 13)
                    sl.isEmpty() -> MutedText("None. Every sender that sends payment messages is already read.", 13)
                    else -> sl.forEach { u ->
                        Column(Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(12.dp)).background(ZColor.Paper).padding(12.dp)) {
                            Text(u.address, color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            MutedText(plural(u.count, "message", "messages") + " that look like payments", 11)
                            Spacer(Modifier.height(4.dp))
                            u.samples.forEach { s2 -> Text(s2, color = ZColor.InkSoft, fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis); Spacer(Modifier.height(3.dp)) }
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                                Text("This is my bank", color = ZColor.Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
                                    scope.launch {
                                        withContext(Dispatchers.IO) { Storage.trustSender(context, u.address) }
                                        senderList = senderList?.filter { it.address != u.address }
                                        rescan(notify = true)
                                    }
                                })
                                Text("Not a bank", color = ZColor.Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable {
                                    scope.launch(Dispatchers.IO) { Storage.markSmsHandled(context, "sender|" + u.address.uppercase()) }
                                    senderList = senderList?.filter { it.address != u.address }
                                })
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.align(Alignment.End).clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                .clickable { sendersOpen = false }.padding(horizontal = 22.dp, vertical = 10.dp)) {
                Text("Close", color = ZColor.Paper, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (catsOpen) Dialog(onDismissRequest = { catsOpen = false }) {
        val names = (rows.filter { it.kind == "expense" || it.category in catOff }.map { it.category } + catOff).distinct().filter { it.isNotBlank() }.sorted()
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp).heightIn(max = 620.dp)) {
            Text("Categories", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            MutedText("Tap to choose whether a category counts as spending. Left out, its payments stay in your list under Ignored but are not in totals, the pace chart or budgets.", 12)
            Spacer(Modifier.height(10.dp))
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                names.forEach { c ->
                    val counts = c !in catOff
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable {
                        scope.launch {
                            withContext(Dispatchers.IO) { Storage.setCategoryCounts(context, c, !counts) }
                            catOff = withContext(Dispatchers.IO) { Storage.categoriesOff(context) }
                            rows = withContext(Dispatchers.IO) { Storage.loadRows(context) }
                        }
                    }.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        CatDot(c, 30.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(c, color = if (counts) ZColor.Ink else ZColor.Muted, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text(if (counts) "Counted" else "Not counted", color = if (counts) ZColor.Income else ZColor.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.align(Alignment.End).clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                .clickable { catsOpen = false }.padding(horizontal = 22.dp, vertical = 10.dp)) {
                Text("Close", color = ZColor.Paper, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    splitFor?.let { t ->
        val amts = remember(t.id) { mutableStateListOf(fmtPlain(t.amount), "") }
        val cats = remember(t.id) { mutableStateListOf(t.category, t.category) }
        val assigned = amts.sumOf { it.toDoubleOrNull() ?: 0.0 }
        val left = t.amount - assigned
        val ok = amts.size >= 2 && amts.all { (it.toDoubleOrNull() ?: 0.0) > 0 } && Math.abs(left) < 0.5
        Dialog(onDismissRequest = { splitFor = null }) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp)
                .heightIn(max = 640.dp).verticalScroll(rememberScrollState())) {
                Text("Split payment", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                MutedText((t.counterparty ?: t.category) + " · " + money(t.amount) + ". Give each part an amount and a category.", 12)
                Spacer(Modifier.height(10.dp))
                for (i in amts.indices) {
                    DropdownField("Part ${i + 1}", cats[i], CAT_OPTS.drop(1), catIcons = true, valueKey = cats[i]) { cats[i] = it }
                    OutlinedTextField(value = amts[i], onValueChange = { v -> amts[i] = v.filter { it.isDigit() || it == '.' } }, singleLine = true,
                        placeholder = { Text("Amount ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                }
                Text(if (Math.abs(left) < 0.5) "All assigned" else if (left > 0) money(left) + " left to assign" else money(-left) + " too much",
                    color = if (Math.abs(left) < 0.5) ZColor.Income else ZColor.Warn, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    if (amts.size < 5) Text("＋ Add part", color = ZColor.Accent, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { amts.add(if (left > 0) fmtPlain(left) else ""); cats.add(t.category) }.padding(vertical = 8.dp))
                    else Spacer(Modifier.width(1.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Cancel", color = ZColor.Muted, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { splitFor = null }.padding(12.dp))
                        Spacer(Modifier.width(6.dp))
                        Box(Modifier.clip(RoundedCornerShape(20.dp)).background(if (ok) ZColor.Ink else ZColor.Line).clickable {
                            if (ok) {
                                val parts = amts.indices.map { (amts[it].toDoubleOrNull() ?: 0.0) to cats[it] }
                                scope.launch {
                                    withContext(Dispatchers.IO) { Storage.setSplit(context, t.id, parts) }
                                    rows = withContext(Dispatchers.IO) { Storage.loadRows(context) }
                                    splitFor = null
                                    Toast.makeText(context, "Split into ${parts.size} parts", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }.padding(horizontal = 22.dp, vertical = 10.dp)) { Text("Save", color = if (ok) ZColor.Paper else ZColor.Muted, fontWeight = FontWeight.SemiBold) }
                    }
                }
            }
        }
    }

    if (dupOpen) {
        val pairs = Analytics.possibleDuplicates(rows, dupOk)
        if (pairs.isNotEmpty()) Dialog(onDismissRequest = { dupOpen = false }) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp).heightIn(max = 620.dp)) {
                Text("Possible duplicates", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                MutedText("Two payments with the same amount, account and day. If it is one payment reported twice, hide the extra one. If you really paid twice, keep both.", 12)
                Spacer(Modifier.height(10.dp))
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    pairs.forEach { (a, b) ->
                        Column(Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(12.dp)).background(ZColor.Paper).padding(12.dp)) {
                            listOf(a, b).forEach { r ->
                                Text((r.counterparty ?: r.category) + " · " + money(r.amount), color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                MutedText(prettyDate(r.date) + " · " + r.account + " · from " + r.source, 11)
                                Spacer(Modifier.height(4.dp))
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                                Text("Hide the extra one", color = ZColor.Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
                                    fun info(r: Row) = (if (!r.counterparty.isNullOrBlank()) 1 else 0) + (if (!r.ref.isNullOrBlank()) 1 else 0)
                                    val drop = if (info(b) > info(a)) a else b
                                    decide(drop.id, "ignore", "Duplicate", null, false)
                                })
                                Text("Keep both", color = ZColor.Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable {
                                    scope.launch {
                                        withContext(Dispatchers.IO) { Storage.markDupOk(context, a.id + "|" + b.id) }
                                        dupOk = withContext(Dispatchers.IO) { Storage.dupOk(context) }
                                    }
                                })
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.align(Alignment.End).clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                    .clickable { dupOpen = false }.padding(horizontal = 22.dp, vertical = 10.dp)) {
                    Text("Close", color = ZColor.Paper, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (eraseOpen) Dialog(onDismissRequest = { eraseOpen = false }) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp)) {
            Text("Erase everything?", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            MutedText("This deletes every payment, rule, account choice and setting Zelu keeps on this phone. Your messages are not touched. Backup files already saved in Downloads/Zelu stay there: delete them in your Files app if you don't want them.", 13)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                Text("Cancel", color = ZColor.Muted, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { eraseOpen = false }.padding(12.dp))
                Spacer(Modifier.width(8.dp))
                Box(Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Warn).clickable {
                    eraseOpen = false
                    scope.launch {
                        withContext(Dispatchers.IO) { Storage.eraseAll(context) }
                        ZeluWidget.refresh(context)
                        Toast.makeText(context, "Erased. Open Zelu again to start fresh.", Toast.LENGTH_LONG).show()
                        (context as? android.app.Activity)?.finishAffinity()
                    }
                }.padding(horizontal = 22.dp, vertical = 10.dp)) { Text("Erase", color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
            }
        }
    }

    if (fullConfirm) Dialog(onDismissRequest = { fullConfirm = false }) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp)) {
            Text("Read everything again?", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            MutedText("Zelu clears its payment list and reads all your messages from scratch. Renames, categories, accounts, budgets and payments you added by hand are kept.", 13)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                Text("Cancel", color = ZColor.Muted, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { fullConfirm = false }.padding(12.dp))
                Spacer(Modifier.width(8.dp))
                Box(Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink).clickable { fullConfirm = false; rescan(full = true, notify = true) }
                    .padding(horizontal = 22.dp, vertical = 10.dp)) { Text("Read again", color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
            }
        }
    }

    if (helpOpen) Dialog(onDismissRequest = { helpOpen = false }) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp).heightIn(max = 640.dp)) {
            Text("How Zelu works", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                @Composable fun topic(head: String, body: String) {
                    Text(head, color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    MutedText(body, 13)
                    Spacer(Modifier.height(12.dp))
                }
                topic("Private", "Zelu reads bank messages on this phone only. It asks for no internet access, so it cannot upload anything. Backups and reports leave the phone only when you send them.")
                topic("Stops reading messages", "Android can switch off an app's permissions when it is not opened for a while. Zelu shows a banner if that happens. To stop it: phone Settings › Apps › Zelu › Permissions, and turn off 'Remove permissions if app isn't used'. Also keep Menu › Help › Keep Zelu alive allowed.")
                topic("A bank is not read", "Zelu recognises banks by their sender name. If one is skipped, open Menu › Messages › Unrecognised senders and tap This is my bank. Its messages, old and new, are read from then on.")
                topic("Categories and splits", "Menu › Money › Categories lets you leave a whole category out of spending. To split one payment across categories, tap it, choose Change category, then Split.")
                topic("Duplicates", "If one payment is reported twice, Zelu shows a banner on Home. Hide the extra one, or keep both if you really paid twice.")
                topic("Payments", "Every bank or UPI message about money going out or coming in becomes a payment. Tap a payment to change its title or category. Zelu remembers the change for the same payee.")
                topic("Missed SMS", "Messages that mention money but could not be understood. A dot on the menu icon appears when new ones turn up. Open Menu › Messages › Missed SMS to add one by hand or hide it. Keep 'Do the same for similar messages' ticked and Zelu reads messages of that form by itself from then on.")
                topic("Rescan", "Zelu reads new messages as they arrive and again each time you open it. Menu › Messages › Rescan looks again right now. Read everything again starts the payment list afresh and keeps your edits.")
                topic("Dues", "Bills, EMIs and card dues from your messages show on Dues. Tap the circle once paid. UPI AutoPay mandates are listed at the bottom; Add to dues turns one into a monthly reminder.")
                topic("Accounts", "Zelu lists the accounts it finds. Mark each as yours, a duplicate of another, or not mine. Menu › Accounts reopens that list and brings removed ones back.")
                topic("Not covered", "Zelu reads SMS only. A payment that exists only in a chat app, an RCS bank chat or the bank's own app is not visible to it.")
                topic("Erase", "Menu › Backup and security › Erase everything deletes all of Zelu's data from this phone.")
                topic("Backup", "Back up saves one file to Downloads/Zelu with everything you taught Zelu. Restore loads it on a new phone. Weekly backup does it automatically.")
                topic("Something wrong?", "Menu › Help › Report a problem opens an email with the app details filled in. You choose what to include and press Send yourself.")
            }
            Spacer(Modifier.height(6.dp))
            Box(Modifier.align(Alignment.End).clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                .clickable { helpOpen = false }.padding(horizontal = 22.dp, vertical = 10.dp)) {
                Text("Close", color = ZColor.Paper, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (emailOpen) {
        var addr by remember { mutableStateOf(Storage.reportEmail(context)) }
        Dialog(onDismissRequest = { emailOpen = false }) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp)) {
                Text("Report address", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                MutedText("Problem reports are emailed here.", 12)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = addr, onValueChange = { addr = it.trim() }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    Text("Cancel", color = ZColor.Muted, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { emailOpen = false }.padding(12.dp))
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink).clickable {
                        if (addr.contains("@") && addr.contains(".")) { Storage.setReportEmail(context, addr); emailOpen = false }
                        else Toast.makeText(context, "Enter a valid email address", Toast.LENGTH_SHORT).show()
                    }.padding(horizontal = 22.dp, vertical = 10.dp)) { Text("Save", color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }

    if (reportOpen) {
        var what by remember { mutableStateOf("") }
        var withInfo by remember { mutableStateOf(true) }
        var withMissed by remember { mutableStateOf(false) }
        val to = remember { Storage.reportEmail(context) }
        Dialog(onDismissRequest = { reportOpen = false }) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp)
                .heightIn(max = 620.dp).verticalScroll(rememberScrollState())) {
                Text("Report a problem", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                MutedText("Opens an email to $to. Nothing is sent until you press Send in your mail app.", 12)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = what, onValueChange = { what = it }, placeholder = { Text("What went wrong?") },
                    minLines = 3, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                @Composable fun tick(on: Boolean, title: String, sub: String, flip: () -> Unit) {
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { flip() }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(22.dp).clip(RoundedCornerShape(6.dp)).background(if (on) ZColor.Accent else ZColor.Paper)
                            .border(1.dp, if (on) ZColor.Accent else ZColor.Line, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                            if (on) Text("✓", color = ZColor.AccentInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(title, color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(sub, color = ZColor.Muted, fontSize = 12.sp)
                        }
                    }
                }
                tick(withInfo, "App details", "Version, phone model and counts. No names or amounts.") { withInfo = !withInfo }
                tick(withMissed, "Messages Zelu couldn't read", "Up to 3 message texts, which include amounts") { withMissed = !withMissed }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    Text("Cancel", color = ZColor.Muted, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { reportOpen = false }.padding(12.dp))
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink).clickable {
                        scope.launch {
                            val sb = StringBuilder(what.trim()).append("\n\n")
                            if (withInfo) {
                                val ver = try { context.packageManager.getPackageInfo(context.packageName, 0).versionName } catch (_: Exception) { "?" }
                                sb.append("--\nZelu ").append(ver).append(" · parser ").append(Storage.PARSER_VERSION)
                                    .append("\nAndroid ").append(Build.VERSION.RELEASE).append(" · ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL)
                                    .append("\n").append(rows.size).append(" payments · ").append(reminders.size).append(" dues · ")
                                    .append(registry.size).append(" accounts decided\n")
                            }
                            if (withMissed) {
                                val miss = withContext(Dispatchers.IO) { Ingest.unreadable(context, 3) }
                                miss.forEach { sb.append("\n[").append(it.address).append("] ").append(it.body).append("\n") }
                            }
                            val mail = android.content.Intent(android.content.Intent.ACTION_SENDTO, android.net.Uri.parse("mailto:"))
                                .putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf(to))
                                .putExtra(android.content.Intent.EXTRA_SUBJECT, "Zelu problem report")
                                .putExtra(android.content.Intent.EXTRA_TEXT, sb.toString())
                            try { context.startActivity(android.content.Intent.createChooser(mail, "Send report")); reportOpen = false }
                            catch (_: Exception) { Toast.makeText(context, "No email app found on this phone", Toast.LENGTH_LONG).show() }
                        }
                    }.padding(horizontal = 22.dp, vertical = 10.dp)) { Text("Write email", color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }

    if (checkOpen) Dialog(onDismissRequest = { checkOpen = false }) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp).heightIn(max = 620.dp)) {
            Text("Data check", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            MutedText("Parser version ${Storage.PARSER_VERSION} · ${rows.size} transactions stored", 12)
            Spacer(Modifier.height(10.dp))
            val c = check
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                if (c == null) MutedText("Reading your messages…", 13) else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Fact("Bank SMS", c.bankSms.toString()); Fact("Payments", c.txns.toString()); Fact("Balances", c.balanceAlerts.toString())
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Fact("Reminders", c.reminders.toString())
                        Fact("Missed", c.unread.toString(), if (c.unread > 0) ZColor.Warn else ZColor.Income)
                        Fact("Hidden accts", Storage.hiddenAccounts(context).size.toString())
                    }
                    Spacer(Modifier.height(14.dp))
                    Eyebrow("Do balances add up?")
                    Spacer(Modifier.height(6.dp))
                    if (c.gaps.isEmpty()) MutedText("Yes. On every account, each alert's balance matches the one before it plus or minus the amount.", 13)
                    else {
                        MutedText("Where a balance jumps by more than the amount, a message is missing in between (deleted, or never sent).", 12)
                        Spacer(Modifier.height(6.dp))
                        c.gaps.forEach { (a, n) ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(a, color = ZColor.Ink, fontSize = 14.sp)
                                Text(if (n == 1) "1 gap" else "$n gaps", color = ZColor.Warn, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    val last = Backup.lastAuto(context)
                    MutedText(if (last > 0) "Last weekly backup: " + java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.US).format(java.util.Date(last))
                    else "No weekly backup saved yet.", 12)
                }
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.align(Alignment.End).clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                .clickable { checkOpen = false }.padding(horizontal = 22.dp, vertical = 10.dp)) {
                Text("Close", color = ZColor.Paper, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (unreadOpen) Dialog(onDismissRequest = { unreadOpen = false }) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp)
            .heightIn(max = 620.dp)) {
            Text("Missed messages", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            MutedText("Bank messages that mention money but Zelu couldn't turn into a payment. If one is a real payment, tap Add as payment and it joins your list. Not a payment hides it.", 12)
            Spacer(Modifier.height(10.dp))
            val list = unreadList
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                when {
                    list == null -> MutedText("Looking through your messages…", 13)
                    list.isEmpty() -> MutedText("Nothing missed. Every money message was understood.", 13)
                    else -> list.forEach { m ->
                        Column(Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(12.dp))
                            .background(ZColor.Paper).padding(12.dp)) {
                            MutedText(m.address + " · " + prettyDate(Dates.fmtMs(m.date)), 11)
                            Spacer(Modifier.height(4.dp))
                            Text(m.body, color = ZColor.Ink, fontSize = 12.sp, maxLines = 6, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("Add as payment", color = ZColor.Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { addFor = m })
                                Text("Not a payment", color = ZColor.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable {
                                        scope.launch(Dispatchers.IO) { Storage.markSmsHandled(context, Ingest.smsKey(m)) }
                                        unreadList = unreadList?.filter { it !== m }
                                        noteMissed(unreadList?.size ?: 0)
                                    })
                                Text("Share", color = ZColor.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable {
                                        val send = android.content.Intent(android.content.Intent.ACTION_SEND)
                                            .setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, m.body)
                                        try { context.startActivity(android.content.Intent.createChooser(send, "Share message")) } catch (_: Exception) {}
                                    })
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.align(Alignment.End).clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                .clickable { unreadOpen = false }.padding(horizontal = 22.dp, vertical = 10.dp)) {
                Text("Close", color = ZColor.Paper, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    addFor?.let { sms ->
        val credit = Regex("""(?i)\b(credited|received|deposited|refund(ed)?)\b""").containsMatchIn(sms.body)
        var amtText by remember(sms) { mutableStateOf(Parser.firstAmount(sms.body)?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "") }
        var isIn by remember(sms) { mutableStateOf(credit) }
        val sugg = remember(sms) { Parser.suggestTitles(sms.body, null) }
        var payee by remember(sms) { mutableStateOf(sugg.firstOrNull() ?: "") }
        var day by remember(sms) { mutableStateOf(Dates.fmtMs(sms.date)) }
        var learnIt by remember(sms) { mutableStateOf(true) }
        Dialog(onDismissRequest = { addFor = null }) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp)
                .heightIn(max = 620.dp).verticalScroll(rememberScrollState())) {
                Text("Add as payment", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                MutedText(sms.body.take(160) + if (sms.body.length > 160) "…" else "", 11)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("Spent", !isIn, true) { isIn = false }
                    Chip("Received", isIn, true) { isIn = true }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = amtText, onValueChange = { v -> amtText = v.filter { it.isDigit() || it == '.' } }, singleLine = true,
                    placeholder = { Text("Amount ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = payee, onValueChange = { payee = it }, singleLine = true,
                    placeholder = { Text("Paid to / from") }, modifier = Modifier.fillMaxWidth())
                if (sugg.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    ChipScroll { sugg.take(6).forEach { sName -> Chip(sName, payee == sName) { payee = sName } } }
                }
                Spacer(Modifier.height(10.dp))
                Box(Modifier.clip(RoundedCornerShape(20.dp)).border(1.dp, ZColor.Line, RoundedCornerShape(20.dp))
                    .clickable { pickDate(context, day) { day = it } }.padding(horizontal = 16.dp, vertical = 9.dp)) {
                    Text(prettyDate(day), color = ZColor.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { learnIt = !learnIt }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(22.dp).clip(RoundedCornerShape(6.dp)).background(if (learnIt) ZColor.Accent else ZColor.Paper)
                        .border(1.dp, if (learnIt) ZColor.Accent else ZColor.Line, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                        if (learnIt) Text("✓", color = ZColor.AccentInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Do the same for similar messages", color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Zelu learns this message's form and reads the next ones itself", color = ZColor.Muted, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    Text("Cancel", color = ZColor.Muted, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { addFor = null }.padding(12.dp))
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink).clickable {
                        val amount = amtText.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            val dir = if (isIn) "credit" else "debit"
                            val name = payee.trim().ifBlank { null }
                            val acct = Parser.accountFor(sms.body, sms.address, dir)
                            val id = "man-" + Ingest.smsKey(sms).hashCode().toUInt().toString(16)
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    val own = Storage.splitList(Storage.loadSettings(context).ownAccounts)
                                    val c = Classifier.classify(Txn(id, day, amount, dir, name, null, sms.body, sms.address, acct), own)
                                    Storage.addManualTxn(context, Row(id, day, amount, dir, name, null, sms.body, sms.address, acct, c.kind, c.category), Ingest.smsKey(sms))
                                    if (learnIt) Taught.learn(context, sms.body, sms.address, name, dir)
                                }
                                addFor = null
                                if (learnIt) rescan()
                                val l = withContext(Dispatchers.IO) { Ingest.unreadable(context) }
                                unreadList = l; noteMissed(l.size)
                                reload()
                                Toast.makeText(context, "Added to your payments", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }.padding(horizontal = 22.dp, vertical = 10.dp)) { Text("Add", color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }

    restoreUri?.let { uri ->
        Dialog(onDismissRequest = { restoreUri = null }) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp)) {
                Text("Restore this backup?", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                MutedText("It replaces your current transactions, categories, budgets and settings with the ones in the file.", 13)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    Text("Cancel", color = ZColor.Muted, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { restoreUri = null }.padding(12.dp))
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink).clickable {
                        restoreUri = null
                        scope.launch {
                            val ok = withContext(Dispatchers.IO) {
                                try {
                                    val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                                    text != null && Backup.restore(context, text)
                                } catch (_: Exception) { false }
                            }
                            if (ok) {
                                val sp = context.getSharedPreferences("zelu_theme", android.content.Context.MODE_PRIVATE)
                                ZTheme.palette = sp.getInt("palette", 0).coerceIn(0, PALETTES.size - 1)
                                if (sp.contains("dark")) ZTheme.dark = sp.getBoolean("dark", false)
                                reload()
                            }
                            Toast.makeText(context, if (ok) "Backup restored" else "That isn't a Zelu backup file", Toast.LENGTH_LONG).show()
                        }
                    }.padding(horizontal = 22.dp, vertical = 10.dp)) { Text("Restore", color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }

    if (themeOpen) Dialog(onDismissRequest = { themeOpen = false }) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(vertical = 10.dp)
            .heightIn(max = 620.dp).verticalScroll(rememberScrollState())) {
            Text("Theme", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp))
            PALETTES.forEachIndexed { i, p ->
                val set = if (ZTheme.dark) p.dark else p.light
                val sel = i == ZTheme.palette
                Row(
                    Modifier.fillMaxWidth().clickable {
                        ZTheme.palette = i
                        context.getSharedPreferences("zelu_theme", android.content.Context.MODE_PRIVATE)
                            .edit().putInt("palette", i).apply()
                    }.padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(Modifier.clip(RoundedCornerShape(10.dp)).border(1.dp, ZColor.Line, RoundedCornerShape(10.dp))) {
                        listOf(set.paper, set.card, set.ink, set.accent).forEach { c ->
                            Box(Modifier.size(width = 24.dp, height = 32.dp).background(Color(c)))
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(p.name, color = ZColor.Ink, fontSize = 15.sp,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
                    if (sel) Text("✓", color = ZColor.Accent, fontWeight = FontWeight.Bold)
                }
            }
            Text("Use the sun / moon button for light or dark.", color = ZColor.Muted, fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp))
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
            onRetitle = { title -> retitle(r.id, title); detailRow = null },
            onSplit = if (r.kind == "expense" || r.id.contains('#')) ({
                detailRow = null
                if (r.id.contains('#')) {
                    scope.launch {
                        withContext(Dispatchers.IO) { Storage.setSplit(context, r.id.substringBefore('#'), null) }
                        rows = withContext(Dispatchers.IO) { Storage.loadRows(context) }
                        Toast.makeText(context, "Split removed", Toast.LENGTH_SHORT).show()
                    }
                } else splitFor = r
            }) else null,
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

private fun agoText(ms: Long): String {
    val min = (System.currentTimeMillis() - ms) / 60_000L
    return when {
        min < 1 -> "just now"
        min < 60 -> "$min min ago"
        min < 1440 -> "${min / 60} h ago"
        else -> "${min / 1440} days ago"
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

// "₹2.37 L", "₹1.20 Cr", "₹57,328": short Indian-style amounts for the plan screen.
private fun inr(v: Double): String {
    val a = kotlin.math.abs(v)
    val t = when {
        a >= 1e7 -> String.format(Locale.US, "%.2f Cr", a / 1e7)
        a >= 1e5 -> String.format(Locale.US, "%.2f L", a / 1e5)
        else -> String.format(Locale.US, "%,.0f", a)
    }
    return (if (v < 0) "−₹" else "₹") + t
}

// Future value of 1 invested at the start of each month, for `months` months.
private fun sipFactor(months: Int, monthlyRate: Double): Double =
    if (monthlyRate == 0.0) months.toDouble()
    else ((Math.pow(1 + monthlyRate, months.toDouble()) - 1) / monthlyRate) * (1 + monthlyRate)

private fun yrMo(m: Int): String {
    val y = m / 12; val mo = m % 12
    return when { y == 0 -> "$mo mo"; mo == 0 -> "$y yr"; else -> "$y yr $mo mo" }
}

@Composable private fun SavingsDialog(rows: List<Row>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val saved = remember { Storage.loadPlan(context) }
    var sip by remember { mutableStateOf(saved["sip"] ?: "") }
    var emi by remember { mutableStateOf(saved["emi"] ?: "") }
    var goal by remember { mutableStateOf(saved["goal"] ?: "") }
    var years by remember { mutableStateOf(saved["years"] ?: "5") }
    var ret by remember { mutableStateOf(saved["ret"] ?: "12") }
    var emiOpen by remember { mutableStateOf((saved["emi"] ?: "").isNotBlank()) }
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
    val factor = sipFactor(n, r)
    val fv = sipN * factor
    val invested = sipN * n
    val needed = if (goalN > 0 && factor > 0) goalN / factor else 0.0
    val goalMonths = if (goalN > 0 && sipN > 0) (1..600).firstOrNull { sipN * sipFactor(it, r) >= goalN } else null
    fun save() {
        Storage.savePlan(context, mapOf("sip" to sip, "emi" to emi, "goal" to goal, "years" to years, "ret" to ret))
        onDismiss()
    }
    val sliderMax = (Math.ceil(maxOf(left, sipN, 10000.0) / 1000.0) * 1000.0).toFloat()
    @Composable fun field(label: String, v: String, set: (String) -> Unit) {
        OutlinedTextField(
            value = v, onValueChange = { x -> set(x.filter { it.isDigit() || it == '.' }) }, singleLine = true,
            label = { Text(label) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
    }
    Dialog(onDismissRequest = { save() }, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(ZColor.Paper)) {
            Column(
                Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 18.dp, end = 18.dp, top = 40.dp, bottom = 28.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Savings plan", color = ZColor.Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f))
                    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink).clickable { save() }
                        .padding(horizontal = 22.dp, vertical = 10.dp)) {
                        Text("Done", color = ZColor.Paper, fontWeight = FontWeight.SemiBold)
                    }
                }

                // 1. This month's picture
                Panel {
                    Eyebrow("Every month")
                    Spacer(Modifier.height(8.dp))
                    Text("You earn " + inr(avgIn) + " and spend " + inr(avgSpent) + ".", color = ZColor.Ink,
                        fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(if (left >= 0) inr(left) + " is left to save or invest." else "You overspend by " + inr(-left) + ".",
                        color = if (left >= 0) ZColor.Income else ZColor.Warn, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    if (avgIn > 0 && left >= 0) {
                        val sp = (avgSpent / avgIn).toFloat().coerceIn(0.001f, 0.999f)
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))) {
                            Box(Modifier.weight(sp).fillMaxHeight().background(ZColor.InkSoft))
                            Box(Modifier.weight(1f - sp).fillMaxHeight().background(ZColor.Income))
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            MutedText("Spent " + Math.round(sp * 100) + "%", 12)
                            MutedText("Left " + Math.round((1f - sp) * 100) + "%", 12)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    MutedText("Average of your last 3 full months. Spending already includes your EMIs.", 12)
                }

                // 2. SIP
                Panel {
                    Eyebrow("Your SIP")
                    Spacer(Modifier.height(6.dp))
                    Text(inr(sipN) + " a month", color = ZColor.Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    androidx.compose.material3.Slider(
                        value = sipN.toFloat().coerceIn(0f, sliderMax),
                        onValueChange = { sip = (Math.round(it / 1000f) * 1000).toString() },
                        valueRange = 0f..sliderMax, steps = (sliderMax / 1000f).toInt() - 1,
                        colors = androidx.compose.material3.SliderDefaults.colors(
                            thumbColor = ZColor.Accent, activeTrackColor = ZColor.Accent,
                            inactiveTrackColor = ZColor.Line, activeTickColor = Color.Transparent,
                            inactiveTickColor = Color.Transparent),
                    )
                    Text("Still free after SIP" + (if (emiN > 0) " and EMI" else "") + ": " + inr(free),
                        color = if (free >= 0) ZColor.Income else ZColor.Warn, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    if (free < 0) MutedText("That is more than you have left each month.", 12)
                    Spacer(Modifier.height(12.dp))
                    if (!emiOpen) {
                        Text("+ Planning a loan? Add an EMI", color = ZColor.Accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { emiOpen = true })
                    } else {
                        field("New EMI to plan for (₹ / month)", emi) { emi = it }
                        Spacer(Modifier.height(4.dp))
                        MutedText("Only add a new loan. Your current EMIs are already in your spending.", 12)
                    }
                }

                // 3. What it becomes
                Panel {
                    Eyebrow("What it becomes")
                    Spacer(Modifier.height(6.dp))
                    if (sipN > 0) {
                        Text(inr(fv), color = ZColor.Ink, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        MutedText("in " + fmtPlain(yrs) + " years at " + fmtPlain(rate) + "% a year", 13)
                        Spacer(Modifier.height(12.dp))
                        val inv = (invested / fv).toFloat().coerceIn(0.001f, 0.999f)
                        Row(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))) {
                            Box(Modifier.weight(inv).fillMaxHeight().background(ZColor.InkSoft))
                            Box(Modifier.weight(1f - inv).fillMaxHeight().background(ZColor.Accent))
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(ZColor.InkSoft))
                                Spacer(Modifier.width(6.dp)); MutedText("You put in " + inr(invested), 12)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(ZColor.Accent))
                                Spacer(Modifier.width(6.dp)); MutedText("Growth " + inr(fv - invested), 12)
                            }
                        }
                    } else MutedText("Move the SIP slider to see how it grows.", 13)
                    Spacer(Modifier.height(14.dp))
                    MutedText("Years", 12); Spacer(Modifier.height(6.dp))
                    ChipScroll { listOf(3, 5, 10, 15).forEach { y -> Chip("$y yr", num(years) == y.toDouble(), true) { years = y.toString() } } }
                    Spacer(Modifier.height(10.dp))
                    MutedText("Yearly return", 12); Spacer(Modifier.height(6.dp))
                    ChipScroll { listOf(8, 10, 12, 15).forEach { p -> Chip("$p%", num(ret) == p.toDouble(), true) { ret = p.toString() } } }
                    Spacer(Modifier.height(10.dp))
                    MutedText("Returns are an assumption, not a promise.", 11)
                }

                // 4. Goal
                Panel {
                    Eyebrow("Goal (optional)")
                    Spacer(Modifier.height(8.dp))
                    field("Goal amount (₹), e.g. a down payment", goal) { goal = it }
                    if (goalN > 0) {
                        Spacer(Modifier.height(12.dp))
                        Text("To reach " + inr(goalN) + " in " + fmtPlain(yrs) + " years you need " + inr(needed) + " a month.",
                            color = ZColor.Ink, fontSize = 15.sp)
                        Spacer(Modifier.height(6.dp))
                        when {
                            sipN <= 0 -> MutedText("Set a SIP above to see how long it takes.", 13)
                            sipN >= needed - 0.5 -> Text(
                                "Your " + inr(sipN) + " SIP gets you there in " + (goalMonths?.let { yrMo(it) } ?: "time") + " ✓",
                                color = ZColor.Income, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            else -> Text(
                                inr(needed - sipN) + " short each month. At your SIP it takes " +
                                    (goalMonths?.let { yrMo(it) } ?: "over 50 years") + ".",
                                color = ZColor.Warn, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        if (needed > left - emiN) {
                            Spacer(Modifier.height(4.dp))
                            MutedText("That is more than you have left each month. Raise the years or lower the goal.", 12)
                        }
                    }
                }
            }
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

@Composable private fun TxnDetailDialog(t: Row, onReclassify: (String, String) -> Unit, onNewCat: (((String) -> Unit)) -> Unit,
    onRetitle: (String) -> Unit = {}, onSplit: (() -> Unit)? = null, onDismiss: () -> Unit) {
    val titleIdeas = remember(t.id) { Parser.suggestTitles(t.raw, t.counterparty) }
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
            Spacer(Modifier.height(12.dp))
            var retitling by remember { mutableStateOf(false) }
            var newTitle by remember { mutableStateOf(t.counterparty ?: "") }
            Box(Modifier.clip(RoundedCornerShape(20.dp)).border(1.dp, ZColor.Line, RoundedCornerShape(20.dp))
                .clickable { retitling = !retitling }.padding(horizontal = 16.dp, vertical = 9.dp)) {
                Text(if (retitling) "Cancel" else "Edit title", color = ZColor.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            if (retitling) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = newTitle, onValueChange = { newTitle = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Who was this?") })
                if (titleIdeas.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    MutedText("Names in the message:", 11)
                    Spacer(Modifier.height(4.dp))
                    ChipScroll { titleIdeas.forEach { n -> Chip(n, newTitle.equals(n, ignoreCase = true)) { newTitle = n } } }
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.align(Alignment.End).clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                    .clickable { if (newTitle.isNotBlank()) onRetitle(newTitle) }.padding(horizontal = 20.dp, vertical = 9.dp)) {
                    Text("Save title", color = ZColor.Paper, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
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
                    if (onSplit != null) Chip(if (t.id.contains('#')) "Remove split" else "Split…", false) { onSplit() }
                }
            }
        }
    }
}

// ---------- shared pieces ----------

@Composable private fun BrandBar() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(ZColor.Accent),
            contentAlignment = Alignment.Center,
        ) { Text("Z", color = ZColor.AccentInk, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
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

@Composable private fun MenuRow(
    title: String, sub: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color = ZColor.Accent, trailing: (@Composable () -> Unit)? = null, onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 18.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center) {
            androidx.compose.material3.Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(sub, color = ZColor.Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (trailing != null) { Spacer(Modifier.width(8.dp)); trailing() }
        androidx.compose.material3.Icon(Icons.Rounded.ChevronRight, null, tint = ZColor.Muted, modifier = Modifier.size(20.dp))
    }
}

@Composable private fun PickChip(label: String, selected: Boolean, lead: (@Composable () -> Unit)? = null, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(20.dp)).background(if (selected) ZColor.Ink else ZColor.Card)
            .border(1.dp, if (selected) ZColor.Ink else ZColor.Line, RoundedCornerShape(20.dp))
            .clickable { onClick() }.padding(start = if (lead != null) 6.dp else 14.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (lead != null) { lead(); Spacer(Modifier.width(7.dp)) }
        Text(label, color = if (selected) ZColor.Paper else ZColor.Ink, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable private fun MenuSection(label: String) {
    Text(label.uppercase(), color = ZColor.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 2.dp))
}

@Composable private fun BottomNav(view: String, modifier: Modifier, onPick: (String) -> Unit) {
    val items = listOf("ledger" to "Home", "spend" to "Spend", "payees" to "Payees", "dues" to "Dues")
    Row(
        modifier.fillMaxWidth().background(ZColor.Card).border(1.dp, ZColor.Line)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 10.dp),
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
    label: String, valueLabel: String, options: List<Pair<String, String>>,
    catIcons: Boolean = false, valueKey: String = "", leadIcon: ((String) -> Pair<androidx.compose.ui.graphics.vector.ImageVector, Color>)? = null,
    onPick: (String) -> Unit,
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
                if (catIcons) { CatDot(valueKey, 26.dp); Spacer(Modifier.width(10.dp)) }
                else if (leadIcon != null) { leadIcon(valueKey).let { (ic, tn) -> IconDot(ic, tn, 26.dp) }; Spacer(Modifier.width(10.dp)) }
                Text(valueLabel, color = ZColor.Ink, fontSize = 14.sp, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text("▾", color = ZColor.Muted, fontSize = 12.sp)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false },
                modifier = Modifier.background(ZColor.Card).heightIn(max = 420.dp)) {
                options.forEach { (v, l) ->
                    DropdownMenuItem(
                        text = { Text(l, color = ZColor.Ink, fontWeight = if (v == valueKey) FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = if (catIcons) ({ CatDot(v, 28.dp) }) else if (leadIcon != null) ({ leadIcon(v).let { (ic, tn) -> IconDot(ic, tn, 28.dp) } }) else null,
                        onClick = { onPick(v); expanded = false })
                }
            }
        }
    }
}

@Composable private fun IconDot(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, size: androidx.compose.ui.unit.Dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(8.dp)).background(tint.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
        androidx.compose.material3.Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.58f))
    }
}

// Small tinted tile with the category's icon ("All" gets a generic grid icon).
@Composable private fun CatDot(cat: String, size: androidx.compose.ui.unit.Dp) {
    val all = cat == "All" || cat.isBlank()
    val tint = if (all) ZColor.Ink else catColor(cat)
    val icon = if (all) Icons.Rounded.Category else CatIcons.forCategory(cat)
    Box(Modifier.size(size).clip(RoundedCornerShape(8.dp)).background(tint.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center) {
        if (icon != null) androidx.compose.material3.Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.58f))
        else Text(cat.take(1).uppercase(), color = tint, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.42f).sp)
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
private fun glanceTips(ins: Analytics.Insight, spent: Double, income: Double, fixedCats: Set<String>): List<String> {
    val cat = ins.topCat
    val pct = ins.topCatPct
    val big = ins.biggestName
    val ob = ins.overBy
    val over = ob != null && ob > 0
    val under = ob != null && ob < 0
    val savings = if (income > 0) Math.round((income - spent) / income * 100).toInt() else null
    // Fixed costs (EMI, rent...) aren't something you trim, so no "cut $cat" advice for them.
    val fixedTop = cat.lowercase() in fixedCats
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
    }.filter { !fixedTop || !it.contains(cat) }.ifEmpty { listOf("Your fixed costs are most of this month. The flexible part is what you can steer.") }
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
    budgets: Map<String, Double> = emptyMap(),
) {
    val s = Analytics.summarize(list)
    val ins = Analytics.insight(list, f)
    // Committed costs (EMI, rent...) are learned from your data; At a glance looks at everyday spending.
    val fixedSet = remember(allRows, fixed) { Analytics.fixedSet(allRows, Dates.todayMonth(), fixed) }
    val flexIns = Analytics.insight(list.filter { !Analytics.isFixed(it, fixedSet) }, f) ?: ins
    val bigIds = Analytics.bigSpendIds(list)
    val pct = if (Analytics.BUDGET > 0) (s.spent / Analytics.BUDGET).toFloat() else 0f
    val budgetLeft = (Analytics.BUDGET - s.spent).coerceAtLeast(0.0)

    // Spent + budget
    Panel {
        val overBudget = s.spent > Analytics.BUDGET
        val emiAmt = list.filter { it.kind == "expense" && it.category == "EMI" }.sumOf { it.amount }
        Eyebrow("Spent in " + Analytics.periodLabel(f) + if (excludeEmi) " · after EMI" else "")
        Text(money(s.spent), color = ZColor.Ink, fontSize = 38.sp, fontWeight = FontWeight.Bold)
        if (Analytics.BUDGET > 0) {
            Spacer(Modifier.height(10.dp)); Meter(pct.coerceIn(0f, 1f), if (overBudget) ZColor.Warn else ZColor.Accent)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().clickable { onEditBudget() }, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    if (overBudget) money(s.spent - Analytics.BUDGET) + " over budget" else money(budgetLeft) + " left",
                    color = if (overBudget) ZColor.Warn else ZColor.Income, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                )
                MutedText("of " + money(Analytics.BUDGET) + " ›", 13)
            }
        } else {
            Spacer(Modifier.height(8.dp))
            Text("Set a monthly budget ›", color = ZColor.Accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onEditBudget() })
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

    // Heads up — budgets nearly used or exceeded, and cards running low (current month only)
    run {
        if (f.period == "month" && f.month == Dates.todayMonth()) {
            // (over?, ratio, icon, title, detail, onClick)
            data class Alert(val over: Boolean, val ratio: Double, val icon: androidx.compose.ui.graphics.vector.ImageVector,
                             val title: String, val detail: String, val go: () -> Unit)
            val alerts = ArrayList<Alert>()
            val totals = Analytics.categoryTotals(allRows, f.month, "All")
            val dayNow = Dates.dayOfMonth(); val dimNow = Dates.daysInMonth(f.month)
            budgets.forEach { (c, b) ->
                val v = totals[c] ?: 0.0
                if (b > 0 && v < b * 0.8 && dayNow >= 5 && v / b > dayNow.toDouble() / dimNow + 0.2) {
                    alerts.add(Alert(false, v / b, CatIcons.forCategory(c) ?: Icons.Rounded.Warning, c,
                        "Running ahead: " + (v / b * 100).toInt() + "% of the budget gone, " + (dayNow * 100 / dimNow) + "% of the month passed",
                    ) { onDrill(c, allRows.filter { it.kind == "expense" && it.category == c && it.date.startsWith(f.month) }) })
                }
                if (b > 0 && v >= b * 0.8) {
                    val over = v > b
                    alerts.add(Alert(over, v / b, CatIcons.forCategory(c) ?: Icons.Rounded.Warning,
                        c,
                        if (over) money(v - b) + " over your " + money(b) + " budget"
                        else money(b - v) + " left of your " + money(b) + " budget",
                    ) { onDrill(c, allRows.filter { it.kind == "expense" && it.category == c && it.date.startsWith(f.month) }) })
                }
            }
            accounts.filter { it.contains(" CC ") }.forEach { a ->
                val avail = limits[a]?.let { it.first - Analytics.cardUsedSince(allRows, a, it) }
                val total = limits["$a#total"]?.first
                if (avail != null && total != null && total > 0 && avail < total * 0.2) {
                    val used = 1.0 - avail / total
                    alerts.add(Alert(avail < total * 0.1, used, Icons.Rounded.CreditCard,
                        a + " is " + (used * 100).toInt() + "% used", money(avail) + " of " + money(total) + " left") { onOpenCard(a) })
                }
            }
            val shownAlerts = alerts.sortedByDescending { it.ratio }.take(3)
            val tdy = Dates.today()
            val spentNow = allRows.filter { it.kind == "expense" && it.date.startsWith(f.month) }.sumOf { it.amount }
            val duesLeft = reminders.filter { r -> !(reminderState[r.id] ?: r.done) && r.dueDate.take(7) == f.month && r.dueDate >= tdy }.sumOf { it.amount }
            val daysToGo = (Dates.daysInMonth(f.month) - Dates.dayOfMonth()).coerceAtLeast(0)
            val projectedEnd = spentNow + duesLeft + Analytics.typicalDaily(allRows, tdy) * daysToGo
            if (shownAlerts.isNotEmpty() || spentNow > 0) Panel {
                Eyebrow("For you")
                if (spentNow > 0) Row(Modifier.fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(12.dp)).clickable { onOpenDues() },
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(ZColor.IncomeSoft), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.Icon(Icons.Rounded.TrendingUp, null, tint = ZColor.Income, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Month-end forecast", color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Spent so far + dues left + a normal day's spend", color = ZColor.Muted, fontSize = 12.sp, maxLines = 2)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("≈ " + money(projectedEnd), color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                shownAlerts.forEach { a ->
                    val tone = if (a.over) ZColor.Warn else ZColor.AmberInk
                    val soft = if (a.over) ZColor.WarnSoft else ZColor.AmberSoft
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { a.go() },
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(soft), contentAlignment = Alignment.Center) {
                            androidx.compose.material3.Icon(a.icon, null, tint = tone, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(a.title, color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(a.detail, color = ZColor.Muted, fontSize = 12.sp, maxLines = 2)
                        }
                        Spacer(Modifier.width(10.dp))
                        Box(Modifier.clip(RoundedCornerShape(10.dp)).background(soft).padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text((a.ratio * 100).toInt().toString() + "%", color = tone, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
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
            // The actual SMS for both, so you can check the match yourself.
            @Composable fun smsBox(label: String, r: Row) {
                Spacer(Modifier.height(10.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(ZColor.Paper)
                    .border(1.dp, ZColor.Line, RoundedCornerShape(12.dp)).padding(12.dp)) {
                    Text(label.uppercase() + " · " + r.date + (if (r.account.isNotBlank()) " · " + r.account else ""),
                        color = ZColor.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(r.raw.ifBlank { "(message text not stored)" }, color = ZColor.InkSoft, fontSize = 12.sp,
                        lineHeight = 17.sp, maxLines = 6, overflow = TextOverflow.Ellipsis)
                }
            }
            smsBox("Refund message", rf)
            smsBox("Spend it may match", sp)
            Spacer(Modifier.height(12.dp))
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
        val fi = flexIns ?: ins
        Panel {
            Eyebrow("At a glance")
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(ZColor.Paper).padding(12.dp)) {
                    MerchantTile("", fi.topCat, 34.dp)
                    Spacer(Modifier.height(8.dp))
                    MutedText("Top category", 11)
                    Text(fi.topCat, color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(money(fi.topCatAmt), color = ZColor.Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("${fi.topCatPct}% of everyday spend", color = ZColor.Income, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(ZColor.OrangeSoft).padding(12.dp)) {
                    MerchantTile(fi.biggestName, "", 34.dp)
                    Spacer(Modifier.height(8.dp))
                    MutedText("Biggest spend", 11)
                    Text(fi.biggestName, color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(money(fi.biggestAmt), color = ZColor.Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }
            val tips = glanceTips(fi, s.spent, s.income, fixedSet.cats)
            if (tips.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                val seed = kotlin.math.abs(Dates.dayOfMonth() * 31 + fi.topCat.hashCode() + month.hashCode())
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(ZColor.IncomeSoft).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Icon(Icons.Rounded.Lightbulb, null, tint = ZColor.Income, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(tips[seed % tips.size], color = ZColor.InkSoft, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
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
    val acctCtx = LocalContext.current
    val acctScope = rememberCoroutineScope()
    var hiddenAccts by remember { mutableStateOf(Storage.hiddenAccounts(acctCtx)) }
    var sourceFor by remember { mutableStateOf<String?>(null) }
    var sourceRefs by remember { mutableStateOf<List<Ingest.Ref>?>(null) }
    val acctNames = (balances.keys + spentBy.keys).distinct().filter { !(it.contains(" CC ") || it.startsWith("CC ") || it.endsWith(" CC")) }
        .filter { it !in hiddenAccts }
        .sortedByDescending { balances[it]?.first ?: -1.0 }
    var allAccountsOpen by remember { mutableStateOf(false) }
    if (acctNames.isNotEmpty()) {
        Panel {
            Row(Modifier.fillMaxWidth().clickable { allAccountsOpen = true },
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Eyebrow("Accounts")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (balances.isNotEmpty())
                        Text(money(balances.filterKeys { it !in hiddenAccts }.values.sumOf { it.first }), color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
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
                MutedText("Tap an account to see the SMS it came from. You can keep it or hide it.", 12)
                acctNames.forEach { a ->
                    AccountCard(a, balances[a], spentBy[a] ?: 0.0, Modifier.fillMaxWidth().clickable {
                        sourceFor = a; sourceRefs = null
                        acctScope.launch { sourceRefs = withContext(Dispatchers.IO) { Ingest.referencesFor(acctCtx, a, allRows) } }
                    })
                }
                if (hiddenAccts.isNotEmpty()) {
                    Eyebrow("Hidden")
                    hiddenAccts.forEach { h ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(h, color = ZColor.Muted, fontSize = 14.sp)
                            Text("Show again", color = ZColor.Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { Storage.setAccountHidden(acctCtx, h, false); hiddenAccts = Storage.hiddenAccounts(acctCtx) }.padding(8.dp))
                        }
                    }
                }
            }
        }
    }
    sourceFor?.let { a ->
        Dialog(onDismissRequest = { sourceFor = null }) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp).heightIn(max = 640.dp)) {
                Text(a, color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                MutedText("Zelu made this account from these messages. If they aren't about an account of yours, hide it.", 12)
                Spacer(Modifier.height(10.dp))
                val refs = sourceRefs
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    when {
                        refs == null -> MutedText("Looking for the messages…", 13)
                        refs.isEmpty() -> MutedText("No message found for it any more.", 13)
                        else -> refs.forEach { r ->
                            Column(Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(12.dp)).background(ZColor.Paper).padding(12.dp)) {
                                MutedText(r.why + " · " + r.sender + " · " + (if (r.date.length == 10) prettyDate(r.date) else r.date), 11)
                                Spacer(Modifier.height(4.dp))
                                Text(r.body, color = ZColor.Ink, fontSize = 12.sp, maxLines = 8, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    Text("Hide account", color = ZColor.Warn, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { Storage.setAccountHidden(acctCtx, a, true); hiddenAccts = Storage.hiddenAccounts(acctCtx); sourceFor = null }.padding(12.dp))
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink).clickable { sourceFor = null }
                        .padding(horizontal = 22.dp, vertical = 10.dp)) { Text("Keep", color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
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
                    val today = Dates.today()
                    val due = reminders.filter { r -> !r.done && r.dueDate >= today && r.account.takeLast(4) == a.takeLast(4) }
                        .minByOrNull { it.dueDate }
                    CardTile(a, spent, limits[a], limits["$a#total"], due, usedSince = Analytics.cardUsedSince(allRows, a, limits[a])) { onOpenCard(a) }
                }
            }
            Spacer(Modifier.height(10.dp))
            MutedText("Tap a card to see its spends. Card spends count in your totals; paying the bill is a transfer.", 11)
        }
    }

    // Loans — found from EMI payments named after the loan ("HDFC Loan 529"), all-time.
    val loanGroups = allRows.filter { it.category == "EMI" && Regex(".*\\bLoan \\d{3,4}$").matches(it.counterparty ?: "") }
        .groupBy { it.counterparty!! }
    if (loanGroups.isNotEmpty()) {
        Panel {
            Eyebrow("Loans")
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                loanGroups.entries.sortedByDescending { e -> e.value.maxOf { it.date } }.forEach { (name, rs) ->
                    val last = rs.maxByOrNull { it.date }!!
                    val logo = bankLogoRes(name)
                    Column(Modifier.width(220.dp).clip(RoundedCornerShape(16.dp)).background(ZColor.Paper)
                        .border(1.dp, ZColor.Line, RoundedCornerShape(16.dp))
                        .clickable { onDrill(name, rs.sortedByDescending { it.date }) }.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                                if (logo != null) androidx.compose.foundation.Image(painter = androidx.compose.ui.res.painterResource(logo),
                                    contentDescription = null, modifier = Modifier.size(22.dp))
                                else Text(name.take(1).uppercase(), color = ZColor.Accent, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(name, color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                MutedText("Loan", 11)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        MutedText("Monthly EMI", 11)
                        Text(money(last.amount), color = ZColor.Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        MutedText("Last paid " + prettyDate(last.date), 11)
                        MutedText(plural(rs.size, "EMI", "EMIs") + " · " + money(rs.sumOf { it.amount }) + " paid", 11)
                    }
                }
            }
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
    MutedText("${allRows.size} transactions read" + (allRows.maxOfOrNull { it.date }?.let { " · newest " + prettyDate(it) } ?: "") +
        " · Private — everything stays on your phone.", 11)
}

@Composable private fun DailyCalendarBlock(
    allRows: List<Row>, list: List<Row>, f: Analytics.Filter, month: String, account: String,
    daySel: Int, onDay: (Int) -> Unit, onRowClick: (Row) -> Unit,
    showCal: Boolean = true, showList: Boolean = true,
) {
    val bigIds = Analytics.bigSpendIds(list)
    val daily = Analytics.dailyTotals(allRows, month, account)
    if (showCal) Panel {
        Eyebrow("Daily spend")
        Spacer(Modifier.height(8.dp))
        Calendar(month, daily, daySel, onDay)
        Spacer(Modifier.height(8.dp))
        MutedText("Darker days cost more. Tap a date to filter this page to that day.", 12)
    }
    if (showList && daySel > 0) {
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

@Composable private fun CardTile(account: String, spent: Double, limit: Pair<Double, Long>?, total: Pair<Double, Long>?, due: Reminder?, usedSince: Double = 0.0, onClick: () -> Unit = {}) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val parts = account.trim().split(" ")
    val last4 = parts.lastOrNull()?.takeIf { it.length in 3..4 && it.all { c -> c.isDigit() } } ?: account.takeLast(4)
    val bank = parts.firstOrNull() ?: account
    val base = bankColor(account)
    val logo = bankLogoRes(account)
    val shown = limit?.let { (it.first - usedSince) to it.second } ?: total
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
                        if (asOf != null) Text(if (limit != null && kotlin.math.abs(usedSince) >= 1.0) "est. · SMS of $asOf, less spends since" else "as of $asOf", color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Spent", color = soft, fontSize = 10.sp)
                        Text(money(spent), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(if (due != null) "Due " + due.dueDate.takeLast(5) + " · " + money(due.amount) else "No bill due",
                            color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp)
                    }
                }
            }
        }
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

@Composable private fun TonePill(text: String, fg: Color, bg: Color) {
    Box(Modifier.clip(RoundedCornerShape(10.dp)).background(bg).padding(horizontal = 8.dp, vertical = 3.dp)) {
        Text(text, color = fg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable private fun BucketsView(
    list: List<Row>, prevByCat: Map<String, Double> = emptyMap(),
    budgets: Map<String, Double> = emptyMap(), onEditBudgets: () -> Unit = {},
    onTrends: (() -> Unit)? = null,
    onCalendar: (() -> Unit)? = null, calActive: Boolean = false,
    dayLabel: String? = null, onClearDay: () -> Unit = {},
    slot: @Composable () -> Unit = {},
    afterSummary: @Composable () -> Unit = {},
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

    Panel {
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

    afterSummary()
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

@Composable private fun SpendNowPanel(spent: Double, budget: Double, daysLeft: Int, dayNum: Int, curSame: Double, prevSame: Double, note: String? = null) {
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

private fun ordinal(n: Int): String = if (n in 11..13) "th" else when (n % 10) { 1 -> "st"; 2 -> "nd"; 3 -> "rd"; else -> "th" }

@Composable private fun WeekdayPanel(list: List<Row>, from: String, to: String, note: String? = null) {
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

@Composable private fun RecurringPanel(list: List<Analytics.Recurring>) {
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

@Composable private fun Donut(segs: List<Pair<Color, Float>>, centerText: String, caption: String = "total",
                              dia: androidx.compose.ui.unit.Dp = 190.dp, ring: androidx.compose.ui.unit.Dp = 28.dp,
                              onSliceTap: (Int) -> Unit = {}) {
    Box(Modifier.size(dia), contentAlignment = Alignment.Center) {
        Canvas(
            Modifier.size(dia).pointerInput(segs) {
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
            val stroke = ring.toPx()
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
            MutedText(caption, 11)
        }
    }
}

// ---------- Payees ----------

@Composable private fun PayeesView(
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

    // 2. Search, sort and category filter.
    Spacer(Modifier.height(14.dp))
    OutlinedTextField(
        value = query, onValueChange = { query = it }, singleLine = true,
        placeholder = { Text("Search payee") }, modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(8.dp))
    ChipScroll {
        listOf("amount" to "Amount", "count" to "Most paid", "recent" to "Recent").forEach { (k, l) ->
            Chip(l, sort == k, true) { sort = k }
        }
    }
    val catsPresent = payees.flatMap { it.cats }.groupingBy { it }.eachCount().entries
        .sortedByDescending { it.value }.map { it.key }
    val sel = if (catSel in catsPresent) catSel else "All"
    Spacer(Modifier.height(8.dp))
    ChipScroll {
        Chip("All", sel == "All", true) { catSel = "All" }
        catsPresent.forEach { c -> Chip(customLabel(c), sel == c, true) { catSel = c } }
    }

    val q = query.trim().lowercase()
    val filtered = payees.filter { p -> (q.isEmpty() || p.name.lowercase().contains(q)) && (sel == "All" || sel in p.cats) }
    val ordered = when (sort) {
        "count" -> filtered.sortedByDescending { it.count }
        "recent" -> filtered.sortedByDescending { lastDate[it.name] ?: "" }
        else -> filtered
    }
    val split = q.isEmpty() && sel == "All"
    val fixedP = ordered.filter { p -> p.name.trim().lowercase() in fixedSet.payees || (p.cats.isNotEmpty() && p.cats.all { it.lowercase() in fixedSet.cats }) }
    val everyday = ordered.filter { it !in fixedP }

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
    run {
        val thisM = Dates.todayMonth()
        val firstSeen = HashMap<String, String>()
        allRows.filter { it.kind == "expense" }.forEach { r ->
            val n = nameOf(r); val cur = firstSeen[n]
            if (cur == null || r.date < cur) firstSeen[n] = r.date
        }
        val fresh = payees.filter { firstSeen[it.name]?.startsWith(thisM) == true }.take(6)
        if (fresh.isNotEmpty() && firstSeen.size > fresh.size) Panel {
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
    when {
        ordered.isEmpty() -> Panel { MutedText("No payee matches.") }
        split && fixedP.isNotEmpty() -> { section("Everyday spending", everyday); section("Fixed & recurring", fixedP) }
        else -> section(if (split) "All payees" else "Results", ordered)
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

@Composable private fun PayeeDetailDialog(
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
    onAdd: (String, Double, String, Boolean) -> Unit = { _, _, _, _ -> }, onRemove: (String) -> Unit = {},
) {
    var detail by remember { mutableStateOf<Reminder?>(null) }
    var adding by remember { mutableStateOf(false) }
    val ctxAdd = LocalContext.current
    var mandates by remember { mutableStateOf<List<Mandate>>(emptyList()) }
    var mandateOpen by remember { mutableStateOf<Mandate?>(null) }
    LaunchedEffect(reminders) { mandates = withContext(Dispatchers.IO) { Storage.loadMandates(ctxAdd) } }
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
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Dues & reminders", color = ZColor.Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Box(Modifier.clip(RoundedCornerShape(20.dp)).border(1.dp, ZColor.Line, RoundedCornerShape(20.dp))
            .clickable { adding = true }.padding(horizontal = 14.dp, vertical = 8.dp)) {
            Text("+ Add bill", color = ZColor.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }

    // Summary: what is overdue, what lands this week, and where the month is heading.
    val weekEnd = Dates.shiftDays(today, 7)
    val overdueList = pending.filter { it.dueDate < today }
    val weekList = pending.filter { it.dueDate >= today && it.dueDate <= weekEnd }
    val laterList = pending.filter { it.dueDate > weekEnd }
    val dim = Dates.daysInMonth(curMonth)
    val spentSoFar = rows.filter { it.kind == "expense" && it.date.startsWith(curMonth) }.sumOf { it.amount }
    val dueLeft = pending.filter { it.dueDate.take(7) == curMonth }.sumOf { it.amount }
    val typicalDaily = remember(rows) { Analytics.typicalDaily(rows, today) }
    val daysLeft = (dim - Dates.dayOfMonth()).coerceAtLeast(0)
    val projected = spentSoFar + dueLeft + typicalDaily * daysLeft
    Panel {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Fact("Overdue", money(overdueList.sumOf { it.amount }))
            Fact("Next 7 days", money(weekList.sumOf { it.amount }))
            Fact("Later", money(laterList.sumOf { it.amount }))
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (i in 0..6) {
                val d = Dates.shiftDays(today, i)
                val due = pending.filter { it.dueDate == d }
                val letter = "SMTWTFS"[Dates.parse(d).get(java.util.Calendar.DAY_OF_WEEK) - 1].toString()
                Column(Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                    .background(if (due.isNotEmpty()) ZColor.Warn.copy(alpha = 0.14f) else ZColor.Paper)
                    .padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(letter, color = ZColor.Muted, fontSize = 10.sp)
                    Text(d.substring(8, 10).toInt().toString(), color = ZColor.Ink, fontSize = 15.sp,
                        fontWeight = if (i == 0) FontWeight.Bold else FontWeight.Medium)
                    Text(if (due.isNotEmpty()) shortAmt(due.sumOf { it.amount }) else "·",
                        color = if (due.isNotEmpty()) ZColor.Warn else ZColor.Line, fontSize = 10.sp, maxLines = 1)
                }
            }
        }
        if (spentSoFar > 0) {
            Spacer(Modifier.height(14.dp))
            Eyebrow("Expected by month end")
            Spacer(Modifier.height(4.dp))
            Text("≈ " + money(projected), color = ZColor.Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            MutedText("${money(spentSoFar)} spent" + (if (dueLeft > 0) " + ${money(dueLeft)} dues left" else "") +
                (if (daysLeft > 0 && typicalDaily > 0) " + about ${money(typicalDaily)} a day for $daysLeft more days" else ""), 12)
        }
    }

    if (pending.isEmpty()) {
        Panel {
            Eyebrow("All clear")
            Spacer(Modifier.height(8.dp))
            MutedText("No bills to pay. Bill and EMI reminders from your SMS appear here — tap the circle to mark one paid.")
        }
    } else {
        Spacer(Modifier.height(4.dp))
        MutedText("Tap the circle to mark a bill paid.", 12)
        @Composable fun group(label: String, list: List<Reminder>, overdue: Boolean) {
            if (list.isEmpty()) return
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Eyebrow(label)
                MutedText(money(list.sumOf { it.amount }), 12)
            }
            list.forEach { r -> ReminderCard(r, overdue = overdue, onOpen = { detail = r }) { onMark(r.id, true) } }
        }
        group("Overdue", overdueList, true)
        group("This week", weekList, false)
        group("Later", laterList, false)
    }
    if (paid.isNotEmpty()) {
        Spacer(Modifier.height(18.dp))
        Eyebrow("Paid")
        paid.take(25).forEach { r -> ReminderCard(r, overdue = false, paid = true, onOpen = { detail = r }) { onMark(r.id, false) } }
    }
    if (mandates.isNotEmpty()) {
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Eyebrow("Autopay & mandates")
            MutedText("${mandates.count { it.status == "active" || it.status == "scheduled" }} active", 12)
        }
        Spacer(Modifier.height(4.dp))
        MutedText("UPI AutoPay set up from your bank messages. These debit on their own, so keep money in the account.", 12)
        Spacer(Modifier.height(6.dp))
        mandates.forEach { md ->
            val live = md.status == "active" || md.status == "scheduled"
            val tone = when (md.status) { "paused" -> ZColor.Warn; "ended" -> ZColor.Muted; else -> ZColor.Income }
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(14.dp)).background(ZColor.Card)
                .border(1.dp, ZColor.Line, RoundedCornerShape(14.dp)).clickable { mandateOpen = md }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                MerchantTile(md.merchant, "Bills", 38.dp, "expense")
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(md.merchant, color = if (live) ZColor.Ink else ZColor.Muted, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val live2 = md.status == "active" || md.status == "scheduled"
                    // A scheduled date in the past means that month's debit has happened: show the next monthly one.
                    val nxt = md.nextDate?.let { d0 -> var d = d0; var g = 0
                        while (d < today && g++ < 24) { val ym = Dates.shiftMonth(d.take(7), 1); d = ym + "-" + minOf(d.substring(8, 10).toInt(), Dates.daysInMonth(ym)).toString().padStart(2, '0') }
                        d }
                    MutedText((if (live2 && nxt != null) "Monthly · next " + prettyDate(nxt) else "Set up " + prettyDate(Dates.fmtMs(md.since))) +
                        (if (md.account.isNotBlank()) " · " + md.account else ""), 12)
                    val inDues = reminders.any { it.recurring && (it.counterparty ?: "").equals(md.merchant, ignoreCase = true) }
                    if (live2 && md.amount != null) {
                        if (inDues) Text("In your dues", color = ZColor.Income, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        else Text("Add to dues", color = ZColor.Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 2.dp).clickable { onAdd(md.merchant, md.amount, nxt ?: Dates.today(), true) })
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (md.amount != null) Text(money(md.amount), color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(when (md.status) { "paused" -> "PAUSED"; "ended" -> "ENDED"; else -> "ACTIVE" }, color = tone, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    Spacer(Modifier.height(6.dp))
    MutedText("Reminders are never counted as spending — only money that actually leaves your bank is.", 11)

    mandateOpen?.let { md ->
        Dialog(onDismissRequest = { mandateOpen = null }) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp).heightIn(max = 560.dp)) {
                Text(md.merchant, color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                MutedText("UPI AutoPay · " + when (md.status) { "paused" -> "paused"; "ended" -> "cancelled or expired"; else -> "active" } +
                    (md.amount?.let { " · up to " + money(it) } ?: ""), 12)
                Spacer(Modifier.height(10.dp))
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                    .clip(RoundedCornerShape(12.dp)).background(ZColor.Paper).padding(12.dp)) {
                    MutedText(md.source + " · " + prettyDate(Dates.fmtMs(md.lastMs)), 11)
                    Spacer(Modifier.height(4.dp))
                    Text(md.raw, color = ZColor.Ink, fontSize = 12.sp)
                }
                Spacer(Modifier.height(8.dp))
                MutedText("To stop an AutoPay, cancel it in your UPI app (Mandates / AutoPay). Zelu only reads the messages.", 11)
                Spacer(Modifier.height(10.dp))
                Box(Modifier.align(Alignment.End).clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                    .clickable { mandateOpen = null }.padding(horizontal = 22.dp, vertical = 10.dp)) {
                    Text("Close", color = ZColor.Paper, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
    detail?.let { r -> ReminderDetailDialog(r, onRemove = if (r.id.startsWith("man-")) ({ onRemove(r.id); detail = null }) else null) { detail = null } }
    if (adding) {
        var name by remember { mutableStateOf("") }
        var amt by remember { mutableStateOf("") }
        var due by remember { mutableStateOf(Dates.today()) }
        var monthly by remember { mutableStateOf(true) }
        Dialog(onDismissRequest = { adding = false }) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp)) {
                Text("Add a bill", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                MutedText("For things with no SMS: rent, school fee, a loan from a friend. Zelu reminds you before the due date.", 12)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, placeholder = { Text("What is it? (Rent)") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = amt, onValueChange = { v -> amt = v.filter { it.isDigit() || it == '.' } }, singleLine = true,
                    placeholder = { Text("Amount ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.clip(RoundedCornerShape(20.dp)).border(1.dp, ZColor.Line, RoundedCornerShape(20.dp))
                        .clickable { pickDate(ctxAdd, due) { due = it } }.padding(horizontal = 16.dp, vertical = 9.dp)) {
                        Text("Due " + prettyDate(due), color = ZColor.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.width(10.dp))
                    Chip("Every month", monthly, true) { monthly = !monthly }
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    Text("Cancel", color = ZColor.Muted, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { adding = false }.padding(12.dp))
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink).clickable {
                        val a = amt.toDoubleOrNull() ?: 0.0
                        if (name.isNotBlank() && a > 0) { onAdd(name.trim(), a, due, monthly); adding = false }
                    }.padding(horizontal = 22.dp, vertical = 10.dp)) { Text("Save", color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

@Composable private fun ReminderDetailDialog(r: Reminder, onRemove: (() -> Unit)? = null, onDismiss: () -> Unit) {
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                if (onRemove != null) {
                    Text("Remove bill", color = ZColor.Warn, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onRemove() }.padding(12.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Box(
                    Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                        .clickable { onDismiss() }.padding(horizontal = 22.dp, vertical = 10.dp),
                ) { Text("Close", color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
            }
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
    excludeEmi: Boolean = false, leave: Set<String> = emptySet(),
) {
    // The budget is for flexible spending: EMI and costs marked Fixed are left out here.
    val baseRows = allRows.filter { it.category !in leave && !(excludeEmi && it.category == "EMI") }
    Text("Are you ahead of budget?", color = ZColor.Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    if (leave.isNotEmpty()) MutedText("Leaves out EMI and fixed costs", 12)

    if (Analytics.BUDGET <= 0) {
        Panel { MutedText("Set a monthly budget from the menu to see whether you are ahead or behind.") }
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
