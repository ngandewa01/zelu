package com.koolicream.zelu

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

@Composable internal fun RemindersView(
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
    val autoSet = remember(reminders, rows) { autoSettledReminders(reminders, rows) }
    // An explicit paid/unpaid tap always wins; otherwise a previous-month due or a debit-matched
    // one is assumed paid.
    fun settled(r: Reminder) = state[r.id] ?: (r.dueDate.take(7) < curMonth || r.id in autoSet)

    val pending = reminders.filter { !settled(it) }.sortedBy { it.dueDate }
    val paid = reminders.filter { settled(it) }.sortedByDescending { it.dueDate }
    // Dues page tabs: what is coming, what is paid, and the autopay set-ups. Older months stay out of the way.
    var duesTab by remember { mutableStateOf(0) }
    var showOlderPaid by remember { mutableStateOf(false) }
    val paidNow = paid.filter { it.dueDate.take(7) >= curMonth }
    val paidOlder = paid.filter { it.dueDate.take(7) < curMonth }

    Spacer(Modifier.height(4.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Dues & reminders", color = ZColor.Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold,
            maxLines = 2, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.clip(RoundedCornerShape(20.dp)).border(1.dp, ZColor.Line, RoundedCornerShape(20.dp))
            .clickable { adding = true }.padding(horizontal = 14.dp, vertical = 8.dp)) {
            Text("+ Add bill", color = ZColor.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
        }
    }

    Spacer(Modifier.height(12.dp))
    ChipScroll {
        Chip("Upcoming" + if (pending.isNotEmpty()) " (" + pending.size + ")" else "", duesTab == 0, filled = true) { duesTab = 0 }
        Chip("Paid", duesTab == 1, filled = true) { duesTab = 1 }
        if (mandates.isNotEmpty()) Chip("Autopay", duesTab == 2, filled = true) { duesTab = 2 }
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
    if (duesTab == 0 && (pending.isNotEmpty() || spentSoFar > 0)) Panel {
        if (pending.isNotEmpty()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Fact("Overdue", money(overdueList.sumOf { it.amount }))
            Fact("Next 7 days", money(weekList.sumOf { it.amount }))
            Fact("Later", money(laterList.sumOf { it.amount }))
        }
        Spacer(Modifier.height(14.dp))
        // The next 30 days; swipe sideways for later ones. Today has an outline; a day with a bill is orange and opens it.
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (i in 0..29) {
                val d = Dates.shiftDays(today, i)
                val due = pending.filter { it.dueDate == d }
                val letter = "SMTWTFS"[Dates.parse(d).get(java.util.Calendar.DAY_OF_WEEK) - 1].toString()
                val dayN = d.substring(8, 10).toInt()
                val shape = RoundedCornerShape(10.dp)
                Column(Modifier.width(44.dp).clip(shape)
                    .background(if (due.isNotEmpty()) ZColor.Warn.copy(alpha = 0.14f) else ZColor.Paper)
                    .then(if (i == 0) Modifier.border(2.dp, ZColor.Ink, shape) else Modifier)
                    .then(if (due.isNotEmpty()) Modifier.clickable { detail = due.first() } else Modifier)
                    .padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (i == 0) "Today" else if (dayN == 1) MONTH_ABBR[d.substring(5, 7).toInt() - 1] else letter,
                        color = if (i == 0) ZColor.Ink else ZColor.Muted, fontSize = 10.sp,
                        fontWeight = if (i == 0 || dayN == 1) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
                    Text(dayN.toString(), color = ZColor.Ink, fontSize = 15.sp,
                        fontWeight = if (i == 0) FontWeight.Bold else FontWeight.Medium)
                    Text(if (due.isNotEmpty()) shortAmt(due.sumOf { it.amount }) else "·",
                        color = if (due.isNotEmpty()) ZColor.Warn else ZColor.Line, fontSize = 10.sp, maxLines = 1)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        MutedText("Swipe for the next 30 days. Tap an orange day to open its bill.", 11)
        }
        if (spentSoFar > 0) {
            if (pending.isNotEmpty()) Spacer(Modifier.height(14.dp))
            Eyebrow("Expected by month end")
            Spacer(Modifier.height(4.dp))
            Text("≈ " + money(projected), color = ZColor.Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            MutedText("${money(spentSoFar)} spent" + (if (dueLeft > 0) " + ${money(dueLeft)} dues left" else "") +
                (if (daysLeft > 0 && typicalDaily > 0) " + about ${money(typicalDaily)} a day for $daysLeft more days" else ""), 12)
        }
    }

    if (duesTab != 0) { /* the Upcoming list is only on its own tab */ }
    else if (pending.isEmpty()) {
        Spacer(Modifier.height(10.dp))
        MutedText("Nothing due. Bill and EMI reminders from your SMS show up here.", 13)
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
    if (duesTab == 1) {
        Spacer(Modifier.height(14.dp))
        if (paidNow.isEmpty() && paidOlder.isEmpty()) Panel { MutedText("Nothing paid yet. Bills you tick off appear here.") }
        if (paidNow.isNotEmpty()) {
            Eyebrow("Paid this month")
            paidNow.forEach { r -> ReminderCard(r, overdue = false, paid = true, onOpen = { detail = r }) { onMark(r.id, false) } }
        }
        if (paidOlder.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(if (showOlderPaid) "Hide earlier months" else "Show earlier months (" + paidOlder.size + ")", color = ZColor.Accent,
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { showOlderPaid = !showOlderPaid }.padding(vertical = 6.dp))
            if (showOlderPaid) paidOlder.take(25).forEach { r -> ReminderCard(r, overdue = false, paid = true, onOpen = { detail = r }) { onMark(r.id, false) } }
        }
    }
    if (duesTab == 2 && mandates.isNotEmpty()) {
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

@Composable internal fun ReminderCard(r: Reminder, overdue: Boolean, paid: Boolean = false, onOpen: () -> Unit = {}, onTick: () -> Unit) {
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
                .checkRow(paid) { onTick() }.spoken("Bill paid"),
            contentAlignment = Alignment.Center,
        ) { Text("✓", color = if (paid) ZColor.Income else ZColor.Muted, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Unspoken) }
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
                (row.kind == "expense" || row.kind == "transfer" || row.kind == "invest") &&
                kotlin.math.abs(row.amount - r.amount) < 1.0 && run {
                    val rc = row.counterparty?.lowercase()?.trim()
                    (window.contains(row.date) && !rc.isNullOrEmpty() && (rc == cp || rc.contains(cp) || cp.contains(rc))) ||
                        (cardWindow.contains(row.date) && r.account.isNotBlank() && row.account == r.account && row.category == "Card bill")
                }
        }
        if (debit != null) { used.add(debit.id); matched.add(r.id) }
    }
    return matched
}
