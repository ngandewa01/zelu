package com.koolicream.zelu

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// The menu fits on one screen: a search that forgives typos, what needs fixing (or "All good"), one suggestion made
// from your own data, four shortcuts learned from what you use, and seven topics with their status. A topic opens its
// own short page. Every row does its job directly; switches change in place and can be undone.
@Composable
internal fun MenuDialog(s: AppState, v: Filtered) {
    if (s.menuOpen) Dialog(onDismissRequest = { if (s.menuSection != null) s.menuSection = null else s.menuOpen = false },
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
        MenuPage(s, v)
    }
}

// One row of the menu. on is set for a switch; more rows stay under "More options" until opened or searched for.
private class Entry(
    val section: String, val title: String, val sub: String, val icon: ImageVector, val tint: Color,
    val words: String = "", val on: Boolean? = null, val more: Boolean = false, val button: String? = null,
    val value: String? = null, val warn: Boolean = false, val act: () -> Unit,
)

// What you use in the menu (for the shortcuts) and the suggestions you said no to. Kept on this phone.
private object MenuUse {
    private fun sp(c: android.content.Context) = c.getSharedPreferences("zelu_menu", android.content.Context.MODE_PRIVATE)
    fun note(c: android.content.Context, key: String) { val p = sp(c); p.edit().putInt("use.$key", p.getInt("use.$key", 0) + 1).apply() }
    fun count(c: android.content.Context, key: String) = sp(c).getInt("use.$key", 0)
    fun dismissed(c: android.content.Context, id: String) = sp(c).getBoolean("no.$id", false)
    fun dismiss(c: android.content.Context, id: String) = sp(c).edit().putBoolean("no.$id", true).apply()
}

// One suggestion made from your own data, with a button that does it.
private class Suggestion(val id: String, val title: String, val sub: String, val button: String, val act: () -> Unit)

private const val ATTENTION = "Needs attention"
private const val MONEY = "Money"
private const val ACCOUNTS = "Accounts"
private const val MESSAGES = "Bank messages"
private const val ALERTS = "Alerts"
private const val BACKUP = "Backup and privacy"
private const val LOOK = "Look"
private const val HELP = "Help"
private val START_PAGES = listOf("ledger" to "Home", "spend" to "Spend", "payees" to "Payees", "dues" to "Dues")

