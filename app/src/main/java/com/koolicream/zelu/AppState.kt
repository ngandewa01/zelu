package com.koolicream.zelu

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Everything the main screen keeps between taps: what was read from storage, which page and dialog are open, the
// filters, and the actions that change them. One holder, so the screen's parts (home, menu, dialogs) are separate
// functions that read it rather than one long function.
@Stable
class AppState(val context: Context, val scope: CoroutineScope) {
    var rows by mutableStateOf<List<Row>>(emptyList())
    var reminders by mutableStateOf<List<Reminder>>(emptyList())
    var reminderState by mutableStateOf<Map<String, Boolean>>(emptyMap())
    var repaid by mutableStateOf<Map<String, Double>>(emptyMap())
    var budgets by mutableStateOf<Map<String, Double>>(emptyMap())
    var budgetsOpen by mutableStateOf(false)
    var savingsOpen by mutableStateOf(false)
    var trendsOpen by mutableStateOf(false)
    var fixed by mutableStateOf<Map<String, Double>>(emptyMap())
    var menuOpen by mutableStateOf(false)
    var menuPage by mutableStateOf("")
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
    var registry by mutableStateOf(Storage.accountRegistry(context))
    var acctConfirmed by mutableStateOf(Storage.accountsConfirmed(context))
    var rescanRangeOpen by mutableStateOf(false)
    var check by mutableStateOf<Ingest.Check?>(null)
    var autoBackup by mutableStateOf(Backup.autoOn(context))
    var unreadList by mutableStateOf<List<RawSms>?>(null)
    var addFor by mutableStateOf<RawSms?>(null)
    var missedCount by mutableStateOf(0)
    var missedSeen by mutableStateOf(Storage.missedSeen(context))
    var eraseOpen by mutableStateOf(false)
    var dupOk by mutableStateOf(Storage.dupOk(context))
    var dupOpen by mutableStateOf(false)
    var sendersOpen by mutableStateOf(false)
    var senderList by mutableStateOf<List<Ingest.UnknownSender>?>(null)
    var catsOpen by mutableStateOf(false)
    var catOff by mutableStateOf(Storage.categoriesOff(context))
    var splitFor by mutableStateOf<Row?>(null)
    var smsOk by mutableStateOf(true)
    var lastScan = 0L
    var restoreUri by mutableStateOf<android.net.Uri?>(null)
    var backupPwOpen by mutableStateOf(false)
    var backupAfterPw by mutableStateOf(false)
    var rangeOpen by mutableStateOf(false)
    var newCatCallback by mutableStateOf<((String) -> Unit)?>(null)
    var balances by mutableStateOf<Map<String, Pair<Double, Long>>>(emptyMap())
    var limits by mutableStateOf<Map<String, Pair<Double, Long>>>(emptyMap())
    var cardPrefs by mutableStateOf<Map<String, Pair<Int, Double>>>(emptyMap())
    var cardOpen by mutableStateOf<String?>(null)
    var monthlyBudget by mutableStateOf(0.0)
    var budgetDlg by mutableStateOf(false)
    var lendPerson by mutableStateOf<String?>(null)
    var borrowPerson by mutableStateOf<String?>(null)
    var view by mutableStateOf(context.getSharedPreferences("zelu_ui", android.content.Context.MODE_PRIVATE).getString("start", "ledger") ?: "ledger")
    var period by mutableStateOf("month")
    var month by mutableStateOf(Dates.todayMonth())
    var account by mutableStateOf("All")
    var daySel by mutableStateOf(0)
    var calOpen by mutableStateOf(false)
    // Spend page sub-tab: 0 Overview, 1 Categories, 2 Patterns.
    var spendTab by mutableStateOf(0)
    var askOpen by mutableStateOf(false) // category picker open for ask
    var loading by mutableStateOf(false)
    var type by mutableStateOf("all")
    var cat by mutableStateOf("All")
    var excludeEmi by mutableStateOf(false)
    var query by mutableStateOf("")
    var controlsOpen by mutableStateOf(false)
    var customFrom by mutableStateOf(Dates.shiftMonth(Dates.todayMonth(), -2) + "-01")
    var customTo by mutableStateOf(Dates.today())
    var stage by mutableStateOf("splash") // splash, welcome, perm, scan, app
    var permDenied by mutableStateOf(false)
    var onbSms by mutableStateOf(false)
    var onbNotif by mutableStateOf(false)
    var detailRow by mutableStateOf<Row?>(null)
    var drill by mutableStateOf<Pair<String, List<Row>>?>(null)
    var notifBlocked by mutableStateOf(false)

