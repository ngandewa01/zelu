package com.koolicream.zelu

import androidx.compose.ui.res.stringResource

import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.util.Locale

@Composable internal fun AccountCard(account: String, bal: Pair<Double, Long>?, spent: Double, modifier: Modifier) {
    val parts = account.trim().split(" ")
    val last4 = parts.lastOrNull()?.takeIf { it.length in 2..8 && it.all { c -> c.isDigit() } }
    val name = if (last4 != null) parts.dropLast(1).joinToString(" ") else account
    val tint = bankColor(account)
    val logo = bankLogoRes(account)
    val fmt = java.text.SimpleDateFormat("d MMM", java.util.Locale.US)
    // Bank and number on top, the balance as the one big number, and what went out this month under it.
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(ZColor.Paper).border(1.dp, ZColor.Line, RoundedCornerShape(16.dp))
        .padding(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(end = 30.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(Color.White).border(1.dp, tint.copy(alpha = 0.25f), RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center) {
                if (logo != null) androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(logo), contentDescription = null, modifier = Modifier.size(26.dp),
                ) else BankMark(account, tint, 16.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(niceName(name), color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val nLinked = LocalAccountInfo.current.links[account]?.size ?: 0
                Text((if (last4 != null) "•• $last4" else "Account") + (if (nLinked > 0) " · +$nLinked linked" else ""),
                    color = ZColor.Muted, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(if (bal != null && bal.second > 0) "Balance · " + fmt.format(java.util.Date(bal.second)) else "Balance",
            color = ZColor.Muted, fontSize = 12.sp)
        Text(if (bal != null) privateMoney(bal.first) else "—", color = ZColor.Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Rounded.NorthEast, null,
                tint = Color(if (ZTheme.dark) 0xFFFF8A3D else 0xFFFF6B1A), modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(4.dp))
            Text(privateMoney(spent), color = ZColor.Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.spent_4), color = ZColor.Muted, fontSize = 13.sp)
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
    val spendRows = rows.filter { it.kind == Kind.EXPENSE }
    val spent = spendRows.rupees()
    val parts = account.trim().split(" ")
    val last4 = parts.lastOrNull()?.takeIf { it.length in 2..8 && it.all { c -> c.isDigit() } } ?: account.takeLast(4)
    val bank = parts.firstOrNull() ?: account
    val logo = bankLogoRes(account)
    val today = Dates.today()
    val due = reminders.filter { r ->
        r.account.takeLast(4) == account.takeLast(4) && !(reminderState[r.id] ?: r.done)
    }.sortedBy { it.dueDate }.firstOrNull { it.dueDate >= today }

    Box(Modifier.fillMaxSize().background(ZColor.Paper)) {
        // A lazy list, so a long cycle draws only the payments on screen.
        androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 18.dp, end = 18.dp, top = 46.dp, bottom = if (due != null) 130.dp else 40.dp)) {
          item { Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TopIcon(stringResource(R.string.back), onClick = onBack) { Text("‹", color = ZColor.Ink, fontSize = 22.sp, modifier = Unspoken) }
                Spacer(Modifier.width(12.dp))
                Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                    if (logo != null) androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(logo), contentDescription = null, modifier = Modifier.size(28.dp),
                    ) else BankMark(account, bankColor(account))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(stringResource(R.string.credit_card, bank), color = ZColor.Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
            val frac = if (budget > 0) (spent / budget).coerceIn(0.0, 1.0).toFloat() else 0f
            // The ring shows spending against this card's budget; with no budget there is nothing to fill, so just the number.
            Box(Modifier.align(Alignment.CenterHorizontally).size(if (budget > 0) 190.dp else 110.dp), contentAlignment = Alignment.Center) {
                if (budget > 0) Canvas(Modifier.fillMaxSize()) {
                    val w = 16f
                    val sz = androidx.compose.ui.geometry.Size(size.width - w, size.height - w)
                    val tl = androidx.compose.ui.geometry.Offset(w / 2, w / 2)
                    drawArc(track, 0f, 360f, false, tl, sz, style = Stroke(width = w))
                    if (frac > 0f) drawArc(arc, -90f, 360f * frac, false, tl, sz, style = Stroke(width = w, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MutedText(stringResource(R.string.spent_3), 12)
                        InfoDot(stringResource(R.string.card_spends_count_in_your))
                    }
                    Text(money(spent), color = ZColor.Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    if (budget > 0) MutedText(stringResource(R.string.of, money(budget)), 12)
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f).clickable { budgetDialog = true }) {
                    MutedText(stringResource(R.string.monthly_budget), 12)
                    Text(if (budget > 0) money(budget) else "Set budget ›", color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    MutedText(stringResource(R.string.available_credit), 12)
                    Text(if (limit != null) (if (Privacy.hideBalance) Privacy.MASK else money(limit)) else "—", color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(16.dp))
            TabRowWrap {
                IconTab(Icons.AutoMirrored.Rounded.FormatListBulleted, stringResource(R.string.transactions), 0xFF3B82F6, 0xFF60A5FA, tab == 0, modifier = Modifier.weight(1f)) { tab = 0 }
                IconTab(Icons.Rounded.Category, stringResource(R.string.categories), 0xFF8B5CF6, 0xFFA78BFA, tab == 1, modifier = Modifier.weight(1f)) { tab = 1 }
                IconTab(Icons.Rounded.Storefront, stringResource(R.string.merchants), 0xFFFF6B1A, 0xFFFF8A3D, tab == 2, modifier = Modifier.weight(1f)) { tab = 2 }
            }
            Spacer(Modifier.height(6.dp))
          } }
          if (tab == 0 && rows.isNotEmpty()) {
            item { PanelEnd(top = true) }
            items(rows.size) { i -> PanelSlice { TxnRow(rows[i], false, onRowClick) } }
            item { PanelEnd(top = false) }
          } else item { Column(Modifier.fillMaxWidth()) {
            when (tab) {
                0 -> Panel { MutedText(stringResource(R.string.no_transactions_in_this_cycle)) }
                1 -> Panel {
                    val items = Analytics.byCategory(spendRows)
                    if (items.isEmpty()) MutedText(stringResource(R.string.nothing_spent_in_this_cycle))
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
                        .map { (k, v) -> Triple(k, v.rupees(), v.first().category) }.sortedByDescending { it.second }
                    if (items.isEmpty()) MutedText(stringResource(R.string.nothing_spent_in_this_cycle))
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
          } }
        }
        if (due != null) {
            Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(ZColor.Card).border(1.dp, ZColor.Line, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 26.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(stringResource(R.string.pending_bill_due) + prettyDay(due.dueDate), color = ZColor.Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(money(due.amount), color = ZColor.Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    due.minAmount?.let { MutedText(stringResource(R.string.min_payable) + money(it), 12) }
                }
                Box(
                    Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                        .clickable { onMarkPaid(due.id) }.padding(horizontal = 18.dp, vertical = 12.dp),
                ) { Text(stringResource(R.string.mark_as_paid), color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
            }
        }
    }

    if (dayDialog) {
        Dialog(onDismissRequest = { dayDialog = false }) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(18.dp)) {
                Text(stringResource(R.string.billing_cycle_starts_on), color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                MutedText(stringResource(R.string.the_day_your_card_s), 12)
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
                Text(stringResource(R.string.monthly_card_budget), color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = text, onValueChange = { v -> text = v.filter { it.isDigit() || it == '.' } },
                    singleLine = true, placeholder = { Text(stringResource(R.string.per_cycle)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(14.dp))
                Box(
                    Modifier.align(Alignment.End).clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                        .clickable { onSavePref(day, text.toDoubleOrNull() ?: 0.0); budgetDialog = false }
                        .padding(horizontal = 22.dp, vertical = 10.dp),
                ) { Text(stringResource(R.string.save), color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}


@Composable internal fun CardTile(account: String, spent: Double, limit: Pair<Double, Long>?, total: Pair<Double, Long>?, due: Reminder?, usedSince: Double = 0.0, onClick: () -> Unit = {}) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val info = LocalAccountInfo.current
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
            // Name on top, one big number (what you can still spend), and one line for this cycle. The top-right corner
            // is left free for the ⋯ menu.
            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Row(Modifier.fillMaxWidth().padding(end = 34.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                        if (logo != null) androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(logo), contentDescription = null, modifier = Modifier.size(24.dp),
                        ) else BankMark(account, base, 15.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(niceName(bank) + " Credit Card", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("•• $last4", color = soft, fontSize = 12.sp, letterSpacing = 1.sp)
                    }
                    NetBadge(account, 28.dp)
                }
                Column {
                    val isManual = account in info.manualLimits
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (limit != null || shown == null) "Available" else "Credit limit", color = soft, fontSize = 13.sp)
                        // Asks the bank for the latest limit by SMS.
                        if (enquiry != null) Text("  ↻", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable {
                                try {
                                    ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_SENDTO,
                                        android.net.Uri.parse("smsto:${enquiry.first}")).putExtra("sms_body", enquiry.second))
                                } catch (e: Exception) { CrashLog.warn("CardScreens.CardTile", e) }
                            }.buttonLabel("Ask the bank for the latest limit"))
                        if (asOf != null) Text("  · $asOf", color = soft, fontSize = 12.sp)
                    }
                    Text(if (shown != null) privateMoney(shown.first) else "Add your limit ›", color = Color.White,
                        fontSize = if (shown != null) 26.sp else 18.sp, fontWeight = FontWeight.Bold,
                        modifier = if (shown == null || isManual) Modifier.clickable { info.askLimit(account) } else Modifier)
                }
                // Only what has something to say: this cycle's spend and the bill, each when there is one.
                if (spent >= 0.5 || due != null) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (spent >= 0.5) {
                        Text(privateMoney(spent), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.spent_this_cycle), color = soft, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.weight(1f))
                    if (due != null) Text(stringResource(R.string.due, prettyDay(due.dueDate)) + " · " + privateMoney(due.amount),
                        color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                } else Spacer(Modifier.height(1.dp))
            }
        }
    }
}