@Composable
private fun MenuPage(s: AppState, v: Filtered) {
    with(s) {
        var find by remember { mutableStateOf("") }
        var tick by remember { mutableStateOf(0) }                 // a setting changed here: read them all again
        var opened by remember { mutableStateOf(setOf<String>()) }  // sections showing their "More options"
        var startPick by remember { mutableStateOf(false) }
        var undo by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
        var leadPick by remember { mutableStateOf(false) }  // switch just changed, and whether it is now on
        fun shut() { menuOpen = false; menuSection = null }
        fun openMissed() {
            shut(); unreadOpen = true; unreadList = null
            scope.launch { val l = withContext(Dispatchers.IO) { Ingest.unreadable(context) }; unreadList = l; noteMissed(l.size) }
        }

        // Keyed on tick: a setting changed on this page redraws it with the saved values read below.
        val ui = remember(tick) { context.getSharedPreferences("zelu_ui", android.content.Context.MODE_PRIVATE) }
        val startNow = ui.getString("start", "ledger") ?: "ledger"
        val missedNew = missedCount - missedSeen
        val hasPw = Backup.hasPassword(context)
        val lastRead = Storage.lastRead(context)
        val newest = Storage.newestBank(context)
        val stale = newest > 0 && System.currentTimeMillis() - newest > 7L * 86_400_000L
        val removed = registry.count { it.value.first == "declined" } + Storage.hiddenAccounts(context).size
        val spendOn = NotifyPrefs.spend(context)
        val dailyOn = NotifyPrefs.dailyOn(context)
        val weeklyOn = NotifyPrefs.weeklyOn(context)
        val duesOn = NotifyPrefs.duesOn(context)
        val recapOn = NotifyPrefs.recapOn(context)
        val lastBackup = Backup.lastAny(context)

        val all = buildList {
            if (missedNew > 0) add(Entry(ATTENTION, plural(missedNew, "new missed message", "new missed messages"),
                "Bank messages Zelu couldn't read. Add them or teach Zelu.", Icons.Rounded.Receipt, ZColor.Warn, button = "Review") { openMissed() })
            if (notifBlocked) add(Entry(ATTENTION, "Alerts are off", "Turn on notifications for payment and bill alerts",
                Icons.Rounded.Notifications, ZColor.Warn, button = "Turn on") { shut(); fixNotif() })
            if (!hasPw && rows.isNotEmpty()) add(Entry(ATTENTION, "Set a backup password", "Backups wait until you set one",
                Icons.Rounded.Lock, ZColor.Warn, button = "Set") { shut(); backupAfterPw = false; backupPwOpen = true })
            if (Demo.on) add(Entry(ATTENTION, "Sample data is on", "Made-up payments. Tap to go back to yours", Icons.Rounded.Visibility, ZColor.Warn, button = "Turn off") {
                shut(); Demo.set(context, false)
                Toast.makeText(context, "Back to your own payments", Toast.LENGTH_SHORT).show()
            })

            add(Entry(MONEY, "Monthly budget", "", Icons.Rounded.AccountBalanceWallet, ZColor.Accent, "limit spending",
                value = if (monthlyBudget > 0) money(monthlyBudget) else "Not set") { shut(); budgetDlg = true })
            add(Entry(MONEY, "Budgets by category", "A limit for food, shopping and the rest", Icons.Rounded.BarChart, ZColor.Accent,
                "category limit") { shut(); budgetsOpen = true })
            add(Entry(MONEY, "Categories", "What counts as spending, and income categories", Icons.Rounded.Category, ZColor.Accent,
                "counted ignore income") { shut(); catsOpen = true })
            add(Entry(MONEY, "Savings plan", "What's left after SIPs, EMIs and goals", Icons.Rounded.Savings, ZColor.Income,
                "sip emi goal") { shut(); savingsOpen = true })
            add(Entry(MONEY, "Investments", "EPF, mutual funds, SIPs", Icons.Rounded.TrendingUp, ZColor.Income,
                "epf nps mutual fund stocks") { shut(); view = "invest"; daySel = 0 })

            add(Entry(ACCOUNTS, "Your accounts", "Join duplicates, say what each one is, remove what isn't yours",
                Icons.Rounded.AccountBalance, ZColor.Accent, "bank card loan merge join duplicate remove") { shut(); stage = "accounts" })
            if (removed > 0) add(Entry(ACCOUNTS, "Bring back removed accounts", "removed hidden", Icons.Rounded.Restore,
                ZColor.Muted, "hidden restore declined", value = "$removed") {
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
            })

            add(Entry(MESSAGES, "Check for new payments",
                if (lastRead == 0L) "Look for anything new in your messages"
                else "Last checked " + agoText(lastRead) + (if (newest > 0) " · newest bank SMS " + agoText(newest) else ""),
                Icons.Rounded.Restore, if (stale) ZColor.Warn else ZColor.Accent, "rescan refresh sync update",
                value = if (lastRead > 0) agoText(lastRead) else null, warn = stale) { shut(); rescan(notify = true) })
            add(Entry(MESSAGES, "Missed messages",
                if (missedCount > 0) "$missedCount waiting · add by hand or teach Zelu" else "Money messages Zelu couldn't read",
                Icons.Rounded.Receipt, ZColor.Warn, "unread sms add manually teach",
                value = if (missedCount > 0) "$missedCount" else null, warn = missedCount > 0) { openMissed() })
            add(Entry(MESSAGES, "Banks Zelu doesn't know", "A bank or wallet that was skipped? Tell Zelu here", Icons.Rounded.AccountBalance,
                ZColor.Muted, "unrecognised senders wallet sender") {
                shut(); sendersOpen = true; senderList = null
                scope.launch { senderList = withContext(Dispatchers.IO) { Ingest.unrecognisedSenders(context) } }
            })
            add(Entry(MESSAGES, "Check a date range", "Read only the messages of a period you pick", Icons.Rounded.CalendarMonth, ZColor.Muted,
                "rescan between dates period", more = true) { shut(); rescanRangeOpen = true })
            add(Entry(MESSAGES, "Read older messages", "Goes back through your whole inbox. Can take a minute.", Icons.Rounded.Restore,
                ZColor.Muted, "history old", more = true) { shut(); rescan(notify = true, depth = 100_000) })
            add(Entry(MESSAGES, "Data check", "What was read, and whether balances add up", Icons.Rounded.Receipt, ZColor.Muted,
                "diagnose balance gaps missing", more = true) {
                shut(); checkOpen = true; check = null
                scope.launch { check = withContext(Dispatchers.IO) { Ingest.diagnose(context) } }
            })
            add(Entry(MESSAGES, "Read everything again", "Reads all messages afresh. Your edits stay.", Icons.Rounded.Warning, ZColor.Muted,
                "reset rebuild start again", more = true) { shut(); fullConfirm = true })

            add(Entry(ALERTS, "Payment alerts", "A note for every payment or balance message", Icons.Rounded.Notifications, ZColor.Accent,
                "notification", on = spendOn) { NotifyPrefs.setSpend(context, !spendOn); tick++ })
            add(Entry(ALERTS, "Daily summary", "What you spent today", Icons.Rounded.BarChart, ZColor.Accent, "notification",
                on = dailyOn) { NotifyPrefs.setDailyOn(context, !dailyOn); tick++ })
            if (dailyOn) add(Entry(ALERTS, "Daily summary time", "", Icons.Rounded.CalendarMonth,
                ZColor.Muted, "notification", value = NotifyPrefs.timeText(NotifyPrefs.dailyAt(context))) {
                val at = NotifyPrefs.dailyAt(context)
                android.app.TimePickerDialog(context, { _, h, m -> NotifyPrefs.setDailyAt(context, h * 60 + m); tick++ }, at / 60, at % 60, false).show()
            })
            add(Entry(ALERTS, "Weekly summary", "Sundays: the week against the one before", Icons.Rounded.BarChart, ZColor.Accent,
                "notification", on = weeklyOn) { NotifyPrefs.setWeeklyOn(context, !weeklyOn); tick++ })
            if (weeklyOn) add(Entry(ALERTS, "Weekly summary time", "Sundays",
                Icons.Rounded.CalendarMonth, ZColor.Muted, "notification", value = "Sun " + NotifyPrefs.timeText(NotifyPrefs.weeklyAt(context))) {
                val at = NotifyPrefs.weeklyAt(context)
                android.app.TimePickerDialog(context, { _, h, m -> NotifyPrefs.setWeeklyAt(context, h * 60 + m); tick++ }, at / 60, at % 60, false).show()
            })
            add(Entry(ALERTS, "Bill reminders", "Before a bill or card is due, around 9 AM", Icons.Rounded.Receipt, ZColor.Accent,
                "dues notification", on = duesOn) { NotifyPrefs.setDuesOn(context, !duesOn); tick++ })
            if (duesOn) add(Entry(ALERTS, "Remind me", "bill reminder days before", Icons.Rounded.CalendarMonth, ZColor.Muted, "dues days before",
                value = listOf("3 days before", "2 days before", "On the day", "A week before").getOrElse(NotifyPrefs.leadIndex(context)) { "" }) { leadPick = true })
            add(Entry(ALERTS, "Monthly recap", "A note in the first days of a new month", Icons.Rounded.TrendingUp, ZColor.Accent,
                "notification", on = recapOn) { NotifyPrefs.setRecapOn(context, !recapOn); tick++ })
            add(Entry(ALERTS, "Test alert", if (notifBlocked) "Alerts are off · tap to turn on" else "Send a sample payment alert",
                Icons.Rounded.Notifications, if (notifBlocked) ZColor.Warn else ZColor.Muted, "notification try", more = true) {
                shut()
                if (notifBlocked) fixNotif() else Notifier.notifyTest(context)
            })

            add(Entry(BACKUP, "Back up now", "Save everything to Downloads/Zelu, locked with a password",
                Icons.Rounded.CloudUpload, ZColor.Income, "save copy",
                value = if (lastBackup > 0) java.text.SimpleDateFormat("d MMM", java.util.Locale.US).format(java.util.Date(lastBackup)) else "Never",
                warn = lastBackup == 0L) {
                shut()
                if (hasPw) backupNow() else { backupAfterPw = true; backupPwOpen = true }
            })
            add(Entry(BACKUP, "Weekly backup",
                if (hasPw) "Saved to Downloads/Zelu every week; the newest 4 are kept" else "Needs a backup password first",
                Icons.Rounded.CloudUpload, ZColor.Income, "automatic", on = autoBackup && hasPw) {
                // Without a password a weekly backup can't run: ask for one first instead of showing a switch that does nothing.
                if (!hasPw) { shut(); backupAfterPw = false; backupPwOpen = true }
                else { autoBackup = !autoBackup; Backup.setAutoOn(context, autoBackup) }
            })
            add(Entry(BACKUP, "Backup password", "needed to restore on a new phone",
                Icons.Rounded.Lock, if (hasPw) ZColor.Income else ZColor.Warn, value = if (hasPw) "Set" else "Not set", warn = !hasPw) { shut(); backupAfterPw = false; backupPwOpen = true })
            add(Entry(BACKUP, "Restore from a backup", "Load a backup file, for example on a new phone", Icons.Rounded.Restore, ZColor.Muted,
                "import new phone") { shut(); restoreLauncher?.launch(arrayOf("*/*")) })
            add(Entry(BACKUP, "App lock", "Ask for your phone lock when opening Zelu", Icons.Rounded.Lock, ZColor.Income,
                "pin fingerprint password security", on = lockOn) {
                val km = context.getSystemService(android.content.Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
                if (!lockOn && !km.isDeviceSecure) {
                    Toast.makeText(context, "Set a screen lock (PIN, pattern or fingerprint) in your phone settings first", Toast.LENGTH_LONG).show()
                } else {
                    lockOn = !lockOn
                    context.getSharedPreferences("zelu_security", android.content.Context.MODE_PRIVATE).edit().putBoolean("lock", lockOn).apply()
                }
            })
            add(Entry(BACKUP, "Export to Excel (CSV)", "What's on screen now, to Downloads/Zelu", Icons.Rounded.FileDownload, ZColor.Muted,
                "csv excel spreadsheet download") {
                shut()
                val ok = Exporter.exportCsv(context, v.shown, "zelu-${Dates.today()}.csv")
                Toast.makeText(context, if (ok) "Saved to Downloads/Zelu" else "Export needs Android 10+", Toast.LENGTH_SHORT).show()
            })
            add(Entry(BACKUP, "Erase everything", "Delete all Zelu data from this phone", Icons.Rounded.Warning, ZColor.Warn,
                "delete wipe reset") { shut(); eraseOpen = true })

            add(Entry(LOOK, "Theme colours", "", Icons.Rounded.Palette, ZColor.Accent, "colour color palette", value = PALETTES[ZTheme.palette].name) {
                shut(); themeOpen = true
            })
            add(Entry(LOOK, "Dark theme", "", Icons.Rounded.DarkMode, ZColor.Accent, "night light", on = ZTheme.dark) {
                ZTheme.dark = !ZTheme.dark
                context.getSharedPreferences("zelu_theme", android.content.Context.MODE_PRIVATE).edit().putBoolean("dark", ZTheme.dark).apply()
            })
            add(Entry(LOOK, "Open Zelu on", "", Icons.Rounded.Home, ZColor.Muted, "start screen first page",
                value = START_PAGES.firstOrNull { it.first == startNow }?.second ?: "Home") { startPick = true })

            add(Entry(HELP, "How Zelu works", "Reading messages, dues, accounts, backup", Icons.Rounded.Lightbulb, ZColor.Accent,
                "guide faq") { shut(); helpOpen = true })
            add(Entry(HELP, "Report a problem", "Email " + Storage.reportEmail(context), Icons.Rounded.Email, ZColor.Accent,
                "bug feedback contact support") { shut(); reportOpen = true })
            add(Entry(HELP, "Privacy policy", "What Zelu reads and keeps", Icons.Rounded.Lock, ZColor.Income, "data") { shut(); privacyOpen = true })
            add(Entry(HELP, "Keep Zelu running", "Stop battery saver blocking new messages", Icons.Rounded.BatteryChargingFull, ZColor.Accent,
                "battery background stopped not working") { shut(); Notifier.openBatterySettings(context) })
            add(Entry(HELP, "Report address", "", Icons.Rounded.Email, ZColor.Muted, "email", more = true, value = Storage.reportEmail(context)) {
                shut(); emailOpen = true
            })
            if (!Demo.on) add(Entry(HELP, "Show sample data", "Made-up payments for screenshots and demos. Yours stay safe",
                Icons.Rounded.Visibility, ZColor.Muted, "demo", more = true) {
                shut(); Demo.set(context, true)
                Toast.makeText(context, "Showing sample data. Menu, Help to go back", Toast.LENGTH_LONG).show()
            })
        }

        // Tapping a row: remember it for the shortcuts; a switch can be undone from the bar at the bottom.
        fun runEntry(e: Entry) {
            MenuUse.note(context, e.title)
            if (e.on != null) undo = e.title to !e.on
            e.act()
        }
        val fixes = all.filter { it.section == ATTENTION }
        val page = menuSection
        val tokens = MenuSearch.tokens(find)

        Box(Modifier.fillMaxSize().background(ZColor.Paper)) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 14.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (page != null && tokens.isEmpty()) {
                    Box(Modifier.size(44.dp).clip(CircleShape).clickable { menuSection = null }.buttonLabel("Back to menu"), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.ChevronLeft, null, tint = ZColor.Ink)
                    }
                } else Spacer(Modifier.width(12.dp))
                Text(if (page != null && tokens.isEmpty()) page else "Menu", color = ZColor.Ink, fontSize = 24.sp,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Box(Modifier.size(44.dp).clip(CircleShape).clickable { shut() }.buttonLabel("Close menu"), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Close, null, tint = ZColor.Ink)
                }
            }
            OutlinedTextField(
                value = find, onValueChange = { find = it }, singleLine = true,
                placeholder = { Text("Search or ask: \"remind me\", \"pin\"…") },
                leadingIcon = { Icon(Icons.Rounded.Search, null, tint = ZColor.Muted) },
                trailingIcon = if (find.isEmpty()) null else ({
                    Box(Modifier.size(40.dp).clip(CircleShape).clickable { find = "" }.buttonLabel("Clear search"), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Close, null, tint = ZColor.Muted)
                    }
                }),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            )
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                if (tokens.isNotEmpty()) {
                    // Rows matching every word first; if none do, the ones matching most of them.
                    val scored = all.map { e -> e to MenuSearch.score(tokens, e.section + " " + e.title + " " + e.sub + " " + e.words) }
                    val hits = scored.filter { it.second == tokens.size }.map { it.first }
                        .ifEmpty { scored.filter { it.second > 0 }.sortedByDescending { it.second }.map { it.first } }
                    Spacer(Modifier.height(8.dp))
                    if (hits.isEmpty()) MutedText("Nothing called that. Try another word, or look under Help.", 14)
                    else MenuGroup(null, hits, showSection = true, onRun = { runEntry(it) })
                } else if (page != null) {
                    // One topic's own short page; rarely used rows wait under "More options".
                    val inSection = all.filter { it.section == page }
                    val extra = inSection.filter { it.more }
                    val open = page in opened
                    Spacer(Modifier.height(4.dp))
                    MenuGroup(null, inSection.filter { !it.more || open }, moreOpen = if (extra.isEmpty()) null else open,
                        onMore = { opened = if (open) opened - page else opened + page }, onRun = { runEntry(it) })
                } else {
                    Spacer(Modifier.height(6.dp))
                    // What needs fixing, each with its own button; or one calm line saying all is well.
                    if (fixes.isNotEmpty()) {
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(ZColor.Card)
                            .border(1.dp, ZColor.Warn.copy(alpha = 0.45f), RoundedCornerShape(18.dp)).padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Text("FIX IN ONE TAP · ${fixes.size}", color = ZColor.Warn, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            fixes.forEach { e ->
                                Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(e.title, color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                    Spacer(Modifier.width(10.dp))
                                    SmallButton(e.button ?: "Fix", filled = true) { runEntry(e) }
                                }
                            }
                        }
                    } else {
                        val facts = listOfNotNull(
                            if (lastRead > 0) "Checked " + agoText(lastRead) else null,
                            if (lastBackup > 0) "Backed up " + agoText(lastBackup) else "No backup yet",
                            if (spendOn || duesOn) "Alerts on" else "Alerts off",
                        )
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(ZColor.IncomeSoft).padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.CheckCircle, null, tint = ZColor.Income, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("All good", color = ZColor.Income, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text(facts.joinToString(" · "), color = ZColor.InkSoft, fontSize = 12.sp)
                            }
                        }
                    }

                    // One suggestion at a time, worked out from your own data. "Not now" hides it for good.
                    val today = Dates.today()
                    val usual = (1..3).map { i ->
                        val m = Dates.shiftMonth(Dates.todayMonth(), -i)
                        rows.filter { it.kind == "expense" && it.date.startsWith(m) }.sumOf { it.amount }
                    }.filter { it > 0 }.let { if (it.isEmpty()) 0.0 else it.average() }
                    val suggestions = buildList {
                        if (monthlyBudget <= 0 && usual > 0) {
                            val pick = Math.ceil(usual / 1000.0) * 1000.0
                            add(Suggestion("budget", "Set a monthly budget of " + money(pick) + "?",
                                "You usually spend " + money(usual) + " a month. Home then shows what you can spend each day.",
                                "Set " + money(pick)) {
                                scope.launch {
                                    withContext(Dispatchers.IO) { Storage.saveMonthlyBudget(context, pick) }
                                    monthlyBudget = pick
                                }
                                Toast.makeText(context, "Budget set to " + money(pick) + " a month", Toast.LENGTH_SHORT).show()
                            })
                        }
                        if (!duesOn && reminders.any { r -> !(reminderState[r.id] ?: r.done) && r.dueDate >= today }) {
                            add(Suggestion("dues", "Get reminded before bills are due?", "You have bills coming up. Zelu can remind you a few days before.",
                                "Turn on") { NotifyPrefs.setDuesOn(context, true); tick++ })
                        }
                        if (hasPw && !autoBackup) {
                            add(Suggestion("autobackup", "Back up every week by itself?", "A locked copy goes to Downloads/Zelu each week; the newest 4 are kept.",
                                "Turn on") { autoBackup = true; Backup.setAutoOn(context, true) })
                        }
                        val km = context.getSystemService(android.content.Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
                        if (!lockOn && rows.size >= 20 && km.isDeviceSecure) {
                            add(Suggestion("lock", "Lock Zelu with your phone lock?", "Your balances show as soon as the app opens.",
                                "Turn on") {
                                lockOn = true
                                context.getSharedPreferences("zelu_security", android.content.Context.MODE_PRIVATE).edit().putBoolean("lock", true).apply()
                            })
                        }
                    }
                    suggestions.firstOrNull { !MenuUse.dismissed(context, it.id) }?.let { sg ->
                        Spacer(Modifier.height(10.dp))
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(ZColor.OrangeSoft).padding(14.dp)) {
                            Text("SUGGESTED FOR YOU", color = ZColor.Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(sg.title, color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SmallButton(sg.button, filled = true) { MenuUse.dismiss(context, sg.id); sg.act(); tick++ }
                                SmallButton("Not now", filled = false) { MenuUse.dismiss(context, sg.id); tick++ }
                            }
                        }
                    }

                    // Four shortcuts: what you use most here. A backup that's overdue moves to the front.
                    val pool = all.filter { it.section != ATTENTION && !it.more && it.title !in setOf("Remind me", "Daily summary time", "Weekly summary time") }
                    val used = pool.map { it to MenuUse.count(context, it.title) }.filter { it.second > 0 }
                        .sortedByDescending { it.second }.map { it.first }
                    val defaults = listOf("Check for new payments", "Back up now", "Dark theme", "Theme colours")
                        .mapNotNull { t -> pool.firstOrNull { it.title == t } }
                    val backupDue = hasPw && (lastBackup == 0L || System.currentTimeMillis() - lastBackup > 7L * 86_400_000L)
                    val shortcuts = ((if (backupDue) pool.filter { it.title == "Back up now" } else emptyList()) + used + defaults)
                        .distinctBy { it.title }.take(4)
                    Text("YOUR SHORTCUTS", color = ZColor.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 6.dp, top = 16.dp, bottom = 8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        shortcuts.forEach { e ->
                            val lit = e.on == true
                            // Same size for every tile: a bright icon on a round tile, and a one-line name.
                            val tint = menuTint(e)
                            Column(Modifier.weight(1f).height(88.dp).clip(RoundedCornerShape(16.dp))
                                .background(if (lit) tint.copy(alpha = if (ZTheme.dark) 0.22f else 0.14f) else ZColor.Card)
                                .border(if (lit) 1.5.dp else 1.dp, if (lit) tint else ZColor.Line, RoundedCornerShape(16.dp))
                                .clickable { runEntry(e) }.padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Box(Modifier.size(38.dp).clip(CircleShape).background(ZColor.Tile), contentAlignment = Alignment.Center) {
                                    Icon(e.icon, null, tint = tint, modifier = Modifier.size(21.dp))
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(shortName(e), color = ZColor.Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                    maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            }
                        }
                    }

                    // The seven topics, each with how it stands right now.
                    val alertsOn = listOf(spendOn, dailyOn, weeklyOn, duesOn, recapOn).count { it }
                    val topics = listOf(
                        Triple(MONEY, Icons.Rounded.AccountBalanceWallet, (if (monthlyBudget > 0) money(monthlyBudget) + " a month" else "No budget") to false),
                        Triple(ACCOUNTS, Icons.Rounded.AccountBalance, (if (v.accounts.isNotEmpty()) plural(v.accounts.size, "account", "accounts") else "") to false),
                        Triple(MESSAGES, Icons.Rounded.Receipt, when {
                            missedCount > 0 -> "$missedCount to review" to true
                            stale -> "Check now" to true
                            else -> "Up to date" to false
                        }),
                        Triple(ALERTS, Icons.Rounded.Notifications, (if (notifBlocked) "Blocked" else "$alertsOn on") to notifBlocked),
                        Triple(BACKUP, Icons.Rounded.Lock, when {
                            !hasPw -> "No password" to true
                            lastBackup == 0L -> "Not backed up" to true
                            System.currentTimeMillis() - lastBackup > 8L * 86_400_000L -> agoText(lastBackup) to true
                            else -> "Safe" to false
                        }),
                        Triple(LOOK, Icons.Rounded.Palette, (PALETTES[ZTheme.palette].name + if (ZTheme.dark) " · dark" else "") to false),
                        Triple(HELP, Icons.Rounded.Lightbulb, "" to false),
                    )
                    Spacer(Modifier.height(16.dp))
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(ZColor.Card).border(1.dp, ZColor.Line, RoundedCornerShape(18.dp))) {
                        topics.forEachIndexed { i, (name, icon, status) ->
                            if (i > 0) RowDivider()
                            Row(Modifier.fillMaxWidth().clickable { menuSection = name }.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(38.dp).clip(CircleShape).background(ZColor.Tile), contentAlignment = Alignment.Center) {
                                    Icon(icon, null, tint = iconTint(icon), modifier = Modifier.size(21.dp))
                                }
                                Spacer(Modifier.width(14.dp))
                                Text(name, color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                if (status.first.isNotEmpty()) Text(status.first, color = if (status.second) ZColor.Warn else ZColor.Muted, fontSize = 12.sp,
                                    fontWeight = if (status.second) FontWeight.SemiBold else FontWeight.Normal)
                                Icon(Icons.Rounded.ChevronRight, null, tint = ZColor.Muted, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
                val version = try { context.packageManager.getPackageInfo(context.packageName, 0).versionName } catch (_: Exception) { null }
                if (version != null) Text("Zelu $version · nothing leaves your phone", color = ZColor.Muted, fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 18.dp))
            }
        }

            // A switch just changed: Undo instead of "Are you sure?".
            undo?.let { (title, nowOn) ->
                LaunchedEffect(undo) { kotlinx.coroutines.delay(4000); undo = null }
                Row(Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(ZColor.Ink).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(title + if (nowOn) " turned on" else " turned off", color = ZColor.Paper, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text("UNDO", color = ZColor.AmberInk, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable {
                            all.firstOrNull { it.title == title && it.on != null }?.act()
                            undo = null
                        }.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        }

        if (leadPick) Dialog(onDismissRequest = { leadPick = false }) {
            val cur = NotifyPrefs.leadIndex(context)
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(vertical = 10.dp)) {
                Text("Remind me", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp))
                NotifyPrefs.LEAD_PRESETS.forEachIndexed { i, (label, _) ->
                    Row(Modifier.fillMaxWidth().choice(i == cur) { NotifyPrefs.setLeadIndex(context, i); leadPick = false; tick++ }
                        .padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(label, color = ZColor.Ink, fontSize = 15.sp, fontWeight = if (i == cur) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f))
                        if (i == cur) Text("✓", color = ZColor.Accent, fontWeight = FontWeight.Bold, modifier = Unspoken)
                    }
                }
            }
        }

        if (startPick) Dialog(onDismissRequest = { startPick = false }) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(vertical = 10.dp)) {
                Text("Open Zelu on", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp))
                START_PAGES.forEach { (key, label) ->
                    Row(Modifier.fillMaxWidth().choice(key == startNow) {
                        ui.edit().putString("start", key).apply(); startPick = false; tick++
                    }.padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(label, color = ZColor.Ink, fontSize = 15.sp, fontWeight = if (key == startNow) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f))
                        if (key == startNow) Text("✓", color = ZColor.Accent, fontWeight = FontWeight.Bold, modifier = Unspoken)
                    }
                }
            }
        }
    }
}

