package com.koolicream.zelu

import androidx.compose.ui.res.stringResource

import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.DonutLarge
import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Add
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : FragmentActivity() {
    // App lock: asks for fingerprint, face or the phone's own PIN / pattern when opening, and again after the app
    // has been away for over 30 seconds. Off by default; switched on from the menu.
    private var locked by mutableStateOf(false)
    private var authShowing = false
    private var stoppedAt = 0L

    private fun securityPrefs() = getSharedPreferences("zelu_security", MODE_PRIVATE)
    private fun lockEnabled() = securityPrefs().getBoolean("lock", false)

    // With the lock on, the Recents screen shows a blank card instead of your balances. Android 13+ can blank just
    // that card; older phones need FLAG_SECURE, which also blocks screenshots.
    private val lockPrefListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "lock") applyRecentsPrivacy()
    }

    private fun applyRecentsPrivacy() {
        val hide = lockEnabled()
        if (Build.VERSION.SDK_INT >= 33) setRecentsScreenshotEnabled(!hide)
        else if (hide) window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        else window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
    }

    // Made once in onCreate, as the library asks. Android 9+ draws the prompt itself; older phones get the system's
    // PIN / pattern screen, so the library's own fingerprint dialog (which wants an AppCompat theme) is never used.
    private lateinit var prompt: BiometricPrompt
    private lateinit var credentialLauncher: androidx.activity.result.ActivityResultLauncher<android.content.Intent>

    private fun unlocked() { authShowing = false; locked = false; stoppedAt = 0L }

    private fun askUnlock() {
        if (authShowing) return
        val km = getSystemService(KEYGUARD_SERVICE) as android.app.KeyguardManager
        // No screen lock on the phone: there is nothing to check against, so the app opens (as before).
        if (!km.isDeviceSecure) { locked = false; return }
        if (Build.VERSION.SDK_INT < 28) {
            @Suppress("DEPRECATION")
            val i = km.createConfirmDeviceCredentialIntent("Unlock Zelu", "Confirm it's you")
            if (i == null) { locked = false; return }
            // A prompt that could not be shown leaves the app locked; the lock screen's Unlock button asks again.
            try { authShowing = true; credentialLauncher.launch(i) } catch (e: Exception) { authShowing = false; CrashLog.warn("App lock", e) }
            return
        }
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Zelu")
            .setSubtitle("Confirm it's you")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()
        try { authShowing = true; prompt.authenticate(info) } catch (e: Exception) { authShowing = false; CrashLog.warn("App lock", e) }
    }

    override fun onStart() {
        super.onStart()
        if (lockEnabled() && !locked && stoppedAt != 0L &&
            android.os.SystemClock.elapsedRealtime() - stoppedAt > 30_000L) locked = true
        if (locked) askUnlock()
    }

    override fun onStop() {
        super.onStop()
        // On older phones the PIN screen is a separate activity: leaving for it is not leaving the app.
        if (!authShowing) stoppedAt = android.os.SystemClock.elapsedRealtime()
        val app = applicationContext
        lifecycleScope.launch(Dispatchers.IO) {
            try { ZeluWidget.refresh(app) } catch (e: Exception) { CrashLog.warn("Widget", e) }
        }
    }

    override fun onDestroy() {
        securityPrefs().unregisterOnSharedPreferenceChangeListener(lockPrefListener)
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = unlocked()
            // Cancelled or failed for good: stay on the lock screen, whose Unlock button asks again.
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { authShowing = false }
        })
        credentialLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            if (res.resultCode == RESULT_OK) unlocked() else authShowing = false
        }
        locked = lockEnabled()
        applyRecentsPrivacy()
        securityPrefs().registerOnSharedPreferenceChangeListener(lockPrefListener)
        val sp = getSharedPreferences("zelu_theme", MODE_PRIVATE)
        val sysDark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        ZTheme.dark = if (sp.contains("dark")) sp.getBoolean("dark", false) else sysDark
        ZTheme.palette = paletteIndex(sp)
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
            // Android 13+: the next launch screen follows Zelu's own dark/light switch, not the phone's.
            androidx.compose.runtime.LaunchedEffect(ZTheme.dark) {
                if (android.os.Build.VERSION.SDK_INT >= 33) splashScreen.setSplashScreenTheme(
                    if (ZTheme.dark) R.style.Theme_Zelu_SplashDark else R.style.Theme_Zelu_SplashLight)
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
    // One AppState for your data and one for sample data. It outlives the screen (a ViewModel), so where you are and what
    // was read survive the screen being rebuilt; its saved state survives Android closing the app in the background.
    val s: AppState = androidx.lifecycle.viewmodel.compose.viewModel(key = if (Demo.on) "zelu-demo" else "zelu")
    // The launchers belong to this screen: the AppState lets go of them when it goes.
    DisposableEffect(s) { onDispose { s.restoreLauncher = null; s.notifLauncher = null; s.storageLauncher = null } }
    with(s) {
        LaunchedEffect(Unit) { crashAsk = withContext(Dispatchers.IO) { CrashLog.isNew(context) } }
        val links = androidx.compose.runtime.remember(registry) {
            registry.filter { it.value.first == "duplicate" }.entries.groupBy({ it.value.second }, { it.key })
        }
        restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) restoreUri = uri }
        val permLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { res ->
            val smsOk = res[Manifest.permission.READ_SMS] == true && res[Manifest.permission.RECEIVE_SMS] == true
            onbSms = smsOk
            onbNotif = Notifier.enabled(context)
            // Show the ticks for a moment before moving on.
            if (smsOk) scope.launch { delay(900); stage = Stage.SCAN } else { permDenied = true }
        }

        storageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
            val save = afterStorage
            afterStorage = null
            if (ok) save?.invoke()
            else android.widget.Toast.makeText(context, context.getString(R.string.allow_storage_access_to_save), android.widget.Toast.LENGTH_LONG).show()
        }
        notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
            checkNotif()
            if (!ok) Notifier.openSettings(context)
        }
        val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
                if (e == androidx.lifecycle.Lifecycle.Event.ON_START) inForeground = true
                if (e == androidx.lifecycle.Lifecycle.Event.ON_STOP) inForeground = false
                if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                    checkNotif()
                    smsOk = hasPerm()
                    // Catch up on anything the phone held back while Zelu was closed.
                    if (stage == Stage.APP && hasPerm() && System.currentTimeMillis() - lastScan > 60_000L) rescan(quick = true)
                }
            }
            lifecycleOwner.lifecycle.addObserver(obs)
            onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
        }

        // Best-effort local reminder on open: nudge about bills due within 3 days.
        LaunchedEffect(stage, reminders) {
            if (stage != Stage.APP) return@LaunchedEffect
            // Daily 9 AM check runs in the background (DueReceiver); this just makes sure it's scheduled.
            DueReminders.schedule(context)
            DailySummary.schedule(context)
            WeeklySummary.schedule(context)
        }

        LaunchedEffect(Unit) {
            // Everything saved, balances and card limits included, shows at once; the inbox check below only adds
            // what is new.
            loadAll()
            // Coming back after Android closed the app in the background (or the theme changed): stay on the screen you were on.
            if (stage == Stage.APP || stage == Stage.ACCOUNTS) {
                smsOk = hasPerm()
                if (smsOk && System.currentTimeMillis() - lastScan > 60_000L) rescan(quick = true)
                return@LaunchedEffect
            }
            stage = Stage.SPLASH
            delay(1600)                                      // the start animation takes about 1.5 s
            if (hasPerm()) { stage = Stage.APP; rescan() } else if (rows.isNotEmpty()) { stage = Stage.APP; smsOk = false } else { stage = Stage.WELCOME }
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
        val v = Filtered(accounts, bankAccounts, f, list, shown)

        // First time (or first time after this update): once messages are read, you confirm which accounts are yours.
        LaunchedEffect(stage, rows.size, acctConfirmed) {
            if (stage == Stage.APP && !acctConfirmed && rows.isNotEmpty()) stage = Stage.ACCOUNTS
            // The old "All set" page is gone: whoever was on it carries on to their accounts, or Home.
            if (stage == Stage.DONE) stage = if (acctConfirmed) Stage.APP else Stage.ACCOUNTS
        }
        // The first read starts when Setting up opens, and starts again if Android closed the app while it ran.
        LaunchedEffect(stage) { if (stage == Stage.SCAN && !loading && hasPerm()) rescan() }
        // Working out the accounts reads every payment, so it is done off the main thread.
        val accountBook by androidx.compose.runtime.produceState<List<AccountBook.Acct>?>(null, rows, balances, limits, registry) {
            value = withContext(Dispatchers.IO) { Storage.accountBook(context) }
        }
        androidx.compose.runtime.CompositionLocalProvider(LocalAccountInfo provides
            AccountInfo(cardNetworks, links, manualLimits, { limitAskFor = it }, { refreshCardNetworks() })) {
        when {
            stage == Stage.ACCOUNTS -> {
                val book = accountBook
                if (book == null) Box(Modifier.fillMaxSize().background(ZColor.Paper))
                else {
                    undo?.let { u -> LaunchedEffect(u) { delay(6000); if (undo === u) undo = null } }
                    AccountsScreen(book, firstRun = !acctConfirmed, undoMsg = undo?.first, onUndo = { undoLast() },
                        onSave = { plan, msg -> saveAccountPlan(plan, msg) }, onDecline = { k, t -> declineAcct(k, t) },
                        onRestore = { k -> restoreAcct(k) }, onDone = { confirmAccounts() }, rows = rows)
                }
            }
            stage != Stage.APP -> {
                OnboardFlow(stage, permDenied, scanDone, scanTotal,
                    smsGiven = onbSms, notifGiven = onbNotif,
                    onBack = { stage = if (stage == Stage.PERM) Stage.PRIVACY else Stage.WELCOME },
                    onNext = { stage = when (stage) { Stage.WELCOME -> Stage.PRIVACY; Stage.PRIVACY -> Stage.PERM; else -> stage } },
                    onAllow = {
                        val perms = mutableListOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
                        if (Build.VERSION.SDK_INT >= 33) perms.add("android.permission.POST_NOTIFICATIONS")
                        permLauncher.launch(perms.toTypedArray())
                    })
            }
            else -> AppScreen(s, v, accountBook)
        }
        }
    }
}