    // Registered by the screen each time it is drawn (Android ties them to the activity).
    var notifLauncher: ActivityResultLauncher<String>? = null
    var restoreLauncher: ActivityResultLauncher<Array<String>>? = null

    fun noteMissed(n: Int) { missedCount = n; missedSeen = n; Storage.setMissedSeen(context, n) }
    fun backupNow() {
        scope.launch {
            val body = withContext(Dispatchers.IO) { Backup.exportEncrypted(context) }
            val ok = body != null && withContext(Dispatchers.IO) {
                Exporter.exportText(context, "zelu-backup-${Dates.today()}.json", "application/json", body)
            }
            if (ok) { Backup.markManual(context); withContext(Dispatchers.IO) { Exporter.pruneBackups(context) } }
            Toast.makeText(context, if (ok) "Backup saved to Downloads/Zelu" else "Backup needs Android 10+", Toast.LENGTH_LONG).show()
        }
    }
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
            lastScan = System.currentTimeMillis()
            var added = 0
            var missed = 0
            withContext(Dispatchers.IO) {
                val fresh = full || Storage.dataVersion(context) < Storage.PARSER_VERSION
                val inbox = SmsReader.readInbox(context, if (fresh) maxOf(depth, 20_000) else depth)
                added = (if (fresh) Ingest.rebuild(context, inbox) else Ingest.ingest(context, inbox)).added
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

    fun checkNotif() { notifBlocked = !Notifier.enabled(context) }
    fun fixNotif() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, "android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED)
            notifLauncher?.launch("android.permission.POST_NOTIFICATIONS")
        else Notifier.openSettings(context)
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

    companion object {
        // Where you are and the filters you set, kept when Android closes the app in the background or the theme
        // changes. What was read from storage is read again.
        fun saver(context: Context, scope: CoroutineScope): Saver<AppState, Any> = mapSaver(
            save = { s ->
                mapOf(
                    "budgetsOpen" to s.budgetsOpen,
                    "savingsOpen" to s.savingsOpen,
                    "trendsOpen" to s.trendsOpen,
                    "lastScan" to s.lastScan,
                    "cardOpen" to s.cardOpen,
                    "lendPerson" to s.lendPerson,
                    "borrowPerson" to s.borrowPerson,
                    "view" to s.view,
                    "period" to s.period,
                    "month" to s.month,
                    "account" to s.account,
                    "daySel" to s.daySel,
                    "spendTab" to s.spendTab,
                    "type" to s.type,
                    "cat" to s.cat,
                    "excludeEmi" to s.excludeEmi,
                    "query" to s.query,
                    "controlsOpen" to s.controlsOpen,
                    "customFrom" to s.customFrom,
                    "customTo" to s.customTo,
                    "stage" to s.stage,
                )
            },
            restore = { m ->
                AppState(context, scope).apply {
                    (m["budgetsOpen"] as? Boolean)?.let { budgetsOpen = it }
                    (m["savingsOpen"] as? Boolean)?.let { savingsOpen = it }
                    (m["trendsOpen"] as? Boolean)?.let { trendsOpen = it }
                    (m["lastScan"] as? Long)?.let { lastScan = it }
                    cardOpen = m["cardOpen"] as? String
                    lendPerson = m["lendPerson"] as? String
                    borrowPerson = m["borrowPerson"] as? String
                    (m["view"] as? String)?.let { view = it }
                    (m["period"] as? String)?.let { period = it }
                    (m["month"] as? String)?.let { month = it }
                    (m["account"] as? String)?.let { account = it }
                    (m["daySel"] as? Int)?.let { daySel = it }
                    (m["spendTab"] as? Int)?.let { spendTab = it }
                    (m["type"] as? String)?.let { type = it }
                    (m["cat"] as? String)?.let { cat = it }
                    (m["excludeEmi"] as? Boolean)?.let { excludeEmi = it }
                    (m["query"] as? String)?.let { query = it }
                    (m["controlsOpen"] as? Boolean)?.let { controlsOpen = it }
                    (m["customFrom"] as? String)?.let { customFrom = it }
                    (m["customTo"] as? String)?.let { customTo = it }
                    (m["stage"] as? String)?.let { stage = it }
                }
            },
        )
    }
}

// The payments the filters pick out, worked out each time the screen is drawn.
class Filtered(val accounts: List<String>, val bankAccounts: List<String>, val f: Analytics.Filter, val list: List<Row>, val shown: List<Row>)
