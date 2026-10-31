package com.koolicream.zelu

import androidx.compose.ui.res.stringResource

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

@Composable internal fun RemindersView(
    reminders: List<Reminder>, rows: List<Row>, state: Map<String, Boolean>, onMark: (String, Boolean) -> Unit,
    onAdd: (String, Double, String, Boolean) -> Unit = { _, _, _, _ -> }, onRemove: (String) -> Unit = {},
    adding: Boolean = false, setAdding: (Boolean) -> Unit = {},
    tab: Int = 0, onTab: (Int) -> Unit = {}, people: @Composable () -> Unit = {},
) {
    var detail by remember { mutableStateOf<Reminder?>(null) }
    val ctxAdd = LocalContext.current
    var mandates by remember { mutableStateOf<List<Mandate>>(emptyList()) }
    var mandateOpen by remember { mutableStateOf<Mandate?>(null) }
    LaunchedEffect(reminders) { mandates = withContext(Dispatchers.IO) { Storage.loadMandates(ctxAdd) } }
    val today = Dates.today()
    val curMonth = Dates.todayMonth()
    // Auto-settle: a real debit to the SAME merchant for the SAME amount within ±5 days of
    // the due date. Each debit is used at most once, so one payment can't tick two reminders.
    val autoSet = remember(reminders, rows) { autoSettledReminders(reminders, rows) }
    // An explicit paid/unpaid tap always wins; otherwise a previous-month due or a debit-matched
    // one is assumed paid.
    fun settled(r: Reminder) = state[r.id] ?: (r.dueDate.take(7) < curMonth || r.id in autoSet || r.done)

    val pending = reminders.filter { !settled(it) }.sortedBy { it.dueDate }
    val paid = reminders.filter { settled(it) }.sortedByDescending { it.dueDate }
    // Dues page tabs: what is coming (with the autopay set-ups under it), what is paid, and people you share money with.
    val duesTab = tab.coerceIn(0, 2)
    var showOlderPaid by remember { mutableStateOf(false) }
    val paidNow = paid.filter { it.dueDate.take(7) >= curMonth }
    val paidOlder = paid.filter { it.dueDate.take(7) < curMonth }

    Spacer(Modifier.height(4.dp))
    // Adding a bill is the round + button above the nav bar (MainActivity), so the title stands alone.
    Text(stringResource(R.string.dues_reminders), color = ZColor.Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold, maxLines = 2)

    Spacer(Modifier.height(12.dp))
    TabRowWrap {
        IconTab(Icons.Rounded.Schedule, stringResource(R.string.upcoming_tab), 0xFFEAB308, 0xFFFACC15, duesTab == 0, pending.size, modifier = Modifier.weight(1f)) { onTab(0) }
        IconTab(Icons.Rounded.TaskAlt, stringResource(R.string.paid_2), 0xFF22B455, 0xFF3DD171, duesTab == 1, modifier = Modifier.weight(1f)) { onTab(1) }
        IconTab(Icons.Rounded.Group, stringResource(R.string.people_tab), 0xFF3B82F6, 0xFF60A5FA, duesTab == 2, modifier = Modifier.weight(1f)) { onTab(2) }
    }

    // Summary: what is overdue, what lands this week, and where the month is heading.
    val weekEnd = Dates.shiftDays(today, 7)
    val overdueList = pending.filter { it.dueDate < today }
    val weekList = pending.filter { it.dueDate >= today && it.dueDate <= weekEnd }
    val laterList = pending.filter { it.dueDate > weekEnd }
    if (duesTab == 0 && pending.isNotEmpty()) Panel {
        run {
        // Only the totals that have something in them: a "₹0" box says nothing.
        val od = overdueList.sumOf { it.amount }
        val wk = weekList.sumOf { it.amount }
        val lt = laterList.sumOf { it.amount }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            if (od > 0) Fact(stringResource(R.string.overdue), money(od), ZColor.Warn)
            if (wk > 0) Fact(stringResource(R.string.next_7_days), money(wk))
            if (lt > 0) Fact(stringResource(R.string.later), money(lt))
        }
        // The 30-day strip shows whenever a bill falls inside it, so it never shows only empty days.
        // Today has an outline; a day with a bill is gold and opens it; swipe for later days.
        val stripEnd = Dates.shiftDays(today, 29)
        if (pending.any { it.dueDate >= today && it.dueDate <= stripEnd }) {
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (i in 0..29) {
                val d = Dates.shiftDays(today, i)
                val due = pending.filter { it.dueDate == d }
                val letter = "SMTWTFS"[Dates.parse(d).get(java.util.Calendar.DAY_OF_WEEK) - 1].toString()
                val dayN = d.substring(8, 10).toInt()
                val shape = RoundedCornerShape(10.dp)
                Column(Modifier.width(50.dp).clip(shape)
                    .background(if (due.isNotEmpty()) ZColor.AmberSoft else ZColor.Paper)
                    .then(if (due.isNotEmpty()) Modifier.border(1.dp, ZColor.AmberInk.copy(alpha = 0.5f), shape) else Modifier)
                    .then(if (i == 0) Modifier.border(2.dp, ZColor.Ink, shape) else Modifier)
                    .then(if (due.isNotEmpty()) Modifier.clickable { detail = due.first() } else Modifier)
                    .padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (i == 0) "Today" else if (dayN == 1) MONTH_ABBR[d.substring(5, 7).toInt() - 1] else letter,
                        color = if (i == 0) ZColor.Ink else ZColor.Muted, fontSize = 10.sp,
                        fontWeight = if (i == 0 || dayN == 1) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
                    Text(dayN.toString(), color = ZColor.Ink, fontSize = 15.sp,
                        fontWeight = if (i == 0) FontWeight.Bold else FontWeight.Medium)
                    // A bill day shows its amount; an empty day shows nothing, so bill days stand out.
                    Text(if (due.isNotEmpty()) "₹" + shortAmt(due.sumOf { it.amount }) else " ",
                        color = ZColor.AmberInk, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
        }
        }
    }

    if (duesTab != 0) { /* the Upcoming list is only on its own tab */ }
    else if (pending.isEmpty()) {
        Spacer(Modifier.height(10.dp))
        MutedText(stringResource(R.string.nothing_due), 13)
    } else {
        Spacer(Modifier.height(4.dp))
        OnceHint("dues_tick", "Tap ✓ when a bill is paid.")
        @Composable fun group(label: String, list: List<Reminder>, overdue: Boolean) {
            if (list.isEmpty()) return
            Spacer(Modifier.height(14.dp))
            // The total for each group is already in the summary above, so the heading is just its name.
            Text(label.uppercase(), color = if (overdue) ZColor.Warn else ZColor.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            list.forEach { r -> ReminderCard(r, overdue = overdue, onOpen = { detail = r }) { onMark(r.id, true) } }
        }
        group("Overdue", overdueList, true)
        group("This week", weekList, false)
        group("Later", laterList, false)
    }
    if (duesTab == 1) {
        Spacer(Modifier.height(14.dp))
        if (paidNow.isEmpty() && paidOlder.isEmpty()) Panel { MutedText(stringResource(R.string.nothing_paid_yet_bills_you)) }
        if (paidNow.isNotEmpty()) {
            Eyebrow(stringResource(R.string.paid_this_month))
            paidNow.forEach { r -> ReminderCard(r, overdue = false, paid = true, onOpen = { detail = r }) { onMark(r.id, false) } }
        }
        if (paidOlder.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            FoldHeading(stringResource(R.string.earlier_months), paidOlder.size, showOlderPaid) { showOlderPaid = !showOlderPaid }
            if (showOlderPaid) paidOlder.take(25).forEach { r -> ReminderCard(r, overdue = false, paid = true, onOpen = { detail = r }) { onMark(r.id, false) } }
        }
    }
    if (duesTab == 2) people()
    // Autopay set-ups sit under the bills to pay, since each is a bill that pays itself.
    if (duesTab == 0 && mandates.isNotEmpty()) {
        var autopayOpen by remember { mutableStateOf(true) }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Eyebrow(stringResource(R.string.autopay_mandates))
                InfoDot(stringResource(R.string.upi_autopay_set_up_from))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                MutedText("${mandates.count { it.status == "active" || it.status == "scheduled" }} active", 12)
                FoldArrow(autopayOpen, Modifier.clip(CircleShape).clickable { autopayOpen = !autopayOpen })
            }
        }

        Spacer(Modifier.height(6.dp))
        // Ended or paused set-ups are history: folded away under one line unless you open them.
        var showEnded by remember { mutableStateOf(false) }
        val liveMandates = mandates.filter { it.status == "active" || it.status == "scheduled" }
        val endedMandates = mandates.filter { it !in liveMandates }
        @Composable fun mandateRow(md: Mandate) {
            val live = md.status == "active" || md.status == "scheduled"
            val tone = when (md.status) { "paused" -> ZColor.Warn; "ended" -> ZColor.Muted; else -> ZColor.Income }
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(14.dp)).background(ZColor.Card)
                .border(1.dp, ZColor.Line, RoundedCornerShape(14.dp)).clickable { mandateOpen = md }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                MerchantTile(md.merchant, "Bills", 38.dp, Kind.EXPENSE)
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
                        if (inDues) Text(stringResource(R.string.in_your_dues), color = ZColor.Income, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        else Text(stringResource(R.string.add_to_dues), color = ZColor.Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 2.dp).clickable { onAdd(md.merchant, md.amount, nxt ?: Dates.today(), true) })
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (md.amount != null) Text(money(md.amount), color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(when (md.status) { "paused" -> "PAUSED"; "ended" -> "ENDED"; else -> "ACTIVE" }, color = tone, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (autopayOpen) {
            liveMandates.forEach { mandateRow(it) }
            // Ended set-ups are history: their own heading, folded until opened.
            if (endedMandates.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                FoldHeading(stringResource(R.string.ended_label), endedMandates.size, showEnded) { showEnded = !showEnded }
                if (showEnded) endedMandates.forEach { mandateRow(it) }
            }
        }
    }

    mandateOpen?.let { md ->
        Dialog(onDismissRequest = { mandateOpen = null }) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp).heightIn(max = 560.dp)) {
                Text(md.merchant, color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                MutedText(stringResource(R.string.upi_autopay) + when (md.status) { "paused" -> "paused"; "ended" -> "cancelled or expired"; else -> "active" } +
                    (md.amount?.let { " · up to " + money(it) } ?: ""), 12)
                Spacer(Modifier.height(10.dp))
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                    .clip(RoundedCornerShape(12.dp)).background(ZColor.Paper).padding(12.dp)) {
                    MutedText(md.source + " · " + prettyDate(Dates.fmtMs(md.lastMs)), 11)
                    Spacer(Modifier.height(4.dp))
                    Text(md.raw, color = ZColor.Ink, fontSize = 12.sp)
                }
                Spacer(Modifier.height(8.dp))
                MutedText(stringResource(R.string.to_stop_it_cancel_it), 11)
                Spacer(Modifier.height(10.dp))
                Box(Modifier.align(Alignment.End).clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                    .clickable { mandateOpen = null }.padding(horizontal = 22.dp, vertical = 10.dp)) {
                    Text(stringResource(R.string.close), color = ZColor.Paper, fontWeight = FontWeight.SemiBold)
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
        Dialog(onDismissRequest = { setAdding(false) }) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp)) {
                Text(stringResource(R.string.add_a_bill), color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                MutedText(stringResource(R.string.for_bills_with_no_sms), 12)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, placeholder = { Text(stringResource(R.string.what_is_it_rent)) }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = amt, onValueChange = { v -> amt = v.filter { it.isDigit() || it == '.' } }, singleLine = true,
                    placeholder = { Text(stringResource(R.string.amount)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.clip(RoundedCornerShape(20.dp)).border(1.dp, ZColor.Line, RoundedCornerShape(20.dp))
                        .clickable { pickDate(ctxAdd, due) { due = it } }.padding(horizontal = 16.dp, vertical = 9.dp)) {
                        Text(stringResource(R.string.due, prettyDate(due)), color = ZColor.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.width(10.dp))
                    Chip(stringResource(R.string.every_month), monthly, true) { monthly = !monthly }
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.cancel), color = ZColor.Muted, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { setAdding(false) }.padding(12.dp))
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink).clickable {
                        val a = amt.toDoubleOrNull() ?: 0.0
                        if (name.isNotBlank() && a > 0) { onAdd(name.trim(), a, due, monthly); setAdding(false) }
                    }.padding(horizontal = 22.dp, vertical = 10.dp)) { Text(stringResource(R.string.save), color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

@Composable internal fun ReminderDetailDialog(r: Reminder, onRemove: (() -> Unit)? = null, onDismiss: () -> Unit) {
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
                    MutedText(stringResource(R.string.due, prettyDate(r.dueDate)) + if (r.recurring) " · Monthly" else "", 12)
                }
                Spacer(Modifier.width(10.dp))
                Text(money(r.amount), color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            if (r.minAmount != null) { Spacer(Modifier.height(6.dp)); MutedText(stringResource(R.string.minimum_due, money(r.minAmount)), 12) }
            if (r.account.isNotBlank()) { Spacer(Modifier.height(4.dp)); MutedText(stringResource(R.string.account_2, r.account), 12) }
            Spacer(Modifier.height(16.dp))
            Eyebrow(stringResource(R.string.from_this_message))
            Spacer(Modifier.height(6.dp))
            Text(
                r.raw.ifBlank { "(no message text stored)" }, color = ZColor.InkSoft, fontSize = 13.sp,
                modifier = Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState()),
            )
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                if (onRemove != null) {
                    Text(stringResource(R.string.remove_bill), color = ZColor.Warn, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onRemove() }.padding(12.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Box(
                    Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                        .clickable { onDismiss() }.padding(horizontal = 22.dp, vertical = 10.dp),
                ) { Text(stringResource(R.string.close), color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

// One bill on one compact line: name with "Due 2 Oct · Monthly" under it, the amount, and the ✓ to mark it paid.
@Composable internal fun ReminderCard(r: Reminder, overdue: Boolean, paid: Boolean = false, onOpen: () -> Unit = {}, onTick: () -> Unit) {
    val name = niceName(r.counterparty?.takeIf { it.isNotBlank() } ?: r.account.ifBlank { "Bill" })
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(14.dp))
            .background(ZColor.Card).border(1.dp, ZColor.Line, RoundedCornerShape(14.dp))
            .clickable { onOpen() }.padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MerchantTile(name, "Bills", 34.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(niceName(name), color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val acct = r.account.takeIf { it.isNotBlank() && !it.equals(r.counterparty ?: "", ignoreCase = true) }
            MutedText(listOfNotNull("Due " + prettyDate(r.dueDate), if (r.recurring) "Monthly" else null, acct,
                r.minAmount?.let { "min " + money(it) }).joinToString(" · "), 12)
        }
        Spacer(Modifier.width(8.dp))
        Text(money(r.amount), color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(16.dp))
                .background(if (paid) ZColor.Income.copy(alpha = 0.15f) else Color.Transparent)
                .border(1.5.dp, if (paid) ZColor.Income else ZColor.Line, RoundedCornerShape(16.dp))
                .checkRow(paid) { onTick() }.spoken("Bill paid"),
            contentAlignment = Alignment.Center,
        ) { Text("✓", color = if (paid) ZColor.Income else ZColor.Muted, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Unspoken) }
    }
}

// Bills that look paid from the payments themselves: a debit to the same payee for the same amount within ±5 days of
// the due date, or a card bill payment landing on that card up to 25 days before. Each payment is used at most once.
internal fun autoSettledReminders(reminders: List<Reminder>, rows: List<Row>): Set<String> {
    val used = HashSet<String>()
    val matched = HashSet<String>()
    reminders.sortedBy { it.dueDate }.forEach { r ->
        val cp = r.counterparty?.lowercase()?.trim()
        if (cp.isNullOrEmpty()) return@forEach
        val window = (-5..5).map { Dates.shiftDays(r.dueDate, it) }.toSet()
        // A card bill is paid onto the card itself ("Payment received on your ICICI Credit Card XX1291"),
        // often a couple of weeks before the due date.
        val cardWindow = (-25..5).map { Dates.shiftDays(r.dueDate, it) }.toSet()
        val debit = rows.firstOrNull { row ->
            row.id !in used &&
                (row.kind == Kind.EXPENSE || row.kind == Kind.TRANSFER || row.kind == Kind.INVEST) &&
                kotlin.math.abs(row.amount - r.amount) < 1.0 && run {
                    val rc = row.counterparty?.lowercase()?.trim()
                    (window.contains(row.date) && !rc.isNullOrEmpty() && (rc == cp || rc.contains(cp) || cp.contains(rc))) ||
                        (cardWindow.contains(row.date) && r.account.isNotBlank() && row.account == r.account && row.category == "Card bill")
                }
        }
        if (debit != null) { used.add(debit.id); matched.add(r.id) }
    }
    // A card's newer statement replaces the older one (it carries over anything unpaid), so an older bill for the
    // same card no longer shows as due or overdue next to it.
    fun isCard(r: Reminder) = r.minAmount != null || r.account.contains(" CC ") || r.account.startsWith("CC ") || r.account.endsWith(" CC")
    reminders.filter { isCard(it) && it.account.isNotBlank() }.groupBy { it.account }.values.forEach { bills ->
        val latest = bills.maxOf { it.dueDate }
        bills.filter { it.dueDate < latest }.forEach { matched.add(it.id) }
    }
    return matched
}
