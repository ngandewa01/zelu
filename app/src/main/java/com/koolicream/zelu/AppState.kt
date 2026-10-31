package com.koolicream.zelu

import android.Manifest
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Everything the main screen keeps between taps: what was read from storage, which page and dialog are open, the
// filters, and the actions that change them. One holder, so the screen's parts (home, menu, dialogs) are separate
// functions that read it rather than one long function.
@Stable
class AppState(app: Application, private val handle: SavedStateHandle) : AndroidViewModel(app) {
    // Sample data runs on its own storage; the screen keeps a separate AppState for it (see ZeluApp). Never the
    // activity: this outlives it.
    val context: Context = if (Demo.on) DemoContext(app) else app
    val scope: CoroutineScope get() = viewModelScope

    // A value kept in the saved state, so it survives Android closing the app in the background, and observed by the
    // screen like any other state.
    private fun <T> saved(key: String, initial: T): ReadWriteProperty<Any?, T> {
        val state = mutableStateOf(handle.get<T>(key) ?: initial)
        return object : ReadWriteProperty<Any?, T> {
            override fun getValue(thisRef: Any?, property: KProperty<*>): T = state.value
            override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) { state.value = value; handle[key] = value }
        }
    }

    var rows by mutableStateOf<List<Row>>(emptyList())
    // Refunds that look like they belong to an earlier spend, waiting for a Yes/No: (refund, spend).
    var refundSuggestions by mutableStateOf<List<Pair<Row, Row>>>(emptyList())
    var reminders by mutableStateOf<List<Reminder>>(emptyList())
    var reminderState by mutableStateOf<Map<String, Boolean>>(emptyMap())
    var repaid by mutableStateOf<Map<String, Double>>(emptyMap())
    var budgets by mutableStateOf<Map<String, Double>>(emptyMap())
    var budgetsOpen by saved("budgetsOpen", false)
    var savingsOpen by saved("savingsOpen", false)
    var addingBill by mutableStateOf(false)
    var fixed by mutableStateOf<Map<String, Double>>(emptyMap())
    var menuOpen by mutableStateOf(false)
    var menuSection by mutableStateOf<String?>(null)   // the menu topic page open, if any
    var helpOpen by mutableStateOf(false)
    var reportOpen by mutableStateOf(false)
    var crashAsk by mutableStateOf(false)
    var emailOpen by mutableStateOf(false)
    var privacyOpen by mutableStateOf(false)
    var fullConfirm by mutableStateOf(false)
    var themeOpen by mutableStateOf(false)
    var lockOn by mutableStateOf(context.getSharedPreferences("zelu_security", android.content.Context.MODE_PRIVATE).getBoolean("lock", false))
    var undo by mutableStateOf<Pair<String, Map<String, String?>>?>(null)
    var unreadOpen by mutableStateOf(false)
    var checkOpen by mutableStateOf(false)
    // Read from the database by loadAll (not here, on the main thread, as the screen opens).
    var registry by mutableStateOf<Map<String, Pair<String, String>>>(emptyMap())
    var acctConfirmed by mutableStateOf(Storage.accountsConfirmed(context))
    var rescanRangeOpen by mutableStateOf(false)
    var check by mutableStateOf<Ingest.Check?>(null)
    var autoBackup by mutableStateOf(Backup.autoOn(context))
    var unreadList by mutableStateOf<List<RawSms>?>(null)
    var addFor by mutableStateOf<RawSms?>(null)
    var addNew by mutableStateOf(false)   // the "Add a transaction" form (cash and anything Zelu never saw a message for)
    var missedCount by mutableStateOf(0)
    var missedSeen by mutableStateOf(Storage.missedSeen(context))
    var eraseOpen by mutableStateOf(false)
    var dupOk by mutableStateOf<Set<String>>(emptySet())
    var sendersOpen by mutableStateOf(false)
    var senderList by mutableStateOf<List<Ingest.UnknownSender>?>(null)
    var catsOpen by mutableStateOf(false)
    var catOff by mutableStateOf<Set<String>>(emptySet())
    var splitFor by mutableStateOf<Row?>(null)
    var smsOk by mutableStateOf(true)
    var lastScan by saved("lastScan", 0L)
    var restoreUri by mutableStateOf<android.net.Uri?>(null)
    var backupPwOpen by mutableStateOf(false)
    var backupAfterPw by mutableStateOf(false)
    var rangeOpen by mutableStateOf(false)
    var newCatCallback by mutableStateOf<((String) -> Unit)?>(null)
    var balances by mutableStateOf<Map<String, Pair<Double, Long>>>(emptyMap())
    var limits by mutableStateOf<Map<String, Pair<Double, Long>>>(emptyMap())
    var cardPrefs by mutableStateOf<Map<String, Pair<Int, Double>>>(emptyMap())
    var monthlyBudget by mutableStateOf(0.0)
    var budgetDlg by mutableStateOf(false)
    // The category whose own budget is being set, from its row on the Spend page.
    var budgetFor by mutableStateOf<String?>(null)
    var lendPerson by saved<String?>("lendPerson", null)
    var borrowPerson by saved<String?>("borrowPerson", null)
    var view by saved("view", Tab.of(context.getSharedPreferences("zelu_ui", android.content.Context.MODE_PRIVATE).getString("start", Tab.LEDGER.key)))
    var period by saved("period", "month")
    var month by saved("month", Dates.todayMonth())
    var account by saved("account", "All")
    var daySel by saved("daySel", 0)
    var calOpen by mutableStateOf(false)
    // Spend page sub-tab: 0 Overview, 1 Categories, 2 Patterns.
    var spendTab by saved("spendTab", 0)
    var askOpen by mutableStateOf(false) // category picker open for ask
    var loading by mutableStateOf(false)
    // First-run reading: how many messages are read so far, and how many there are. The Setting up screen shows them.
    var scanDone by mutableStateOf(0)
    var scanTotal by mutableStateOf(0)
    // False while the app is in the background, so a long first read can say it is finished with a notification.
    var inForeground = true
    // True from the end of the first read until Home has finished rising into view.
    var homeIntro by mutableStateOf(false)
    var type by saved("type", "all")
    var cat by saved("cat", "All")
    var excludeEmi by saved("excludeEmi", false)
    var query by saved("query", "")
    var controlsOpen by saved("controlsOpen", false)
    var customFrom by saved("customFrom", Dates.shiftMonth(Dates.todayMonth(), -2) + "-01")
    var customTo by saved("customTo", Dates.today())
    var stage by saved("stage", Stage.SPLASH)

    // Pages opened over Home (Trends, a card), newest last; Back closes the top one. Saved as their routes.
    private var pageRoutes by saved("pages", "")
    val page: Page? get() = pageRoutes.lines().lastOrNull { it.isNotEmpty() }?.let { Page.of(it) }
    fun open(p: Page) { pageRoutes = (pageRoutes.lines().filter { it.isNotEmpty() } + p.route).joinToString("\n") }
    fun back(): Boolean {
        val open = pageRoutes.lines().filter { it.isNotEmpty() }
        if (open.isEmpty()) return false
        pageRoutes = open.dropLast(1).joinToString("\n")
        return true
    }

    // What account and card tiles anywhere on screen read (see LocalAccountInfo): card networks, labels joined into
    // each account, limits you typed, and which card is waiting for one.
    var cardNetworks by mutableStateOf<Map<String, String>>(emptyMap())
    var manualLimits by mutableStateOf<Set<String>>(emptySet())
    var limitAskFor by mutableStateOf<String?>(null)
    fun refreshCardNetworks() { scope.launch { cardNetworks = withContext(Dispatchers.IO) { Storage.cardNetworks(context) } } }
    var permDenied by mutableStateOf(false)
    var onbSms by mutableStateOf(false)
    var onbNotif by mutableStateOf(false)
    var detailRow by mutableStateOf<Row?>(null)
    var drill by mutableStateOf<Pair<String, List<Row>>?>(null)
    var notifBlocked by mutableStateOf(false)

    // Registered by the screen each time it is drawn (Android ties them to the activity).
    var notifLauncher: ActivityResultLauncher<String>? = null
    var restoreLauncher: ActivityResultLauncher<Array<String>>? = null
    var storageLauncher: ActivityResultLauncher<String>? = null
    // What to save once storage access is given (Android 7-9 only; see Exporter.needsPermission).
    var afterStorage: (() -> Unit)? = null

    /** Runs [save] now, or first asks for storage access on Android 7-9 and runs it once given. */
    fun withStorage(save: () -> Unit) {
        val launcher = storageLauncher
        if (!Exporter.needsPermission(context) || launcher == null) { save(); return }
        afterStorage = save
        launcher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    fun noteMissed(n: Int) { missedCount = n; missedSeen = n; Storage.setMissedSeen(context, n) }

    fun backupNow() = withStorage {
        scope.launch {
            val body = withContext(Dispatchers.IO) { Backup.exportEncrypted(context) }
            val ok = body != null && withContext(Dispatchers.IO) {
                Exporter.exportText(context, "zelu-backup-${Dates.today()}.json", "application/json", body)
            }
            if (ok) { Backup.markManual(context); withContext(Dispatchers.IO) { Exporter.pruneBackups(context) } }
            Toast.makeText(context, if (ok) "Backup saved to Downloads/Zelu" else "Couldn't save the backup to Downloads", Toast.LENGTH_LONG).show()
        }
    }

    fun hasPerm() = context is DemoContext || ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
        PackageManager.PERMISSION_GRANTED

    /** Reads the payments again (and the refund questions that go with them) after something changed. */
    suspend fun refreshRows() {
        val (r, refunds) = withContext(Dispatchers.IO) { Storage.loadRowsAndRefunds(context) }
        refundSuggestions = refunds
        rows = r
    }

    fun reload() {
        scope.launch { loadAll() }
    }

    /** Reads everything saved (payments, balances, card limits, bills, budgets, prefs) and shows it in one go. */
    suspend fun loadAll() {
        run {
            // Read everything first, then show it in one go, so balances never trail behind the payments.
            class Loaded(
                val rows: Pair<List<Row>, List<Pair<Row, Row>>>, val reminders: List<Reminder>, val reminderState: Map<String, Boolean>,
                val repaid: Map<String, Double>, val budgets: Map<String, Double>, val monthlyBudget: Double,
                val fixed: Map<String, Double>, val balances: Map<String, Pair<Double, Long>>,
                val limits: Map<String, Pair<Double, Long>>, val manual: Set<String>,
                val cardPrefs: Map<String, Pair<Int, Double>>, val customCats: Map<String, Pair<String, Long>>,
                val registry: Map<String, Pair<String, String>>,
            )
            val l = withContext(Dispatchers.IO) {
                IncomeCats.load(context)
                Loaded(
                    Storage.loadRowsAndRefunds(context), Storage.loadReminders(context), Storage.loadReminderState(context),
                    Storage.loadRepaid(context), Storage.loadBudgets(context), Storage.loadMonthlyBudget(context),
                    Storage.loadFixed(context), Storage.loadBalances(context), Storage.loadLimits(context),
                    Storage.manualLimitKeys(context), Storage.loadCardPrefs(context), Storage.loadCustomCats(context),
                    Storage.accountRegistry(context),
                )
            }
            manualLimits = l.manual
            cardNetworks = withContext(Dispatchers.IO) { Storage.cardNetworks(context) }
            dupOk = withContext(Dispatchers.IO) { Storage.dupOk(context) }
            catOff = withContext(Dispatchers.IO) { Storage.categoriesOff(context) }
            cardPrefs = l.cardPrefs
            CustomCats.map = l.customCats
            registry = l.registry
            reminders = l.reminders; reminderState = l.reminderState; repaid = l.repaid
            budgets = l.budgets; monthlyBudget = l.monthlyBudget; fixed = l.fixed
            balances = l.balances; limits = l.limits
            refundSuggestions = l.rows.second
            rows = l.rows.first
        }
    }

    // Continue / Done on the Your accounts screen: what is shown and not removed is yours, so Zelu stops asking about it.
    fun confirmAccounts() {
        scope.launch {
            withContext(Dispatchers.IO) { Storage.confirmShownAccounts(context); Storage.setAccountsConfirmed(context) }
            acctConfirmed = true
            undo = null
            registry = withContext(Dispatchers.IO) { Storage.accountRegistry(context) }
            stage = Stage.APP
        }
    }

    fun setFixed(name: String, amount: Double) {
        scope.launch {
            withContext(Dispatchers.IO) { Storage.setFixed(context, name, amount) }
            fixed = withContext(Dispatchers.IO) { Storage.loadFixed(context) }
        }
    }

    // Two messages for one payment: hide the one that says less (it is the same payment), or keep both (they are two).
    fun hideDuplicate(a: Row, b: Row) {
        fun info(r: Row) = (if (!r.counterparty.isNullOrBlank()) 1 else 0) + (if (!r.ref.isNullOrBlank()) 1 else 0)
        val drop = if (info(b) > info(a)) a else b
        decide(drop.id, Kind.IGNORE, "Duplicate", null, false)
    }

    fun keepBothDuplicates(a: Row, b: Row) {
        scope.launch {
            withContext(Dispatchers.IO) { Storage.markDupOk(context, a.id + "|" + b.id) }
            dupOk = withContext(Dispatchers.IO) { Storage.dupOk(context) }
        }
    }

    fun answerRefund(refundId: String, yes: Boolean) {
        scope.launch {
            val snap = withContext(Dispatchers.IO) { Storage.snapshot(context) }
            withContext(Dispatchers.IO) { Storage.setRefundLink(context, refundId, yes) }
            refreshRows()
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

    /**
     * Reads the inbox again. [quick] (coming back to the app) reads only what arrived since the last read, with two days'
     * overlap for messages the phone delivered late, and leaves the Missed count as it was.
     */
    fun rescan(full: Boolean = false, notify: Boolean = false, depth: Int = 5000, quick: Boolean = false) {
        scope.launch {
            loading = true
            lastScan = System.currentTimeMillis()
            var added = 0
            var missed = 0
            var fresh = false
            withContext(Dispatchers.IO) {
                fresh = full || Storage.dataVersion(context) < Storage.PARSER_VERSION
                val since = Storage.lastRead(context)
                val inbox = if (quick && !fresh && since > 0L)
                    SmsReader.readInboxBetween(context, since - 2L * 86_400_000L, System.currentTimeMillis()).take(depth)
                else SmsReader.readInbox(context, if (fresh) maxOf(depth, 20_000) else depth)
                val firstRun = stage == Stage.SCAN
                val onProgress: ((Int, Int) -> Unit)? = if (firstRun) ({ done, total -> scanDone = done; scanTotal = total }) else null
                if (firstRun) { scanDone = 0; scanTotal = inbox.size }
                added = (if (fresh) Ingest.rebuild(context, inbox, onProgress) else Ingest.ingest(context, inbox, onProgress)).added
                Storage.setDataVersion(context, Storage.PARSER_VERSION)
                Storage.setReadStamp(context, System.currentTimeMillis(), inbox.filter { Parser.isFinancialSender(it.address) }.maxOfOrNull { it.date } ?: 0L)
            }
            // Refresh everything (balances, card limits, fixed plan, prefs too), not just the rows. Shown before the
            // missed-message count below, which reads the inbox a second time.
            loadAll()
            loading = false
            if (!quick || fresh || added > 0) {
                missed = withContext(Dispatchers.IO) { Ingest.unreadable(context).size }
                missedCount = missed
            }
            if (stage == Stage.SCAN) finishFirstRun()
            if (notify) Toast.makeText(context, if (full) "Read everything again" else if (added == 0) "Nothing new in your messages" else plural(added, "new payment", "new payments") + " found", Toast.LENGTH_LONG).show()
        }
    }

    // The first read ends here. The accounts Zelu found are taken as yours (Settings, Linked accounts, fixes any) and Home
    // opens. If the person left the app while it read, a notification says it is ready.
    private fun finishFirstRun() {
        scanDone = scanTotal
        homeIntro = true
        scope.launch {
            if (inForeground) delay(700) else if (Notifier.enabled(context)) Notifier.notifyReady(context, rows.size)
            confirmAccounts()
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

    fun checkNotif() { notifBlocked = !Notifier.enabled(context) }

    fun fixNotif() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, "android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED)
            notifLauncher?.launch("android.permission.POST_NOTIFICATIONS")
        else Notifier.openSettings(context)
    }

    // decisions
    fun decide(id: String, kind: Kind, category: String, payee: String?, remember: Boolean) {
        scope.launch {
            val snap = withContext(Dispatchers.IO) { Storage.snapshot(context) }
            withContext(Dispatchers.IO) {
                Storage.setOverride(context, id, kind, category)
                if (remember && !payee.isNullOrBlank()) Storage.setPayeeRule(context, payee, kind, category)
            }
            refreshRows()
            askOpen = false
            undo = (if (remember && !payee.isNullOrBlank()) "Moved $payee to $category" else "Moved to $category") to snap
        }
    }

    fun renamePayee(from: String, to: String) {
        scope.launch {
            val snap = withContext(Dispatchers.IO) { Storage.snapshot(context) }
            withContext(Dispatchers.IO) { Storage.setAlias(context, from, to) }
            refreshRows()
            undo = (if (to.isBlank()) "Name reset" else "Renamed to ${to.trim()}") to snap
        }
    }

    fun retitle(id: String, title: String) {
        scope.launch {
            val snap = withContext(Dispatchers.IO) { Storage.snapshot(context) }
            withContext(Dispatchers.IO) { Storage.setTitle(context, id, title) }
            refreshRows()
            undo = "Title changed" to snap
        }
    }

    fun undoLast() {
        val u = undo ?: return
        undo = null
        scope.launch {
            withContext(Dispatchers.IO) { Storage.restoreSnapshot(context, u.second) }
            registry = withContext(Dispatchers.IO) { Storage.accountRegistry(context) }
            refreshRows()
            balances = withContext(Dispatchers.IO) { Storage.loadBalances(context) }
            limits = withContext(Dispatchers.IO) { Storage.loadLimits(context) }
        }
    }

    fun relink(msg: String, change: () -> Unit) {
        scope.launch {
            val snap = withContext(Dispatchers.IO) { Storage.snapshot(context) }
            withContext(Dispatchers.IO) { change() }
            registry = withContext(Dispatchers.IO) { Storage.accountRegistry(context) }
            refreshRows()
            balances = withContext(Dispatchers.IO) { Storage.loadBalances(context) }
            limits = withContext(Dispatchers.IO) { Storage.loadLimits(context) }
            undo = msg to snap
        }
    }

    fun saveAccountPlan(plan: AccountBook.Plan, msg: String) = relink(msg) { Storage.applyAccountPlan(context, plan) }
    fun declineAcct(key: String, type: String) = relink("Removed " + AccountBook.displayName(key)) { Storage.declineAccount(context, key, type) }
    fun restoreAcct(key: String) = relink("Brought back " + AccountBook.displayName(key)) { Storage.restoreAccount(context, key) }

}

// A page opened over Home. [route] is how it is kept in the saved state.
sealed class Page(val route: String) {
    object Trends : Page("trends")
    data class Card(val account: String) : Page("card:$account")
    data class Payee(val name: String) : Page("payee:$name")
    data class Category(val name: String) : Page("category:$name")

    companion object {
        fun of(route: String): Page? = when {
            route == Trends.route -> Trends
            route.startsWith("card:") -> Card(route.removePrefix("card:"))
            route.startsWith("payee:") -> Payee(route.removePrefix("payee:"))
            route.startsWith("category:") -> Category(route.removePrefix("category:"))
            else -> null
        }
    }
}

// The payments the filters pick out, worked out each time the screen is drawn.
class Filtered(val accounts: List<String>, val bankAccounts: List<String>, val f: Analytics.Filter, val list: List<Row>, val shown: List<Row>)
