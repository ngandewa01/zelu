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

@Composable internal fun AccountCard(account: String, bal: Pair<Double, Long>?, spent: Double, modifier: Modifier) {
    val parts = account.trim().split(" ")
    val last4 = parts.lastOrNull()?.takeIf { it.length in 2..8 && it.all { c -> c.isDigit() } }
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
                ) else BankMark(account, tint, 16.sp)
            }
            Column {
                Text(name, color = ZColor.Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (last4 != null) MutedText("xx$last4", 11)
                val nLinked = LinkInfo.map[account]?.size ?: 0
                if (nLinked > 0) Text("+$nLinked linked", color = ZColor.Accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Column(
            Modifier.weight(0.58f).fillMaxHeight().background(ZColor.Card).padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(if (bal != null) (if (Privacy.hideBalance) Privacy.MASK else money(bal.first)) else "—", color = ZColor.Ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                MutedText(if (bal != null && bal.second > 0) "as of " + fmt.format(java.util.Date(bal.second)) else "no balance alert", 11)
            }
            MutedText("Spent  " + money(spent), 12)
        }
    }
}

// ---------- credit card detail ----------

@Composable internal fun CardDetailScreen(
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
        .newestFirst()
    val spendRows = rows.filter { it.kind == "expense" }
    val spent = spendRows.sumOf { it.amount }
    val parts = account.trim().split(" ")
    val last4 = parts.lastOrNull()?.takeIf { it.length in 2..8 && it.all { c -> c.isDigit() } } ?: account.takeLast(4)
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
                TopIcon("Back", onClick = onBack) { Text("‹", color = ZColor.Ink, fontSize = 22.sp, modifier = Unspoken) }
                Spacer(Modifier.width(12.dp))
                Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                    if (logo != null) androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(logo), contentDescription = null, modifier = Modifier.size(28.dp),
                    ) else BankMark(account, bankColor(account))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("$bank Credit Card", color = ZColor.Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    MutedText("•••• $last4", 12)
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                NavButton("‹", "Previous cycle") { offset -= 1 }
                Row(
                    Modifier.weight(1f).clip(RoundedCornerShape(22.dp)).background(ZColor.Card)
                        .border(1.dp, ZColor.Line, RoundedCornerShape(22.dp)).clickable { dayDialog = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(prettyDay(from) + " – " + prettyDay(to) + "  ▾", color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                if (offset < 0) NavButton("›", "Next cycle") { offset += 1 }
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
                    Text(if (limit != null) (if (Privacy.hideBalance) Privacy.MASK else money(limit)) else "—", color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
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

// Which card is waiting for a limit you type in, and which limits were typed by you (shown as such).
internal object LimitAsk {
    var account by mutableStateOf<String?>(null)
    var manual by mutableStateOf<Set<String>>(emptySet())
}

@Composable internal fun CardTile(account: String, spent: Double, limit: Pair<Double, Long>?, total: Pair<Double, Long>?, due: Reminder?, usedSince: Double = 0.0, onClick: () -> Unit = {}) {
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
                        ) else BankMark(account, base, 15.sp)
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
                    Spacer(Modifier.weight(1f))
                    NetBadge(account, 30.dp)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Column {
                        val isManual = account in LimitAsk.manual
                        Text(if (limit != null || shown == null) "Available limit" else "Credit limit", color = soft, fontSize = 10.sp)
                        Text(if (shown != null) (if (Privacy.hideBalance) Privacy.MASK else money(shown.first)) else "Add your limit ›", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                            modifier = if (shown == null || isManual) Modifier.clickable { LimitAsk.account = account } else Modifier)
                        if (asOf != null) Text(if (account in LimitAsk.manual) "set by you on $asOf, less spends since · tap to change" else if (limit != null && kotlin.math.abs(usedSince) >= 1.0) "est. · SMS of $asOf, less spends since" else "as of $asOf", color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp)
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
