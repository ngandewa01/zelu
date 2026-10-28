package com.koolicream.zelu

import android.widget.Toast
import androidx.compose.foundation.background
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

// The menu: one page with a search box. What needs doing (missed messages, alerts off, no backup password) comes first,
// then everything else under plain headings. Every row does its job directly; switches change in place.
@Composable
internal fun MenuDialog(s: AppState, v: Filtered) {
    if (s.menuOpen) Dialog(onDismissRequest = { s.menuOpen = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        MenuPage(s, v)
    }
}

// One row of the menu. on is set for a switch; more rows stay under "More options" until opened or searched for.
private class Entry(
    val section: String, val title: String, val sub: String, val icon: ImageVector, val tint: Color,
    val words: String = "", val on: Boolean? = null, val more: Boolean = false, val act: () -> Unit,
)

private const val ATTENTION = "Needs attention"
private const val MONEY = "Money"
private const val ACCOUNTS = "Accounts"
private const val MESSAGES = "Bank messages"
private const val ALERTS = "Alerts"
private const val BACKUP = "Backup and privacy"
private const val LOOK = "Look"
private const val HELP = "Help"
private val SECTIONS = listOf(ATTENTION, MONEY, ACCOUNTS, MESSAGES, ALERTS, BACKUP, LOOK, HELP)
private val START_PAGES = listOf("ledger" to "Home", "spend" to "Spend", "payees" to "Payees", "dues" to "Dues")

@Composable
private fun MenuPage(s: AppState, v: Filtered) {
    with(s) {
        var find by remember { mutableStateOf("") }
        var tick by remember { mutableStateOf(0) }                 // a setting changed here: read them all again
        var opened by remember { mutableStateOf(setOf<String>()) }  // sections showing their "More options"
        var startPick by remember { mutableStateOf(false) }
        fun shut() { menuOpen = false }
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
                "Bank messages Zelu couldn't read. Add them or teach Zelu.", Icons.Rounded.Receipt, ZColor.Warn) { openMissed() })
            if (notifBlocked) add(Entry(ATTENTION, "Alerts are off", "Turn on notifications for payment and bill alerts",
                Icons.Rounded.Notifications, ZColor.Warn) { shut(); fixNotif() })
            if (!hasPw && rows.isNotEmpty()) add(Entry(ATTENTION, "Set a backup password", "Backups wait until you set one",
                Icons.Rounded.Lock, ZColor.Warn) { shut(); backupAfterPw = false; backupPwOpen = true })
            if (Demo.on) add(Entry(ATTENTION, "Sample data is on", "Made-up payments. Tap to go back to yours", Icons.Rounded.Visibility, ZColor.Warn) {
                shut(); Demo.set(context, false)
                Toast.makeText(context, "Back to your own payments", Toast.LENGTH_SHORT).show()
            })

            add(Entry(MONEY, "Monthly budget", if (monthlyBudget > 0) money(monthlyBudget) + " a month" else "Not set",
                Icons.Rounded.AccountBalanceWallet, ZColor.Accent, "limit spending") { shut(); budgetDlg = true })
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
            if (removed > 0) add(Entry(ACCOUNTS, "Bring back removed accounts", "$removed removed or hidden", Icons.Rounded.Restore,
                ZColor.Muted, "hidden restore declined") {
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
                Icons.Rounded.Restore, if (stale) ZColor.Warn else ZColor.Accent, "rescan refresh sync update") { shut(); rescan(notify = true) })
            add(Entry(MESSAGES, "Missed messages",
                if (missedCount > 0) "$missedCount waiting · add by hand or teach Zelu" else "Money messages Zelu couldn't read",
                Icons.Rounded.Receipt, ZColor.Warn, "unread sms add manually teach") { openMissed() })
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
            if (dailyOn) add(Entry(ALERTS, "Daily summary time", NotifyPrefs.timeText(NotifyPrefs.dailyAt(context)), Icons.Rounded.CalendarMonth,
                ZColor.Muted, "notification") {
                val at = NotifyPrefs.dailyAt(context)
                android.app.TimePickerDialog(context, { _, h, m -> NotifyPrefs.setDailyAt(context, h * 60 + m); tick++ }, at / 60, at % 60, false).show()
            })
            add(Entry(ALERTS, "Weekly summary", "Sundays: the week against the one before", Icons.Rounded.BarChart, ZColor.Accent,
                "notification", on = weeklyOn) { NotifyPrefs.setWeeklyOn(context, !weeklyOn); tick++ })
            if (weeklyOn) add(Entry(ALERTS, "Weekly summary time", "Sundays at " + NotifyPrefs.timeText(NotifyPrefs.weeklyAt(context)),
                Icons.Rounded.CalendarMonth, ZColor.Muted, "notification") {
                val at = NotifyPrefs.weeklyAt(context)
                android.app.TimePickerDialog(context, { _, h, m -> NotifyPrefs.setWeeklyAt(context, h * 60 + m); tick++ }, at / 60, at % 60, false).show()
            })
            add(Entry(ALERTS, "Bill reminders", "Before a bill or card is due, around 9 AM", Icons.Rounded.Receipt, ZColor.Accent,
                "dues notification", on = duesOn) { NotifyPrefs.setDuesOn(context, !duesOn); tick++ })
            if (duesOn) add(Entry(ALERTS, "Remind me", NotifyPrefs.LEAD_PRESETS[NotifyPrefs.leadIndex(context)].first + " · tap to change",
                Icons.Rounded.CalendarMonth, ZColor.Muted, "dues days before") {
                NotifyPrefs.setLeadIndex(context, (NotifyPrefs.leadIndex(context) + 1) % NotifyPrefs.LEAD_PRESETS.size); tick++
            })
            add(Entry(ALERTS, "Monthly recap", "A note in the first days of a new month", Icons.Rounded.TrendingUp, ZColor.Accent,
                "notification", on = recapOn) { NotifyPrefs.setRecapOn(context, !recapOn); tick++ })
            add(Entry(ALERTS, "Test alert", if (notifBlocked) "Alerts are off · tap to turn on" else "Send a sample payment alert",
                Icons.Rounded.Notifications, if (notifBlocked) ZColor.Warn else ZColor.Muted, "notification try", more = true) {
                shut()
                if (notifBlocked) fixNotif() else Notifier.notifyTest(context)
            })

            add(Entry(BACKUP, "Back up now",
                if (lastBackup > 0) "Last saved " + java.text.SimpleDateFormat("d MMM", java.util.Locale.US).format(java.util.Date(lastBackup)) + " · locked with your password"
                else "Save everything to Downloads/Zelu, locked with a password",
                Icons.Rounded.CloudUpload, ZColor.Income, "save copy") {
                shut()
                if (hasPw) backupNow() else { backupAfterPw = true; backupPwOpen = true }
            })
            add(Entry(BACKUP, "Weekly backup",
                if (hasPw) "Saved to Downloads/Zelu every week; the newest 4 are kept" else "Needs a backup password first",
                Icons.Rounded.CloudUpload, ZColor.Income, "automatic", on = autoBackup) { autoBackup = !autoBackup; Backup.setAutoOn(context, autoBackup) })
            add(Entry(BACKUP, "Backup password", if (hasPw) "Set · needed to restore on a new phone" else "Not set · backups wait until you set one",
                Icons.Rounded.Lock, if (hasPw) ZColor.Income else ZColor.Warn) { shut(); backupAfterPw = false; backupPwOpen = true })
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
            add(Entry(BACKUP, "Export spending (CSV)", "What's on screen now, to Downloads/Zelu", Icons.Rounded.FileDownload, ZColor.Muted,
                "csv excel spreadsheet download") {
                shut()
                val ok = Exporter.exportCsv(context, v.shown, "zelu-${Dates.today()}.csv")
                Toast.makeText(context, if (ok) "Saved to Downloads/Zelu" else "Export needs Android 10+", Toast.LENGTH_SHORT).show()
            })
            add(Entry(BACKUP, "Erase everything", "Delete all Zelu data from this phone", Icons.Rounded.Warning, ZColor.Warn,
                "delete wipe reset") { shut(); eraseOpen = true })

            add(Entry(LOOK, "Theme colours", PALETTES[ZTheme.palette].name, Icons.Rounded.Palette, ZColor.Accent, "colour color palette") {
                shut(); themeOpen = true
            })
            add(Entry(LOOK, "Dark theme", "", Icons.Rounded.DarkMode, ZColor.Accent, "night light", on = ZTheme.dark) {
                ZTheme.dark = !ZTheme.dark
                context.getSharedPreferences("zelu_theme", android.content.Context.MODE_PRIVATE).edit().putBoolean("dark", ZTheme.dark).apply()
            })
            add(Entry(LOOK, "Open Zelu on", START_PAGES.firstOrNull { it.first == startNow }?.second ?: "Home", Icons.Rounded.Home,
                ZColor.Muted, "start screen first page") { startPick = true })

            add(Entry(HELP, "How Zelu works", "Reading messages, dues, accounts, backup", Icons.Rounded.Lightbulb, ZColor.Accent,
                "guide faq") { shut(); helpOpen = true })
            add(Entry(HELP, "Report a problem", "Email " + Storage.reportEmail(context), Icons.Rounded.Email, ZColor.Accent,
                "bug feedback contact support") { shut(); reportOpen = true })
            add(Entry(HELP, "Privacy policy", "What Zelu reads and keeps", Icons.Rounded.Lock, ZColor.Income, "data") { shut(); privacyOpen = true })
            add(Entry(HELP, "Keep Zelu running", "Stop battery saver blocking new messages", Icons.Rounded.BatteryChargingFull, ZColor.Accent,
                "battery background stopped not working") { shut(); Notifier.openBatterySettings(context) })
            add(Entry(HELP, "Report address", Storage.reportEmail(context), Icons.Rounded.Email, ZColor.Muted, "email", more = true) {
                shut(); emailOpen = true
            })
            if (!Demo.on) add(Entry(HELP, "Show sample data", "Made-up payments for screenshots and demos. Yours stay safe",
                Icons.Rounded.Visibility, ZColor.Muted, "demo", more = true) {
                shut(); Demo.set(context, true)
                Toast.makeText(context, "Showing sample data. Menu, Help to go back", Toast.LENGTH_LONG).show()
            })
        }

        Column(Modifier.fillMaxSize().background(ZColor.Paper)) {
            Row(Modifier.fillMaxWidth().padding(start = 22.dp, end = 10.dp, top = 14.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Menu", color = ZColor.Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Box(Modifier.size(44.dp).clip(CircleShape).clickable { shut() }.buttonLabel("Close menu"), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Close, null, tint = ZColor.Ink)
                }
            }
            OutlinedTextField(
                value = find, onValueChange = { find = it }, singleLine = true,
                placeholder = { Text("Search: budget, backup, missed…") },
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
                val words = find.trim().lowercase().split(' ').filter { it.isNotEmpty() }
                if (words.isNotEmpty()) {
                    val hits = all.filter { e ->
                        val hay = (e.section + " " + e.title + " " + e.sub + " " + e.words).lowercase()
                        words.all { hay.contains(it) }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (hits.isEmpty()) MutedText("Nothing called that. Try another word, or look under Help.", 14)
                    else MenuGroup(null, hits, showSection = true)
                } else for (section in SECTIONS) {
                    val inSection = all.filter { it.section == section }
                    if (inSection.isEmpty()) continue
                    val extra = inSection.filter { it.more }
                    val open = section in opened
                    MenuGroup(section, inSection.filter { !it.more || open }, moreOpen = if (extra.isEmpty()) null else open) {
                        opened = if (open) opened - section else opened + section
                    }
                }
                val version = try { context.packageManager.getPackageInfo(context.packageName, 0).versionName } catch (_: Exception) { null }
                if (version != null) Text("Zelu $version", color = ZColor.Muted, fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 18.dp))
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
private fun MenuGroup(title: String?, items: List<Entry>, showSection: Boolean = false, moreOpen: Boolean? = null, onMore: () -> Unit = {}) {
    if (title != null) Text(title.uppercase(), color = if (title == ATTENTION) ZColor.Warn else ZColor.Muted, fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp, top = 18.dp, bottom = 8.dp))
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(ZColor.Card).border(1.dp, ZColor.Line, RoundedCornerShape(18.dp))) {
        items.forEachIndexed { i, e ->
            if (i > 0) RowDivider()
            EntryRow(e, showSection)
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
private fun EntryRow(e: Entry, showSection: Boolean) {
    val on = e.on
    Row(
        Modifier.fillMaxWidth()
            .then(if (on != null) Modifier.toggleable(value = on, role = Role.Switch) { e.act() } else Modifier.clickable { e.act() })
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(e.tint.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
            Icon(e.icon, null, tint = e.tint, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            if (showSection) Text(e.section.uppercase(), color = ZColor.Muted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            Text(e.title, color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (e.sub.isNotEmpty()) Text(e.sub, color = ZColor.Muted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        if (on != null) Switch(checked = on, onCheckedChange = null, colors = SwitchDefaults.colors(
            checkedThumbColor = ZColor.AccentInk, checkedTrackColor = ZColor.Accent, checkedBorderColor = ZColor.Accent,
            uncheckedThumbColor = ZColor.Muted, uncheckedTrackColor = ZColor.Paper, uncheckedBorderColor = ZColor.Line,
        ))
        else Icon(Icons.Rounded.ChevronRight, null, tint = ZColor.Muted, modifier = Modifier.size(20.dp))
    }
}