// A heading and its rows on one card. moreOpen is null when the group has nothing under "More options".
@Composable
private fun MenuGroup(title: String?, items: List<Entry>, showSection: Boolean = false, moreOpen: Boolean? = null, onMore: () -> Unit = {},
                      onRun: (Entry) -> Unit) {
    if (title != null) Text(title.uppercase(), color = if (title == ATTENTION) ZColor.Warn else ZColor.Muted, fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp, top = 18.dp, bottom = 8.dp))
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(ZColor.Card).border(1.dp, ZColor.Line, RoundedCornerShape(18.dp))) {
        items.forEachIndexed { i, e ->
            if (i > 0) RowDivider()
            EntryRow(e, showSection, onRun)
        }
        if (moreOpen != null) {
            RowDivider()
            Row(Modifier.fillMaxWidth().clickable { onMore() }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (moreOpen) "Fewer options" else "More options", color = ZColor.Accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f))
                Icon(if (moreOpen) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown, null, tint = ZColor.Accent)
            }
        }
    }
}

@Composable
private fun RowDivider() {
    Box(Modifier.padding(start = 62.dp).fillMaxWidth().height(1.dp).background(ZColor.Line))
}

@Composable
private fun EntryRow(e: Entry, showSection: Boolean, onRun: (Entry) -> Unit) {
    val on = e.on
    Row(
        Modifier.fillMaxWidth()
            .then(if (on != null) Modifier.toggleable(value = on, role = Role.Switch) { onRun(e) } else Modifier.clickable { onRun(e) })
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(ZColor.Tile), contentAlignment = Alignment.Center) {
            Icon(e.icon, null, tint = menuTint(e), modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            if (showSection) Text(e.section.uppercase(), color = ZColor.Muted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            Text(e.title, color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.width(8.dp))
        if (e.value != null) {
            Text(e.value, color = if (e.warn) ZColor.Warn else ZColor.Muted, fontSize = 13.sp,
                fontWeight = if (e.warn) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1)
            Spacer(Modifier.width(6.dp))
        }
        if (on != null) Switch(checked = on, onCheckedChange = null, colors = SwitchDefaults.colors(
            checkedThumbColor = ZColor.AccentInk, checkedTrackColor = ZColor.Accent, checkedBorderColor = ZColor.Accent,
            uncheckedThumbColor = ZColor.Muted, uncheckedTrackColor = ZColor.Paper, uncheckedBorderColor = ZColor.Line,
        ))
        else Icon(Icons.Rounded.ChevronRight, null, tint = ZColor.Muted, modifier = Modifier.size(20.dp))
    }
}

// Short names for the shortcut tiles.
private fun shortName(e: Entry): String = when (e.title) {
    "Check for new payments" -> "Check now"
    "Back up now" -> "Back up"
    "Dark theme" -> if (e.on == true) "Dark on" else "Dark"
    "Theme colours" -> "Colours"
    "Budgets by category" -> "Budgets"
    "Your accounts" -> "Accounts"
    "Export spending (CSV)" -> "Export"
    "Missed messages" -> "Missed"
    "Banks Zelu doesn't know" -> "New bank"
    "Export to Excel (CSV)" -> "Export"
    "Read older messages" -> "Older"
    "Payment alerts" -> "Alerts"
    else -> e.title
}

@Composable
private fun SmallButton(label: String, filled: Boolean, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(12.dp)).background(if (filled) ZColor.Ink else ZColor.Card)
        .border(1.dp, if (filled) ZColor.Ink else ZColor.Line, RoundedCornerShape(12.dp))
        .clickable { onClick() }.padding(horizontal = 12.dp, vertical = 7.dp)) {
        Text(label, color = if (filled) ZColor.Paper else ZColor.Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

// The menu's icons in the same bright colours as the rest of the app; a warning keeps its red.
private fun menuTint(e: Entry): Color = if (e.tint == ZColor.Warn) ZColor.Warn else iconTint(e.icon)

private fun iconTint(icon: androidx.compose.ui.graphics.vector.ImageVector): Color {
    val (light, dark) = when (icon) {
        Icons.Rounded.AccountBalanceWallet -> 0xFF8B5CF6 to 0xFFA78BFA
        Icons.Rounded.AccountBalance -> 0xFF3B82F6 to 0xFF60A5FA
        Icons.Rounded.Receipt -> 0xFF0D9488 to 0xFF2DD4BF
        Icons.Rounded.Notifications -> 0xFFEAB308 to 0xFFFACC15
        Icons.Rounded.Lock -> 0xFF22B455 to 0xFF3DD171
        Icons.Rounded.Palette -> 0xFFEC4899 to 0xFFF472B6
        Icons.Rounded.Lightbulb -> 0xFFF59E0B to 0xFFFBBF24
        Icons.Rounded.BarChart -> 0xFF06B6D4 to 0xFF22D3EE
        Icons.Rounded.Category -> 0xFF8B5CF6 to 0xFFA78BFA
        Icons.Rounded.Savings, Icons.Rounded.TrendingUp -> 0xFF22B455 to 0xFF3DD171
        Icons.Rounded.Restore -> 0xFF3B82F6 to 0xFF60A5FA
        Icons.Rounded.CalendarMonth -> 0xFFFF6B1A to 0xFFFF8A3D
        Icons.Rounded.CloudUpload -> 0xFF0EA5E9 to 0xFF7DD3FC
        Icons.Rounded.FileDownload -> 0xFF0D9488 to 0xFF2DD4BF
        Icons.Rounded.DarkMode -> 0xFF6366F1 to 0xFF818CF8
        Icons.Rounded.Home -> 0xFFFF6B1A to 0xFFFF8A3D
        Icons.Rounded.Email -> 0xFF3B82F6 to 0xFF60A5FA
        Icons.Rounded.Warning -> 0xFFE5484D to 0xFFFF6369
        else -> 0xFF64748B to 0xFF94A3B8
    }
    return Color(if (ZTheme.dark) dark else light)
}
