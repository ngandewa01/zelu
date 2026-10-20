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
        Privacy.load(this)
        IncomeCats.load(this)
        Demo.load(this)
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
            // Sample data runs the whole app on its own storage; switching it rebuilds the app from scratch.
            val demoOn = Demo.on
            val appContext = androidx.compose.runtime.remember(demoOn) { if (demoOn) DemoContext(this) else this }
            MaterialTheme(colorScheme = scheme) {
                if (locked) LockScreen { askUnlock() }
                else androidx.compose.runtime.CompositionLocalProvider(LocalContext provides appContext) {
                    androidx.compose.runtime.key(demoOn) { ZeluApp() }
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
    var crashAsk by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { crashAsk = withContext(Dispatchers.IO) { CrashLog.isNew(context) } }
    var emailOpen by remember { mutableStateOf(false) }
    var privacyOpen by remember { mutableStateOf(false) }
    var fullConfirm by remember { mutableStateOf(false) }
    var themeOpen by remember { mutableStateOf(false) }
    var lockOn by remember { mutableStateOf(context.getSharedPreferences("zelu_security", android.content.Context.MODE_PRIVATE).getBoolean("lock", false)) }
    var undo by remember { mutableStateOf<Pair<String, Map<String, String?>>?>(null) }
    var unreadOpen by remember { mutableStateOf(false) }
    var checkOpen by remember { mutableStateOf(false) }
    var registry by remember { mutableStateOf(Storage.accountRegistry(context)) }
    var acctConfirmed by remember { mutableStateOf(Storage.accountsConfirmed(context)) }
    var rescanRangeOpen by remember { mutableStateOf(false) }
    LaunchedEffect(registry) { LinkInfo.map = registry.filter { it.value.first == "duplicate" }.entries.groupBy({ it.value.second }, { it.key }) }
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
    var backupPwOpen by remember { mutableStateOf(false) }
    var backupAfterPw by remember { mutableStateOf(false) }
    fun backupNow() {
        scope.launch {
            val body = withContext(Dispatchers.IO) { Backup.exportEncrypted(context) }
            val ok = body != null && withContext(Dispatchers.IO) {
                Exporter.exportText(context, "zelu-backup-${Dates.today()}.json", "application/json", body)
            }
            if (ok) Backup.markManual(context)
            Toast.makeText(context, if (ok) "Backup saved to Downloads/Zelu" else "Backup needs Android 10+", Toast.LENGTH_LONG).show()
        }
    }
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
    var view by remember { mutableStateOf(context.getSharedPreferences("zelu_ui", android.content.Context.MODE_PRIVATE).getString("start", "ledger") ?: "ledger") }
    var period by remember { mutableStateOf("month") }
    var month by remember { mutableStateOf(Dates.todayMonth()) }
    var account by remember { mutableStateOf("All") }
    var daySel by remember { mutableStateOf(0) }
    var calOpen by remember { mutableStateOf(false) }
    // Spend page sub-tab: 0 Overview, 1 Categories, 2 Patterns.
    var spendTab by remember { mutableStateOf(0) }
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
    var onbSms by remember { mutableStateOf(false) }
    var onbNotif by remember { mutableStateOf(false) }
    var detailRow by remember { mutableStateOf<Row?>(null) }
    var drill by remember { mutableStateOf<Pair<String, List<Row>>?>(null) }

    fun hasPerm() = context is DemoContext || ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
        PackageManager.PERMISSION_GRANTED

    fun reload() {
        scope.launch {
            // Read everything first, then show it in one go, so balances never trail behind the payments.
            class Loaded(
                val rows: List<Row>, val reminders: List<Reminder>, val reminderState: Map<String, Boolean>,
                val repaid: Map<String, Double>, val budgets: Map<String, Double>, val monthlyBudget: Double,
                val fixed: Map<String, Double>, val balances: Map<String, Pair<Double, Long>>,
                val limits: Map<String, Pair<Double, Long>>, val manual: Set<String>,
                val cardPrefs: Map<String, Pair<Int, Double>>, val customCats: Map<String, Pair<String, Long>>,
                val registry: Map<String, Pair<String, String>>,
            )
            val l = withContext(Dispatchers.IO) {
                IncomeCats.load(context)
                Loaded(
                    Storage.loadRows(context), Storage.loadReminders(context), Storage.loadReminderState(context),
                    Storage.loadRepaid(context), Storage.loadBudgets(context), Storage.loadMonthlyBudget(context),
                    Storage.loadFixed(context), Storage.loadBalances(context), Storage.loadLimits(context),
                    Storage.manualLimitKeys(context), Storage.loadCardPrefs(context), Storage.loadCustomCats(context),
                    Storage.accountRegistry(context),
                )
            }
            LimitAsk.manual = l.manual
            CardNet.map = withContext(Dispatchers.IO) { Storage.cardNetworks(context) }
            cardPrefs = l.cardPrefs
            CustomCats.map = l.customCats
            registry = l.registry
            reminders = l.reminders; reminderState = l.reminderState; repaid = l.repaid
            budgets = l.budgets; monthlyBudget = l.monthlyBudget; fixed = l.fixed
            balances = l.balances; limits = l.limits
            rows = l.rows
        }
    }

    // Continue / Done on the Your accounts screen: what is shown and not removed is yours, so Zelu stops asking about it.
    fun confirmAccounts() {
        scope.launch {
            withContext(Dispatchers.IO) { Storage.confirmShownAccounts(context); Storage.setAccountsConfirmed(context) }
            acctConfirmed = true
            undo = null
            registry = withContext(Dispatchers.IO) { Storage.accountRegistry(context) }
            stage = "app"
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

    fun rescanRange(fromDay: String, toDay: String) {
        scope.launch {
            loading = true
            var found = 0
            var added = 0
            var missed = 0
            withContext(Dispatchers.IO) {
                val from = Dates.parse(fromDay).timeInMillis
                val to = Dates.parse(toDay).timeInMillis + 86_400_000L - 1
                val inbox = SmsReader.readInboxBetween(context, from, to)
                found = inbox.count { Parser.isFinancialSender(it.address) }
                added = Ingest.ingest(context, inbox).added
                missed = Ingest.unreadable(context).size
            }
            missedCount = missed
            reload()
            loading = false
            Toast.makeText(context, plural(found, "bank message", "bank messages") + " in that period · " +
                (if (added == 0) "nothing new" else plural(added, "new payment", "new payments")), Toast.LENGTH_LONG).show()
        }
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { res ->
        val smsOk = res[Manifest.permission.READ_SMS] == true && res[Manifest.permission.RECEIVE_SMS] == true
        onbSms = smsOk
        onbNotif = Notifier.enabled(context)
        // Show the ticks for a moment before moving on.
        if (smsOk) scope.launch { delay(900); stage = "scan"; rescan() } else { permDenied = true }
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
        WeeklySummary.schedule(context)
    }

    LaunchedEffect(Unit) {
        rows = withContext(Dispatchers.IO) { Storage.loadRows(context) }
        reminders = withContext(Dispatchers.IO) { Storage.loadReminders(context) }
        reminderState = withContext(Dispatchers.IO) { Storage.loadReminderState(context) }
        repaid = withContext(Dispatchers.IO) { Storage.loadRepaid(context) }
        budgets = withContext(Dispatchers.IO) { Storage.loadBudgets(context) }
        monthlyBudget = withContext(Dispatchers.IO) { Storage.loadMonthlyBudget(context) }
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
            registry = withContext(Dispatchers.IO) { Storage.accountRegistry(context) }
            rows = withContext(Dispatchers.IO) { Storage.loadRows(context) }
            balances = withContext(Dispatchers.IO) { Storage.loadBalances(context) }
            limits = withContext(Dispatchers.IO) { Storage.loadLimits(context) }
        }
    }

    fun relink(msg: String, change: () -> Unit) {
        scope.launch {
            val snap = withContext(Dispatchers.IO) { Storage.snapshot(context) }
            withContext(Dispatchers.IO) { change() }
            registry = withContext(Dispatchers.IO) { Storage.accountRegistry(context) }
            rows = withContext(Dispatchers.IO) { Storage.loadRows(context) }
            balances = withContext(Dispatchers.IO) { Storage.loadBalances(context) }
            limits = withContext(Dispatchers.IO) { Storage.loadLimits(context) }
            undo = msg to snap
        }
    }
    fun saveAccountPlan(plan: AccountBook.Plan, msg: String) = relink(msg) { Storage.applyAccountPlan(context, plan) }
    fun declineAcct(key: String, type: String) = relink("Removed " + AccountBook.displayName(key)) { Storage.declineAccount(context, key, type) }
    fun restoreAcct(key: String) = relink("Brought back " + AccountBook.displayName(key)) { Storage.restoreAccount(context, key) }

    // First time (or first time after this update): once messages are read, you confirm which accounts are yours.
    LaunchedEffect(stage, rows.size, acctConfirmed) {
        if (stage == "app" && !acctConfirmed && rows.isNotEmpty()) stage = "accounts"
        if (stage == "done" && acctConfirmed) stage = "app"
    }
    if (stage == "accounts") {
        val book = remember(rows, balances, limits, registry) { Storage.accountBook(context) }
        undo?.let { u -> LaunchedEffect(u) { delay(6000); if (undo === u) undo = null } }
        AccountsScreen(book, firstRun = !acctConfirmed, undoMsg = undo?.first, onUndo = { undoLast() },
            onSave = { plan, msg -> saveAccountPlan(plan, msg) }, onDecline = { k, t -> declineAcct(k, t) },
            onRestore = { k -> restoreAcct(k) }, onDone = { confirmAccounts() }, rows = rows)
        return
    }
    if (stage != "app") {
        val accts = rows.map { it.account }.filter { it.isNotBlank() }.distinct()
        val cardsN = accts.count { it.contains(" CC") }
        OnboardFlow(stage, permDenied, Triple(rows.size, accts.size - cardsN, cardsN),
            smsGiven = onbSms, notifGiven = onbNotif,
            onBack = { stage = if (stage == "perm") "privacy" else "welcome" },
            onNext = { stage = when (stage) { "welcome" -> "privacy"; "privacy" -> "perm"; "done" -> if (acctConfirmed) "app" else "accounts"; else -> stage } },
            onAllow = {
                val perms = mutableListOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
                if (Build.VERSION.SDK_INT >= 33) perms.add("android.permission.POST_NOTIFICATIONS")
                permLauncher.launch(perms.toTypedArray())
            })
        return
    }

    val pageScroll = rememberScrollState()
    val pageAlpha = remember { Animatable(1f) }
    LaunchedEffect(view) {                              // each tab opens at its top and fades in
        pageScroll.scrollTo(0)
        pageAlpha.snapTo(0.35f); pageAlpha.animateTo(1f, tween(180))
    }
    Box(Modifier.fillMaxSize().background(ZColor.Paper)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(pageScroll)
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
                else Storage.accountBook(context).filter { !it.declined && it.key !in registry && it.count > 0 && (it.count >= 2 || it.balance != null || it.limit != null || it.type == "loan") }
            }
            pendingAccts.firstOrNull()?.let { c ->
                Row(Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(16.dp)).background(ZColor.Card).border(1.dp, ZColor.Accent, RoundedCornerShape(16.dp))
                    .clickable { stage = "accounts" }.padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (pendingAccts.size == 1) "New account found" else "${pendingAccts.size} new accounts found", color = ZColor.Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(AccountBook.displayName(c.key) + " · " + plural(c.count, "payment", "payments"), color = ZColor.InkSoft, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text("Review", color = ZColor.Accent, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp))
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
            // Only the new page is drawn, fading in. A Column (not a Box) so the page's parts stack top to bottom.
            Column(Modifier.fillMaxWidth().graphicsLayer { alpha = pageAlpha.value }) {
            val pageView = view
            when (pageView) {
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
                        tabs = {
                            Spacer(Modifier.height(12.dp))
                            ChipScroll {
                                listOf("Overview", "Categories", "Patterns").forEachIndexed { i, t ->
                                    Chip(t, spendTab == i, filled = true) { spendTab = i }
                                }
                            }
                        },
                        showSummary = spendTab == 0,
                        showCategories = spendTab == 1,
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
                    if (spendTab == 2 && dayStr == null) {
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
                    if (spendTab == 2) PaceView(rows, shown, f, month, account, excludeEmi)
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
                    MenuRow("Notifications", "Payment alerts, daily summary, bill reminders", Icons.Rounded.Notifications, ZColor.Accent) { menuPage = "notify" }
                    MenuRow("Backup and security", "Back up, restore, app lock", Icons.Rounded.Lock, ZColor.Income) { menuPage = "backup" }
                    MenuRow("Export CSV", "Save to Downloads/Zelu", Icons.Rounded.FileDownload, ZColor.Muted) {
                        shut()
                        val ok = Exporter.exportCsv(context, shown, "zelu-${Dates.today()}.csv")
                        Toast.makeText(context, if (ok) "Saved to Downloads/Zelu" else "Export needs Android 10+", Toast.LENGTH_SHORT).show()
                    }
                    run {
                        val startNow = context.getSharedPreferences("zelu_ui", android.content.Context.MODE_PRIVATE).getString("start", "ledger") ?: "ledger"
                        var startTick by remember { mutableStateOf(0) }
                        val names = listOf("ledger" to "Home", "spend" to "Spend", "payees" to "Payees", "dues" to "Dues")
                        MenuRow("Start screen", (names.firstOrNull { it.first == startNow }?.second ?: "Home") + " · tap to change", Icons.Rounded.CalendarMonth, ZColor.Muted) {
                            val next = names[(names.indexOfFirst { it.first == startNow }.coerceAtLeast(0) + 1) % names.size].first
                            context.getSharedPreferences("zelu_ui", android.content.Context.MODE_PRIVATE).edit().putString("start", next).apply()
                            startTick++
                        }
                        if (startTick < 0) Text("")
                    }
                    MenuRow("Help", "How Zelu works, privacy policy, report a problem", Icons.Rounded.Lightbulb, ZColor.Accent) { menuPage = "help" }
                } else {
                    Row(Modifier.fillMaxWidth().clickable { menuPage = "" }.padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("‹", color = ZColor.Ink, fontSize = 26.sp)
                        Spacer(Modifier.width(12.dp))
                        Text(when (menuPage) { "money" -> "Money"; "accounts" -> "Accounts"; "messages" -> "Messages"; "backup" -> "Backup and security"; "notify" -> "Notifications"; else -> "Help" },
                            color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    when (menuPage) {
                        "notify" -> {
                            var nv by remember { mutableStateOf(0) }
                            val seen = nv
                            fun onOff(on: Boolean) = @Composable { Text(if (on) "ON" else "OFF", color = if (on) ZColor.Income else ZColor.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                            val spendOn = NotifyPrefs.spend(context)
                            MenuRow("Payment alerts", "A note for every payment or balance message", Icons.Rounded.Notifications, if (spendOn) ZColor.Income else ZColor.Muted, trailing = onOff(spendOn)) {
                                NotifyPrefs.setSpend(context, !spendOn); nv++
                            }
                            val dailyOn = NotifyPrefs.dailyOn(context)
                            MenuRow("Daily summary", "What you spent today", Icons.Rounded.BarChart, if (dailyOn) ZColor.Income else ZColor.Muted, trailing = onOff(dailyOn)) {
                                NotifyPrefs.setDailyOn(context, !dailyOn); nv++
                            }
                            if (dailyOn) MenuRow("Daily summary time", NotifyPrefs.timeText(NotifyPrefs.dailyAt(context)), Icons.Rounded.CalendarMonth, ZColor.Muted) {
                                val at = NotifyPrefs.dailyAt(context)
                                android.app.TimePickerDialog(context, { _, h, m -> NotifyPrefs.setDailyAt(context, h * 60 + m); nv++ }, at / 60, at % 60, false).show()
                            }
                            val weeklyOn = NotifyPrefs.weeklyOn(context)
                            MenuRow("Weekly summary", "Sundays: the week against the one before", Icons.Rounded.BarChart, if (weeklyOn) ZColor.Income else ZColor.Muted, trailing = onOff(weeklyOn)) {
                                NotifyPrefs.setWeeklyOn(context, !weeklyOn); nv++
                            }
                            if (weeklyOn) MenuRow("Weekly summary time", "Sundays at " + NotifyPrefs.timeText(NotifyPrefs.weeklyAt(context)), Icons.Rounded.CalendarMonth, ZColor.Muted) {
                                val at = NotifyPrefs.weeklyAt(context)
                                android.app.TimePickerDialog(context, { _, h, m -> NotifyPrefs.setWeeklyAt(context, h * 60 + m); nv++ }, at / 60, at % 60, false).show()
                            }
                            val duesOn = NotifyPrefs.duesOn(context)
                            MenuRow("Bill reminders", "Before a bill or card is due", Icons.Rounded.Receipt, if (duesOn) ZColor.Income else ZColor.Muted, trailing = onOff(duesOn)) {
                                NotifyPrefs.setDuesOn(context, !duesOn); nv++
                            }
                            if (duesOn) MenuRow("Remind me", NotifyPrefs.LEAD_PRESETS[NotifyPrefs.leadIndex(context)].first + " · tap to change", Icons.Rounded.CalendarMonth, ZColor.Muted) {
                                NotifyPrefs.setLeadIndex(context, (NotifyPrefs.leadIndex(context) + 1) % NotifyPrefs.LEAD_PRESETS.size); nv++
                            }
                            val recapOn = NotifyPrefs.recapOn(context)
                            MenuRow("Monthly recap", "A note in the first days of a new month", Icons.Rounded.TrendingUp, if (recapOn) ZColor.Income else ZColor.Muted, trailing = onOff(recapOn)) {
                                NotifyPrefs.setRecapOn(context, !recapOn); nv++
                            }
                            if (seen >= 0) MutedText("Bill reminders go out around 9 AM.", 11)
                        }
                        "money" -> {
                            MenuRow("Budget", if (monthlyBudget > 0) money(monthlyBudget) + " a month" else "Not set", Icons.Rounded.AccountBalanceWallet) { shut(); budgetDlg = true }
                            MenuRow("Categories", "Spending and income categories", Icons.Rounded.Category, ZColor.Accent) { shut(); catsOpen = true }
                            MenuRow("Savings plan", "What's left · SIP, EMI, goals", Icons.Rounded.Savings, ZColor.Income) { shut(); savingsOpen = true }
                            MenuRow("Investments", "EPF, mutual funds, SIPs", Icons.Rounded.TrendingUp, ZColor.Income) { shut(); view = "invest"; daySel = 0 }
                        }
                        "accounts" -> {
                            MenuRow("Your accounts", "Join duplicates, set what each one is, remove what is not yours", Icons.Rounded.AccountBalance) { shut(); stage = "accounts" }
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
                            var advOpen by remember { mutableStateOf(false) }
                            val lr = Storage.lastRead(context)
                            val nb = Storage.newestBank(context)
                            val stale = nb > 0 && System.currentTimeMillis() - nb > 7L * 86_400_000L
                            MenuRow("Rescan messages", if (lr == 0L) "Look for anything new in your inbox" else "Last read " + agoText(lr) + (if (nb > 0) " · newest bank SMS " + agoText(nb) else ""),
                                Icons.Rounded.Restore, if (stale) ZColor.Warn else ZColor.Accent) { shut(); rescan(notify = true) }
                            MenuRow("Missed SMS", if (missedCount > 0) "$missedCount waiting · add by hand or teach Zelu" else "Money messages Zelu couldn't read", Icons.Rounded.Receipt, ZColor.Warn) {
                                shut(); unreadOpen = true; unreadList = null
                                scope.launch { val l = withContext(Dispatchers.IO) { Ingest.unreadable(context) }; unreadList = l; noteMissed(l.size) }
                            }
                            MenuRow("Advanced", if (advOpen) "Hide" else "Other senders, date ranges, data check, start again", Icons.Rounded.Lightbulb, ZColor.Muted,
                                trailing = { Text(if (advOpen) "▲" else "▼", color = ZColor.Muted, fontSize = 12.sp) }) { advOpen = !advOpen }
                            if (advOpen) {
                            MenuRow("Unrecognised senders", "A bank or wallet Zelu skipped? Tell it here", Icons.Rounded.AccountBalance, ZColor.Muted) {
                                shut(); sendersOpen = true; senderList = null
                                scope.launch { senderList = withContext(Dispatchers.IO) { Ingest.unrecognisedSenders(context) } }
                            }
                            MenuRow("Rescan between dates", "Read only the messages of a period you pick", Icons.Rounded.CalendarMonth, ZColor.Muted) { shut(); rescanRangeOpen = true }
                            MenuRow("Read older messages", "Goes back through your whole inbox. Can take a minute.", Icons.Rounded.Restore, ZColor.Muted) { shut(); rescan(notify = true, depth = 100_000) }
                            MenuRow("Data check", "What was read, and whether balances add up", Icons.Rounded.Receipt) {
                                shut(); checkOpen = true; check = null
                                scope.launch { check = withContext(Dispatchers.IO) { Ingest.diagnose(context) } }
                            }
                            MenuRow("Read everything again", "Clears payments and reads all messages afresh. Your edits stay.", Icons.Rounded.Warning, ZColor.Muted) { shut(); fullConfirm = true }
                            }
                        }
                        "backup" -> {
                            val lb = Backup.lastAny(context)
                            MenuRow("Back up now", if (lb > 0) "Last saved " + java.text.SimpleDateFormat("d MMM", java.util.Locale.US).format(java.util.Date(lb)) + " · locked with your password" else "Save everything to Downloads/Zelu, locked with a password", Icons.Rounded.CloudUpload) {
                                shut()
                                if (Backup.hasPassword(context)) backupNow() else { backupAfterPw = true; backupPwOpen = true }
                            }
                            MenuRow("Backup password", if (Backup.hasPassword(context)) "Set · needed to restore on a new phone" else "Not set · backups wait until you set one", Icons.Rounded.Lock,
                                if (Backup.hasPassword(context)) ZColor.Income else ZColor.Warn) { shut(); backupAfterPw = false; backupPwOpen = true }
                            MenuRow("Restore data", "Load a backup file", Icons.Rounded.Restore, ZColor.Muted) { shut(); restoreLauncher.launch(arrayOf("*/*")) }
                            MenuRow("Weekly backup", if (Backup.hasPassword(context)) "Saved to Downloads/Zelu every week" else "Needs a backup password first", Icons.Rounded.CloudUpload, if (autoBackup) ZColor.Income else ZColor.Muted,
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
                            var helpAdv by remember { mutableStateOf(false) }
                            if (Demo.on) MenuRow("Sample data is on", "Made-up payments. Tap to go back to yours", Icons.Rounded.Visibility, ZColor.Warn) {
                                shut(); Demo.set(context, false)
                                Toast.makeText(context, "Back to your own payments", Toast.LENGTH_SHORT).show()
                            }
                            MenuRow("How Zelu works", "Reading messages, dues, accounts, backup", Icons.Rounded.Lightbulb) { shut(); helpOpen = true }
                            MenuRow("Report a problem", "Email " + Storage.reportEmail(context), Icons.Rounded.Warning, ZColor.Warn) { shut(); reportOpen = true }
                            MenuRow("Privacy policy", "What Zelu reads and keeps", Icons.Rounded.Lock, ZColor.Income) { shut(); privacyOpen = true }
                            MenuRow("Keep Zelu alive", "Stop battery saver blocking SMS", Icons.Rounded.BatteryChargingFull) { shut(); Notifier.openBatterySettings(context) }
                            MenuRow("Advanced", if (helpAdv) "Hide" else "Report address, test alert", Icons.Rounded.Lightbulb, ZColor.Muted,
                                trailing = { Text(if (helpAdv) "▲" else "▼", color = ZColor.Muted, fontSize = 12.sp) }) { helpAdv = !helpAdv }
                            if (helpAdv) {
                            MenuRow("Report address", Storage.reportEmail(context), Icons.Rounded.Palette, ZColor.Muted) { shut(); emailOpen = true }
                            if (!Demo.on) MenuRow("Show sample data", "Made-up payments for screenshots and demos. Yours stay safe", Icons.Rounded.Visibility, ZColor.Muted) {
                                shut(); Demo.set(context, true)
                                Toast.makeText(context, "Showing sample data. Menu, Help to go back", Toast.LENGTH_LONG).show()
                            }
                            MenuRow("Test alert", if (notifBlocked) "Alerts are off · tap to turn on" else "Send a sample payment alert",
                                Icons.Rounded.Notifications, if (notifBlocked) ZColor.Warn else ZColor.Accent) {
                                shut()
                                if (notifBlocked) fixNotif() else Notifier.notifyTest(context)
                            }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }

    LimitAsk.account?.let { acct ->
        var txt by remember(acct) { mutableStateOf("") }
        Dialog(onDismissRequest = { LimitAsk.account = null }) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp)) {
                Text("Card limit", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                MutedText("Your bank's messages for " + acct + " don't state the available limit. Enter what your bank app shows now. If a later message states it, Zelu uses that instead.", 12)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = txt, onValueChange = { v -> txt = v.filter { it.isDigit() || it == '.' } }, singleLine = true,
                    placeholder = { Text("Available limit ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    Text("Cancel", color = ZColor.Muted, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { LimitAsk.account = null }.padding(12.dp))
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink).clickable {
                        val v = txt.toDoubleOrNull() ?: 0.0
                        if (v > 0) scope.launch {
                            withContext(Dispatchers.IO) { Storage.setManualLimit(context, acct, v) }
                            limits = withContext(Dispatchers.IO) { Storage.loadLimits(context) }
                            LimitAsk.manual = withContext(Dispatchers.IO) { Storage.manualLimitKeys(context) }
                            LimitAsk.account = null
                        }
                    }.padding(horizontal = 22.dp, vertical = 10.dp)) { Text("Save", color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }

    if (sendersOpen) Dialog(onDismissRequest = { sendersOpen = false }) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp).heightIn(max = 620.dp)) {
            Text("Unrecognised senders", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            MutedText("Senders Zelu doesn't recognise, whose messages read like payments. Say what each is, or tap Tell Zelu to ask for it to be added for everyone (only the sender id is sent). Bank: read as an account. Wallet: its spends are not counted again, since the top-up already was. Other: read as payments with no account. Ignore: skip it. Only add senders you trust.", 12)
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
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                listOf("bank" to "Bank", "wallet" to "Wallet", "other" to "Other").forEach { (ty, lab) ->
                                    Text(lab, color = ZColor.Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
                                        scope.launch {
                                            withContext(Dispatchers.IO) { Storage.trustSender(context, u.address, ty) }
                                            senderList = senderList?.filter { it.address != u.address }
                                            rescan(notify = true)
                                        }
                                    })
                                }
                                Text("Ignore", color = ZColor.Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable {
                                    scope.launch(Dispatchers.IO) { Storage.markSmsHandled(context, "sender|" + u.address.uppercase()) }
                                    senderList = senderList?.filter { it.address != u.address }
                                })
                                // Opens an email draft with only the sender id, so the bank can be added for everyone. The user sends it.
                                Text("Tell Zelu", color = ZColor.Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable {
                                    val mail = android.content.Intent(android.content.Intent.ACTION_SENDTO, android.net.Uri.parse("mailto:"))
                                        .putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf(Storage.reportEmail(context)))
                                        .putExtra(android.content.Intent.EXTRA_SUBJECT, "Zelu: please add this bank")
                                        .putExtra(android.content.Intent.EXTRA_TEXT, "Sender id: " + u.address + "\nBank name: (please type it here)\n\nNo message text is included.")
                                    try { context.startActivity(android.content.Intent.createChooser(mail, "Tell Zelu")) }
                                    catch (_: Exception) { Toast.makeText(context, "No email app found on this phone", Toast.LENGTH_LONG).show() }
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
            var catTab by remember { mutableStateOf(0) }
            Text("Categories", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Spending", catTab == 0, filled = true) { catTab = 0 }
                Chip("Income", catTab == 1, filled = true) { catTab = 1 }
            }
            Spacer(Modifier.height(10.dp))
            if (catTab == 1) IncomeCategoriesBody() else {
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
                                Spacer(Modifier.height(3.dp))
                                Text(r.raw, color = ZColor.InkSoft, fontSize = 12.sp, maxLines = 6, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(8.dp))
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
        // Four fix-it tiles with numbered steps, small chips for everything else, and one answer shown at the top.
        class Fix(val key: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val title: String, val steps: List<String>)
        val fixes = listOf(
            Fix("pay", Icons.Rounded.Receipt, "A payment is wrong", listOf("Open the payment from Home or Spend.", "Tap Change category.",
                "Pick a category, or Transfer, Lent, Borrowed, Ignore.", "Zelu remembers it for the same payee.")),
            Fix("bank", Icons.Rounded.AccountBalance, "A bank is not read", listOf("Open Menu, then Messages.", "Tap Unrecognised senders.",
                "Say whether the sender is a bank, a wallet or other.", "Its old and new messages are read from then on.")),
            Fix("acct", Icons.Rounded.CreditCard, "An account is wrong", listOf("Open Menu, then Your accounts.", "Tap the account.",
                "Choose what it is, and tick any that are the same account.", "Tap Save. Untick later to separate them.")),
            Fix("data", Icons.Rounded.CloudUpload, "Back up or erase", listOf("Open Menu, then Backup and security.", "Back up saves one file to Downloads/Zelu, locked with a password you choose. Keep that password: Zelu cannot recover it.",
                "Restore loads it on a new phone.", "Erase everything clears Zelu from this phone.")),
        )
        val topics = listOf(
            "Private" to "Your messages and payments stay on this phone. Zelu has no ads and no internet access, so nothing can be sent anywhere.",
            "Rescan" to "New messages are read as they arrive. Menu, Messages, Rescan reads again right now.",
            "Missed SMS" to "Money messages Zelu could not read. Menu, Messages, Missed SMS lets you add one by hand.",
            "Messages stop" to "Android may switch off permissions for an app you open rarely. In Settings, Apps, Zelu, Permissions, turn off 'Remove permissions if app isn't used'.",
            "Duplicates" to "If one payment was reported twice, hide the extra one. Keep both if you really paid twice.",
            "Dues" to "Bills, EMIs and card dues. Tap the circle when paid. AutoPay has its own tab.",
            "Split a payment" to "Open the payment, tap Change category, then Split.",
            "Leave a category out" to "Menu, Money, Categories. It stops counting as spending.",
            "Not covered" to "Zelu reads SMS only. Chat apps, RCS bank chats and bank apps are not visible to it.",
        )
        var hq by remember { mutableStateOf("") }
        var hsel by remember { mutableStateOf("fix:pay") }
        val q = hq.trim().lowercase()
        val fx = fixes.filter { q.isEmpty() || (it.title + " " + it.steps.joinToString(" ")).lowercase().contains(q) }
        val tp = topics.filter { q.isEmpty() || (it.first + " " + it.second).lowercase().contains(q) }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp).heightIn(max = 640.dp)) {
            Text("Help", color = ZColor.Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = hq, onValueChange = { hq = it }, singleLine = true, placeholder = { Text("Search help") }, modifier = Modifier.fillMaxWidth())
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                Spacer(Modifier.height(12.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(ZColor.Paper).padding(14.dp)) {
                    val f = fixes.firstOrNull { "fix:" + it.key == hsel }
                    val t = topics.firstOrNull { "topic:" + it.first == hsel }
                    if (f != null) {
                        Eyebrow(f.title)
                        f.steps.forEachIndexed { i, st ->
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.Top) {
                                Box(Modifier.size(22.dp).clip(CircleShape).background(ZColor.Accent), contentAlignment = Alignment.Center) {
                                    Text((i + 1).toString(), color = ZColor.AccentInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(st, color = ZColor.InkSoft, fontSize = 14.sp, lineHeight = 20.sp)
                            }
                        }
                    } else if (t != null) {
                        Text(t.first, color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(t.second, color = ZColor.InkSoft, fontSize = 14.sp, lineHeight = 20.sp)
                    } else MutedText("Pick a problem or a topic. The answer shows here.", 13)
                }
                if (fx.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp)); Eyebrow("Fix something"); Spacer(Modifier.height(8.dp))
                    fx.chunked(2).forEach { pair ->
                        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            pair.forEach { f ->
                                val on = hsel == "fix:" + f.key
                                Column(Modifier.weight(1f).heightIn(min = 92.dp).clip(RoundedCornerShape(16.dp)).background(if (on) ZColor.Paper else ZColor.Card)
                                    .border(1.dp, if (on) ZColor.Accent else ZColor.Line, RoundedCornerShape(16.dp))
                                    .clickable { hsel = "fix:" + f.key }.padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                    Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(ZColor.Paper), contentAlignment = Alignment.Center) {
                                        androidx.compose.material3.Icon(f.icon, null, tint = ZColor.Accent, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    Text(f.title, color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                if (tp.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp)); Eyebrow("Good to know"); Spacer(Modifier.height(8.dp))
                    tp.chunked(2).forEach { pair ->
                        Row(Modifier.padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            pair.forEach { (k, _) -> Chip(k, hsel == "topic:" + k) { hsel = "topic:" + k } }
                        }
                    }
                }
                if (fx.isEmpty() && tp.isEmpty()) MutedText("Nothing matches that. Try another word, or report it.", 13)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Still stuck? Report a problem", color = ZColor.Accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { helpOpen = false; reportOpen = true }.padding(vertical = 8.dp))
                Box(Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink).clickable { helpOpen = false }.padding(horizontal = 22.dp, vertical = 10.dp)) {
                    Text("Close", color = ZColor.Paper, fontWeight = FontWeight.SemiBold)
                }
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

    if (privacyOpen) Dialog(onDismissRequest = { privacyOpen = false }) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp).heightIn(max = 620.dp)) {
            Text("Privacy policy", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            MutedText("Last updated " + PrivacyText.UPDATED, 12)
            Spacer(Modifier.height(10.dp))
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                Text(PrivacyText.SUMMARY, color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrivacyText.facts.forEach { (t, d) ->
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(ZColor.IncomeSoft).padding(10.dp)) {
                            Text("✓", color = ZColor.Income, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(t, color = ZColor.Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(d, color = ZColor.InkSoft, fontSize = 11.sp)
                        }
                    }
                }
                PrivacyText.sections.forEach { sec ->
                    Spacer(Modifier.height(12.dp))
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ZColor.Paper).padding(14.dp)) {
                        Text(sec.title, color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        sec.points.forEach { pt ->
                            Row(Modifier.padding(top = 6.dp)) {
                                Text("•", color = ZColor.Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(8.dp))
                                Text(pt, color = ZColor.InkSoft, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(Modifier.align(Alignment.End).clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                .clickable { privacyOpen = false }.padding(horizontal = 22.dp, vertical = 10.dp)) {
                Text("Close", color = ZColor.Paper, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    // After a crash, offer once to send the details. The email opens as a draft; nothing goes out unless the user sends it.
    if (crashAsk) {
        fun seen() { crashAsk = false; scope.launch(Dispatchers.IO) { CrashLog.markSeen(context) } }
        Dialog(onDismissRequest = { seen() }) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp)) {
                Text("Zelu closed unexpectedly", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                MutedText("Sorry about that. Sending the error details helps fix it. They hold only technical text, no payments or messages, and you see the email before sending.", 13)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.align(Alignment.End)) {
                    Box(Modifier.clip(RoundedCornerShape(20.dp)).clickable { seen() }.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Text("Not now", color = ZColor.Muted, fontWeight = FontWeight.SemiBold)
                    }
                    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                        .clickable { seen(); reportOpen = true }.padding(horizontal = 18.dp, vertical = 10.dp)) {
                        Text("Send report", color = ZColor.Paper, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    if (reportOpen) {
        var what by remember { mutableStateOf("") }
        var withInfo by remember { mutableStateOf(true) }
        var withMissed by remember { mutableStateOf(false) }
        val crash = remember { CrashLog.last(context) }
        var withCrash by remember { mutableStateOf(crash != null) }
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
                tick(withMissed, "Messages Zelu couldn't read", "Up to 3 message texts. Long numbers and UPI names are hidden; amounts stay.") { withMissed = !withMissed }
                if (crash != null) tick(withCrash, "Details of the last crash", "Technical error text only. No payments.") { withCrash = !withCrash }
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
                                miss.forEach { sb.append("\n[").append(it.address).append("] ").append(Redact.message(it.body)).append("\n") }
                            }
                            if (withCrash && crash != null) sb.append("\n--\nLast crash:\n").append(crash).append("\n")
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
                                    Storage.addManualTxn(context, Row(id, day, amount, dir, name, null, sms.body, sms.address, acct, c.kind, c.category, sms.date), Ingest.smsKey(sms))
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

    if (backupPwOpen) {
        var pw by remember { mutableStateOf("") }
        var pw2 by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { backupPwOpen = false }) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp)) {
                Text(if (Backup.hasPassword(context)) "Change backup password" else "Set a backup password", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                MutedText("Backups are locked with this password, so nobody who finds the file can read your payments. Zelu cannot recover it: if you forget it, old backups cannot be opened.", 12)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = pw, onValueChange = { pw = it }, singleLine = true, label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = pw2, onValueChange = { pw2 = it }, singleLine = true, label = { Text("Type it again") },
                    visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    Text("Cancel", color = ZColor.Muted, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { backupPwOpen = false }.padding(12.dp))
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink).clickable {
                        when {
                            pw.length < BackupCrypto.MIN_PASSWORD -> Toast.makeText(context, "Use at least ${BackupCrypto.MIN_PASSWORD} characters", Toast.LENGTH_SHORT).show()
                            pw != pw2 -> Toast.makeText(context, "The two passwords don't match", Toast.LENGTH_SHORT).show()
                            else -> {
                                Backup.setPassword(context, pw)
                                backupPwOpen = false
                                if (backupAfterPw) { backupAfterPw = false; backupNow() }
                                else Toast.makeText(context, "Backup password saved", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }.padding(horizontal = 22.dp, vertical = 10.dp)) { Text("Save", color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }

    restoreUri?.let { uri ->
        var needPw by remember(uri) { mutableStateOf(false) }
        var pw by remember(uri) { mutableStateOf("") }
        var wrong by remember(uri) { mutableStateOf(false) }
        Dialog(onDismissRequest = { restoreUri = null }) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp)) {
                Text("Restore this backup?", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                MutedText("It replaces your current transactions, categories, budgets and settings with the ones in the file.", 13)
                if (needPw) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(value = pw, onValueChange = { pw = it; wrong = false }, singleLine = true, label = { Text("Backup password") },
                        isError = wrong, supportingText = { if (wrong) Text("That password doesn't open this backup") },
                        visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    Text("Cancel", color = ZColor.Muted, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { restoreUri = null }.padding(12.dp))
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink).clickable {
                        scope.launch {
                            val r = withContext(Dispatchers.IO) {
                                try {
                                    val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                                    if (text == null) Backup.Result.NOT_A_BACKUP else Backup.restoreFrom(context, text, pw)
                                } catch (_: Exception) { Backup.Result.NOT_A_BACKUP }
                            }
                            when (r) {
                                Backup.Result.NEEDS_PASSWORD -> needPw = true
                                Backup.Result.WRONG_PASSWORD -> { needPw = true; wrong = true }
                                else -> {
                                    restoreUri = null
                                    val ok = r == Backup.Result.OK
                                    if (ok) {
                                        val sp = context.getSharedPreferences("zelu_theme", android.content.Context.MODE_PRIVATE)
                                        ZTheme.palette = sp.getInt("palette", 0).coerceIn(0, PALETTES.size - 1)
                                        if (sp.contains("dark")) ZTheme.dark = sp.getBoolean("dark", false)
                                        reload()
                                    }
                                    Toast.makeText(context, if (ok) "Backup restored" else "That isn't a Zelu backup file", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }.padding(horizontal = 22.dp, vertical = 10.dp)) { Text("Restore", color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }

    if (rescanRangeOpen) RescanRangeDialog(onRun = { a, b -> rescanRangeOpen = false; rescanRange(a, b) }, onDismiss = { rescanRangeOpen = false })


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