// The app once set up: the home pages, and every dialog that can open over them.
@Composable
private fun AppScreen(s: AppState, v: Filtered, accountBook: List<AccountBook.Acct>?) {
    with(s) {
        HomeScreen(s, v, accountBook)
        MonthlyBudgetDialog(s)
        MenuDialog(s, v)
        CardLimitDialog(s)
        SendersDialog(s)
        CategoriesDialog(s)
        SplitDialog(s)
        EraseDialog(s)
        ReadAgainDialog(s)
        HelpDialog(s)
        ReportAddressDialog(s)
        PrivacyDialog(s)
        CrashDialog(s)
        ReportDialog(s)
        DataCheckDialog(s)
        MissedDialog(s)
        AddPaymentDialog(s)
        AddTransactionDialog(s)
        BackupPasswordDialog(s)
        RestoreDialog(s)
        if (rescanRangeOpen) RescanRangeDialog(onRun = { a, b -> rescanRangeOpen = false; rescanRange(a, b) }, onDismiss = { rescanRangeOpen = false })
        ThemeDialog(s)
        PaymentDialogs(s, v)
    }
}

@Composable
internal fun HomeScreen(s: AppState, v: Filtered, accountBook: List<AccountBook.Acct>?) {
    with(s) { with(v) {
        // Pickers open a window on the screen, so they take the screen's context, not the AppState's.
        val screen = androidx.compose.ui.platform.LocalContext.current
        val pageScroll = rememberScrollState()
        val pageAlpha = remember { Animatable(1f) }
        // The payments marked on Home's Recent strip: nothing sorted them, a refund may belong to a purchase, or a message came twice.
        val attention = remember(rows, refundSuggestions, dupOk) { Analytics.attention(rows, refundSuggestions, Analytics.possibleDuplicates(rows, dupOk)) }
        LaunchedEffect(view) {                              // each tab opens at its top and fades in
            pageScroll.scrollTo(0)
            pageAlpha.snapTo(0.35f); pageAlpha.animateTo(1f, tween(180))
        }
        Box(Modifier.fillMaxSize().background(ZColor.Paper)) {
            // Page colour behind the status bar, drawn on top, so scrolled content never runs under the clock.
            Box(Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars).background(ZColor.Paper)
                .align(Alignment.TopCenter).zIndex(1f))
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
                        TopIcon(stringResource(R.string.filters), onClick = { controlsOpen = true }, dot = advActiveTop, dotLabel = "some are on") { FunnelIcon() }
                        TopIcon(stringResource(R.string.menu), onClick = { menuOpen = true }, dot = missedCount > missedSeen, dotLabel = "new messages to check") { MenuIcon() }
                    }
                }
                Spacer(Modifier.height(14.dp))

                // A new account showed up in your messages after you set things up.
                val pendingAccts = remember(accountBook, registry, acctConfirmed) {
                    if (!acctConfirmed) emptyList()
                    else accountBook.orEmpty().filter { !it.declined && it.key !in registry && it.count > 0 && (it.count >= 2 || it.balance != null || it.limit != null || it.type == "loan") }
                }
                pendingAccts.firstOrNull()?.let { c ->
                    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(16.dp)).background(ZColor.Card).border(1.dp, ZColor.Accent, RoundedCornerShape(16.dp))
                        .clickable { stage = Stage.ACCOUNTS }.padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(if (pendingAccts.size == 1) "New account found" else "${pendingAccts.size} new accounts found", color = ZColor.Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(AccountBook.displayName(c.key) + " · " + plural(c.count, "payment", "payments"), color = ZColor.InkSoft, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(stringResource(R.string.review), color = ZColor.Accent, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp))
                    }
                }
                if (!smsOk) {
                    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(16.dp)).background(ZColor.Card).border(1.dp, ZColor.Warn, RoundedCornerShape(16.dp))
                        .clickable {
                            try {
                                context.startScreen(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    android.net.Uri.fromParts("package", context.packageName, null)).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                            } catch (e: Exception) { CrashLog.warn("MainActivity.HomeScreen", e) }
                        }.padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.zelu_can_t_read_your), color = ZColor.Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.android_may_have_switched_off),
                                color = ZColor.InkSoft, fontSize = 12.sp)
                        }
                        Text(stringResource(R.string.fix), color = ZColor.Accent, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp))
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
                // Dues always shows the next 30 days, so the month picker would only confuse there.
                if (view != Tab.DUES) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    if (period == "month") NavButton("‹", "Previous month") { month = Dates.shiftMonth(month, -1); daySel = 0 }
                    Row(
                        Modifier.weight(1f).clip(RoundedCornerShape(22.dp))
                            .clickable { rangeOpen = true }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.Icon(Icons.Rounded.CalendarMonth, null,
                            tint = Color(if (ZTheme.dark) 0xFF3DD171 else 0xFF22B455), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(summary, color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.width(8.dp))
                        Text("▾", color = ZColor.Muted, fontSize = 12.sp)
                    }
                    if (period == "month") NavButton("›", "Next month") { month = Dates.shiftMonth(month, 1); daySel = 0 }
                }
                if (rangeOpen) Dialog(onDismissRequest = { rangeOpen = false }) {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(vertical = 10.dp)) {
                        Text(stringResource(R.string.date_range), color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp))
                        RANGE_OPTS.forEach { (k, label) ->
                            Row(
                                Modifier.fillMaxWidth().choice(k == period) { period = k; daySel = 0; if (k != "custom") rangeOpen = false }
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(label, color = ZColor.Ink, fontSize = 15.sp,
                                    fontWeight = if (k == period) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
                                if (k == period) Text("✓", color = ZColor.Accent, fontWeight = FontWeight.Bold, modifier = Unspoken)
                            }
                        }
                        if (period == "custom") {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                DateChip(customFrom) { pickDate(screen, customFrom) { customFrom = it } }
                                DateChip(customTo) { pickDate(screen, customTo) { customTo = it } }
                            }
                            Box(
                                Modifier.align(Alignment.End).padding(end = 20.dp, top = 4.dp).clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                                    .clickable { rangeOpen = false }.padding(horizontal = 22.dp, vertical = 10.dp),
                            ) { Text(stringResource(R.string.done), color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
                        }
                    }
                }
                if (controlsOpen) Dialog(onDismissRequest = { controlsOpen = false }) {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(18.dp)
                        .heightIn(max = 640.dp).verticalScroll(rememberScrollState())) {
                        Text(stringResource(R.string.filters), color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = query, onValueChange = { query = it }, singleLine = true,
                            placeholder = { Text(stringResource(R.string.search_payee_or_amount)) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(12.dp))
                        MutedText(stringResource(R.string.account), 12)
                        Spacer(Modifier.height(6.dp))
                        AccountChips(bankAccounts, account) { account = it; daySel = 0 }
                        Spacer(Modifier.height(12.dp))
                        MutedText(stringResource(R.string.show), 12)
                        Spacer(Modifier.height(6.dp))
                        ChipScroll {
                            listOf("all" to "All", "expense" to "Spent", "income" to "Received", "ignore" to "Ignored").forEach { (k, l) ->
                                val ic = when (k) {
                                    "expense" -> Icons.Rounded.ArrowUpward to ZColor.Accent
                                    "income" -> Icons.Rounded.ArrowDownward to ZColor.Income
                                    "ignore" -> Icons.Rounded.VisibilityOff to ZColor.Muted
                                    else -> null
                                }
                                // "All" is drawn like the Category row's "All" (icon on a solid tile): an ink icon on an
                                // ink wash vanished on the chosen (ink) chip.
                                PickChip(l, type == k, { if (ic == null) CatDot("All", 20.dp) else IconDot(ic.first, ic.second, 20.dp) }) { type = k; daySel = 0 }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        MutedText(stringResource(R.string.category), 12)
                        Spacer(Modifier.height(6.dp))
                        ChipScroll {
                            CAT_OPTS.forEach { (k, l) ->
                                PickChip(if (k == "All") "All" else l, cat == k, { CatDot(k, 20.dp) }) { cat = k; daySel = 0 }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.clear_all), color = ZColor.Muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable {
                                    query = ""; account = "All"; type = "all"; cat = "All"; daySel = 0
                                }.padding(horizontal = 14.dp, vertical = 10.dp))
                            Spacer(Modifier.width(8.dp))
                            Box(
                                Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                                    .clickable { controlsOpen = false }.padding(horizontal = 22.dp, vertical = 10.dp),
                            ) { Text(stringResource(R.string.done), color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
                        }
                    }
                }

                if (loading) {
                    Spacer(Modifier.height(10.dp)); MutedText(stringResource(R.string.checking_for_new_payments))
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
                        Text(stringResource(R.string.alerts_are_off), color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.allow_notifications_to_get_instant), color = ZColor.InkSoft, fontSize = 12.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.turn_on), color = ZColor.Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                // Only the new page is drawn, fading in. A Column (not a Box) so the page's parts stack top to bottom.
                Column(Modifier.fillMaxWidth().graphicsLayer { alpha = pageAlpha.value }) {
                val pageView = view
                when (pageView) {
                    Tab.LEDGER -> LedgerView(rows, shown, f, accounts, month, account, daySel,
                        { daySel = if (daySel == it) 0 else it },
                        { detailRow = it }, { title, r -> drill = title to r }, excludeEmi,
                        repaid, { lendPerson = it }, { borrowPerson = it }, fixed, { budgetsOpen = true }, balances, limits, reminders, { open(Page.Card(it)) }, { excludeEmi = !excludeEmi }, { budgetDlg = true }, { view = Tab.DUES }, reminderState, attention, budgets = budgets,
                        onSetBudget = { amt, noEmi ->
                            scope.launch { withContext(Dispatchers.IO) { Storage.saveMonthlyBudget(context, amt) }; monthlyBudget = amt }
                            if (noEmi) excludeEmi = true
                        },
                        onOpenSpend = { view = Tab.SPEND }, monthlyBudget = monthlyBudget, onAdd = { addNew = true })
                    Tab.SPEND -> {
                        // Last month, compared fairly: only up to today's date while this month is running, and without EMI
                        // when this month's view leaves it out. (Comparing part of this month with all of last month showed
                        // "▼10%" right above a same-days "▲112%".)
                        val prevByCat = if (period == "month") {
                            val cutDay = if (month == Dates.todayMonth()) Dates.dayOfMonth() else 99
                            Analytics.categoryTotals(rows.filter { (it.date.drop(8).toIntOrNull() ?: 99) <= cutDay }, Dates.shiftMonth(month, -1), account)
                                .let { m -> if (excludeEmi && cat != "EMI") m - "EMI" else m }
                        } else emptyMap()
                        val isMonth = f.period == "month"
                        // The weekday pattern is about everyday spending: EMI and rent land on one fixed day and would
                        // make that weekday look the heaviest, so they are always left out (unless that is the filter).
                        val fixedCats = setOf("EMI", "Rent")
                        val flexShown = if (cat in fixedCats) shown else shown.filter { it.category !in fixedCats }
                        val fixedSpent = shown.filter { it.kind == Kind.EXPENSE && it.category in fixedCats }.rupees()
                        val fixedNote = if (fixedSpent <= 0 || cat in fixedCats) null else "Leaves out EMI and rent"
                        val dayStr = if (isMonth && daySel > 0) "%s-%02d".format(month, daySel) else null
                        val dayShown = if (dayStr != null) shown.filter { it.date == dayStr } else shown
                        BucketsView(dayShown, prevByCat, budgets, { budgetsOpen = true }, { open(Page.Trends) },
                            onCalendar = if (isMonth) ({ calOpen = !calOpen }) else null,
                            calActive = calOpen || daySel > 0,
                            dayLabel = if (dayStr != null) "$daySel ${Dates.monthLong(month)}" else null,
                            onClearDay = { daySel = 0 },
                            tabs = {
                                Spacer(Modifier.height(12.dp))
                                TabRowWrap {
                                    IconTab(Icons.Rounded.DonutLarge, stringResource(R.string.overview), 0xFFFF6B1A, 0xFFFF8A3D, spendTab == 0, modifier = Modifier.weight(1f)) { spendTab = 0 }
                                    IconTab(Icons.Rounded.Category, stringResource(R.string.categories), 0xFF8B5CF6, 0xFFA78BFA, spendTab == 1, modifier = Modifier.weight(1f)) { spendTab = 1 }
                                    IconTab(Icons.Rounded.Insights, stringResource(R.string.patterns), 0xFF06B6D4, 0xFF22D3EE, spendTab == 2, modifier = Modifier.weight(1f)) { spendTab = 2 }
                                }
                            },
                            showSummary = spendTab == 0,
                            showCategories = spendTab == 1,
                            slot = {
                                if (isMonth && calOpen) DailyCalendarBlock(rows, shown, f, month, account, daySel,
                                    { daySel = if (daySel == it) 0 else it }, { detailRow = it }, showList = false)
                            },
                            prevLabel = Dates.monthLong(Dates.shiftMonth(month, -1)).let { prev ->
                                if (month == Dates.todayMonth()) "the same days of $prev" else prev },
                        ) { title, rows -> drill = title to rows }
                        if (spendTab == 2 && dayStr == null) {
                            val wFrom: String; val wTo: String
                            if (isMonth) {
                                wFrom = "$month-01"
                                wTo = if (month == Dates.todayMonth()) Dates.today() else "$month-%02d".format(Dates.daysInMonth(month))
                            } else {
                                val ds = shown.filter { it.kind == Kind.EXPENSE }.map { it.date }
                                wFrom = ds.minOrNull() ?: Dates.today(); wTo = (ds.maxOrNull() ?: Dates.today()).coerceAtMost(Dates.today())
                            }
                            WeekdayPanel(flexShown, wFrom, wTo, fixedNote)
                            val recurring = remember(rows) { Analytics.recurring(rows, Dates.todayMonth()) }
                            RecurringPanel(recurring)
                        }
                        if (spendTab == 2) PaceView(rows, shown, f, month, account, excludeEmi, budget = monthlyBudget)
                        if (isMonth && daySel > 0) DailyCalendarBlock(rows, shown, f, month, account, daySel,
                            { daySel = if (daySel == it) 0 else it }, { detailRow = it }, showCal = false)
                    }
                    Tab.BUCKETS -> {
                        val prevByCat = if (period == "month")
                            Analytics.categoryTotals(rows, Dates.shiftMonth(month, -1), account) else emptyMap()
                        BucketsView(shown, prevByCat, budgets, { budgetsOpen = true }) { title, rows -> drill = title to rows }
                    }
                    Tab.PAYEES -> PayeesView(shown, { title, rows -> drill = title to rows },
                        { payee, id, kind, cat -> decide(id, kind, cat, payee, true) }, { cb -> newCatCallback = cb },
                        s::renamePayee,
                        remember(rows, fixed) { Analytics.fixedSet(rows, Dates.todayMonth(), fixed) }, rows)
                    Tab.INVEST -> InvestView(rows) { title, r -> drill = title to r }
                    Tab.PACE -> PaceView(rows, shown, f, month, account, excludeEmi, budget = monthlyBudget)
                    Tab.DUES -> RemindersView(reminders, rows, reminderState, s::markReminder,
                        onAdd = { n, a, d, r -> scope.launch {
                            withContext(Dispatchers.IO) { Storage.addManualReminder(context, n, a, d, r) }
                            reminders = withContext(Dispatchers.IO) { Storage.loadReminders(context) } } },
                        onRemove = { id -> scope.launch {
                            withContext(Dispatchers.IO) { Storage.removeManualReminder(context, id) }
                            reminders = withContext(Dispatchers.IO) { Storage.loadReminders(context) } } },
                        adding = addingBill, setAdding = { addingBill = it })
                }
                }
            }
            // Dues: add a bill from a round + button that stays put while the list scrolls.
            if (view == Tab.DUES) Box(
                Modifier.align(Alignment.BottomEnd).windowInsetsPadding(WindowInsets.navigationBars).padding(end = 18.dp, bottom = 84.dp)
                    .size(56.dp).clip(CircleShape).background(ZColor.Accent).clickable { addingBill = true }.buttonLabel("Add bill"),
                contentAlignment = Alignment.Center,
            ) { androidx.compose.material3.Icon(Icons.Rounded.Add, null, tint = ZColor.AccentInk, modifier = Modifier.size(28.dp)) }
            BottomNav(view, Modifier.align(Alignment.BottomCenter)) { view = it; daySel = 0 }
            undo?.let { u ->
                LaunchedEffect(u) { delay(6000); if (undo === u) undo = null }
                Row(Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = 88.dp).fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp)).background(ZColor.Ink).padding(start = 16.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(u.first, color = ZColor.Paper, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(stringResource(R.string.undo), color = ZColor.Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { undoLast() }.padding(horizontal = 14.dp, vertical = 12.dp))
                }
            }
            // A page opened over Home; Back closes it.
            androidx.activity.compose.BackHandler(page != null) { back() }
            when (val p = page) {
                Page.Trends -> TrendsScreen(rows, accounts, onBack = { back() }, onReview = { t, r -> drill = t to r })
                is Page.Card -> {
                    val a = p.account
                    CardDetailScreen(
                        account = a, allRows = rows, limit = limits[a]?.let { it.first - Analytics.cardUsedSince(rows, a, it) }, reminders = reminders,
                        reminderState = reminderState, pref = cardPrefs[a],
                        onSavePref = { d, b -> setCardPref(a, d, b) },
                        onMarkPaid = { id -> markReminder(id, true) },
                        onRowClick = { detailRow = it }, onBack = { back() },
                    )
                }
                null -> {}
            }
        }
    } }
}
