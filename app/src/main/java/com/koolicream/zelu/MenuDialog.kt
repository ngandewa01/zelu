package com.koolicream.zelu

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Warning
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// The menu: theme, money, accounts, messages, notifications, backup and help pages.

@Composable
internal fun MenuDialog(s: AppState, v: Filtered) {
    with(s) { with(v) {
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
                        Row(Modifier.fillMaxWidth().clickable { menuPage = "" }.buttonLabel("Back to menu").padding(horizontal = 20.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("‹", color = ZColor.Ink, fontSize = 26.sp, modifier = Unspoken)
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
                                MenuRow("Read everything again", "Reads all messages afresh. Your edits stay.", Icons.Rounded.Warning, ZColor.Muted) { shut(); fullConfirm = true }
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
                                MenuRow("Restore data", "Load a backup file", Icons.Rounded.Restore, ZColor.Muted) { shut(); restoreLauncher?.launch(arrayOf("*/*")) }
                                MenuRow("Weekly backup", if (Backup.hasPassword(context)) "Saved to Downloads/Zelu every week; the newest 4 are kept" else "Needs a backup password first", Icons.Rounded.CloudUpload, if (autoBackup) ZColor.Income else ZColor.Muted,
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
    } }
}
