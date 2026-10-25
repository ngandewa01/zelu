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

// "Not mine" on something that has payments: show them and ask once more. Removing deletes the account AND those payments from Zelu.
@Composable internal fun RescanRangeDialog(onRun: (String, String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val today = Dates.today()
    var from by remember { mutableStateOf(Dates.shiftDays(today, -30)) }
    var to by remember { mutableStateOf(today) }
    fun pick(cur: String, set: (String) -> Unit) {
        val c = Dates.parse(cur)
        android.app.DatePickerDialog(context, { _, y, m, d -> set(String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)) },
            c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH), c.get(java.util.Calendar.DAY_OF_MONTH)).show()
    }
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp)) {
            Text("Rescan between dates", color = ZColor.Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            MutedText("Zelu reads only the messages of this period. Anything already in is left as it is.", 12)
            Spacer(Modifier.height(12.dp))
            ChipScroll {
                val cur = Dates.todayMonth()
                val last = Dates.shiftMonth(cur, -1)
                Chip("Last 7 days", false) { from = Dates.shiftDays(today, -7); to = today }
                Chip("Last 30 days", false) { from = Dates.shiftDays(today, -30); to = today }
                Chip("This month", false) { from = "$cur-01"; to = today }
                Chip("Last month", false) { from = "$last-01"; to = Dates.shiftDays("$cur-01", -1) }
                Chip("Last 3 months", false) { from = Dates.shiftDays(today, -90); to = today }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, ZColor.Line, RoundedCornerShape(12.dp))
                .clickable { pick(from) { from = it } }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { MutedText("Start date", 12); Text(prettyDate(from), color = ZColor.Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
                androidx.compose.material3.Icon(Icons.Rounded.CalendarMonth, null, tint = ZColor.Accent, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, ZColor.Line, RoundedCornerShape(12.dp))
                .clickable { pick(to) { to = it } }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { MutedText("End date", 12); Text(prettyDate(to), color = ZColor.Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
                androidx.compose.material3.Icon(Icons.Rounded.CalendarMonth, null, tint = ZColor.Accent, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                Text("Cancel", color = ZColor.Muted, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { onDismiss() }.padding(12.dp))
                Spacer(Modifier.width(8.dp))
                Box(Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                    .clickable { if (from <= to) onRun(from, to) else onRun(to, from) }.padding(horizontal = 22.dp, vertical = 10.dp)) {
                    Text("Rescan", color = ZColor.Paper, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// The income half of the Categories page: built-in ones stay, your own can be added and deleted.
@Composable internal fun ColumnScope.IncomeCategoriesBody() {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    MutedText("Pick one from a payment's details (Change category). The built-in ones stay; ones you add can be deleted.", 12)
    Spacer(Modifier.height(10.dp))
    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
        IncomeCats.all.forEach { c ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(catColor(c).copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.Icon(CatIcons.forCategory(c) ?: Icons.Rounded.ArrowDownward, null, tint = catColor(c), modifier = Modifier.size(19.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(c, color = ZColor.Ink, fontSize = 15.sp, modifier = Modifier.weight(1f))
                if (c in IncomeCats.custom) Text("Delete", color = ZColor.Warn, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { IncomeCats.remove(context, c) }.padding(8.dp))
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, modifier = Modifier.weight(1f),
            placeholder = { Text("New income category") })
        Spacer(Modifier.width(8.dp))
        Text("Add", color = ZColor.Accent, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
            if (IncomeCats.add(context, name)) name = "" else Toast.makeText(context, "Type a new name", Toast.LENGTH_SHORT).show()
        }.padding(12.dp))
    }
}

@Composable internal fun DeclineConfirmDialog(key: String, rows: List<Row>, onKeep: () -> Unit, onRemove: () -> Unit) {
    val mine = remember(rows, key) { rowsOfAccount(rows, key).newestFirst() }
    Dialog(onDismissRequest = onKeep) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp).heightIn(max = 620.dp)) {
            Text("Remove " + AccountBook.displayName(key) + "?", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            MutedText("It has ${mine.size} ${if (mine.size == 1) "payment" else "payments"} (${money(mine.sumOf { it.amount })}). " +
                "If you remove it, these go from Zelu too: totals, payees, notifications.", 12)
            Spacer(Modifier.height(10.dp))
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                mine.take(10).forEach { r ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(r.counterparty?.takeIf { it.isNotBlank() } ?: r.category, color = ZColor.Ink, fontSize = 14.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            MutedText(prettyDate(r.date), 11)
                        }
                        Text(money(r.amount), color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (mine.size > 10) MutedText("and ${mine.size - 10} more", 12)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                Text("Remove all", color = ZColor.Warn, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onRemove() }.padding(12.dp))
                Spacer(Modifier.width(8.dp))
                Box(Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink).clickable { onKeep() }
                    .padding(horizontal = 22.dp, vertical = 10.dp)) { Text("Keep it", color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

// One screen for first setup and for Menu: your accounts, what is joined into each, and one sheet to change any of them.
@Composable internal fun AccountsScreen(
    book: List<AccountBook.Acct>, firstRun: Boolean, undoMsg: String?, onUndo: () -> Unit,
    onSave: (AccountBook.Plan, String) -> Unit, onDecline: (String, String) -> Unit, onRestore: (String) -> Unit,
    onDone: () -> Unit, rows: List<Row>,
) {
    var open by remember { mutableStateOf<String?>(null) }
    var showRemoved by remember { mutableStateOf(false) }
    var declineFor by remember { mutableStateOf<AccountBook.Acct?>(null) }
    val (known, unknown) = remember(book) { AccountBook.ordered(book) }
    val removed = remember(book) { book.filter { it.declined } }
    androidx.activity.compose.BackHandler(!firstRun) { if (open != null) open = null else onDone() }
    androidx.activity.compose.BackHandler(open != null) { open = null }
    Box(Modifier.fillMaxSize().background(ZColor.Paper)) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 18.dp, end = 18.dp, top = 46.dp, bottom = 16.dp)) {
            Text("Your accounts", color = ZColor.Ink, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            MutedText(if (firstRun) "Zelu found these in your bank messages. A quick check, about 30 seconds:"
                else "Tap an account to see what is joined into it, or to change it.", 13)
            Spacer(Modifier.height(10.dp))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                if (firstRun) {
                    // New users did not know what to look for here: say it in three plain steps.
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ZColor.Card).padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(
                            "Not yours?" to "A family member's or an old account: tap it, then Not my account.",
                            "Same account twice?" to "Banks write one account in different ways (XX9116, A/c 9116, its debit card). Tap one and tick the other to join them.",
                            "Looks fine?" to "Just tap Continue. You can change this any time in Menu, Accounts.",
                        ).forEachIndexed { i, (head, body) ->
                            Row {
                                Box(Modifier.size(22.dp).clip(RoundedCornerShape(11.dp)).background(ZColor.Accent), contentAlignment = Alignment.Center) {
                                    Text("${i + 1}", color = ZColor.AccentInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(head, color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    MutedText(body, 12)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
                @Composable fun card(a: AccountBook.Acct, faded: Boolean = false) {
                    Column(Modifier.fillMaxWidth().padding(top = 10.dp).clip(RoundedCornerShape(16.dp)).background(ZColor.Card)
                        .clickable { open = a.key }.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AcctLogo(a.key)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(AccountBook.displayName(a.key), color = if (faded) ZColor.Muted else ZColor.Ink, fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                MutedText(acctLine(a.type, a.count) + (if (a.own.lastDate.isNotBlank()) " · last " + money(a.own.lastAmount) + " on " + prettyDate(a.own.lastDate) else ""), 12)
                                if (a.own.lastRaw.isNotBlank()) Text("“" + a.own.lastRaw.take(90) + "”", color = ZColor.InkSoft, fontSize = 11.sp,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                AccountBook.cardLink(book, a)?.let { Text("Looks like the card of ${it.key}. Tap to link.", color = ZColor.Accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                            }
                            val bal = if (a.type == "card") a.limit else a.balance
                            if (bal != null && !faded) Text(if (Privacy.hideBalance) Privacy.MASK else money(bal), color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        if (a.parts.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text("Includes " + a.parts.joinToString(" · ") { AccountBook.displayName(it.key) }, color = ZColor.Muted, fontSize = 12.sp,
                                modifier = Modifier.padding(start = 50.dp))
                        }
                    }
                }
                known.forEach { card(it) }
                if (unknown.isNotEmpty()) {
                    Spacer(Modifier.height(18.dp))
                    Eyebrow("Bank not known")
                    MutedText("Zelu could not tell which bank these are. If one is the same as an account above, tap it and tick that account. Otherwise leave it.", 12)
                    unknown.forEach { card(it) }
                }
                if (removed.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    Text((if (showRemoved) "Hide " else "Show ") + plural(removed.size, "account that is not mine", "accounts that are not mine"),
                        color = ZColor.Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { showRemoved = !showRemoved }.padding(vertical = 8.dp))
                    if (showRemoved) removed.forEach { card(it, faded = true) }
                }
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(8.dp))
            Cta(if (firstRun) "Looks right, continue" else "Done", onDone)
        }
        undoMsg?.let { m ->
            Row(Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = 84.dp).fillMaxWidth()
                .clip(RoundedCornerShape(14.dp)).background(ZColor.Ink).padding(start = 16.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(m, color = ZColor.Paper, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("UNDO", color = ZColor.Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { onUndo() }.padding(horizontal = 14.dp, vertical = 12.dp))
            }
        }
        book.firstOrNull { it.key == open }?.let { a ->
            AccountSheet(a, book, onClose = { open = null },
                onSave = { plan, msg -> open = null; onSave(plan, msg) },
                onDecline = { declineFor = a }, onRestore = { open = null; onRestore(a.key) })
        }
    }
    declineFor?.let { a ->
        DeclineConfirmDialog(a.key, rows, onKeep = { declineFor = null }, onRemove = { onDecline(a.key, a.type); declineFor = null; open = null })
    }
}

// Everything you can do to one account: its type, and which other accounts are really the same one.
@Composable internal fun AccountSheet(
    a: AccountBook.Acct, book: List<AccountBook.Acct>, onClose: () -> Unit,
    onSave: (AccountBook.Plan, String) -> Unit, onDecline: () -> Unit, onRestore: () -> Unit,
) {
    val context = LocalContext.current
    class Opt(val key: String, val type: String, val stat: AccountBook.Stat, val auto: Boolean, val numberMatch: Boolean)
    val options = remember(a, book) {
        val parts = a.parts.map { Opt(it.key, AccountBook.guessType(it.key).let { g -> if (g == "savings") it.stat.looks ?: g else g }, it.stat, it.auto, AccountBook.numberMatch(a.key, it.key)) }
        val partKeys = parts.map { it.key }.toSet()
        parts + AccountBook.similar(book, a).filter { it.key !in partKeys }.map { Opt(it.key, it.type, it.own, false, AccountBook.numberMatch(a.key, it.key)) }
    }
    val suggested = remember(a, book) { AccountBook.cardLink(book, a) }
    val ticked = remember(a) { mutableStateMapOf<String, Boolean>().also { m -> a.parts.forEach { m[it.key] = true }; suggested?.let { m[it.key] = true } } }
    var type by remember(a) { mutableStateOf(a.type) }
    var net by remember(a) { mutableStateOf(CardNet.map[a.key]) }
    val netScope = androidx.compose.runtime.rememberCoroutineScope()
    var showMsg by remember(a) { mutableStateOf(false) }
    androidx.compose.runtime.key(a.key) {
        Box(Modifier.fillMaxSize().background(Color(0x99000000)).clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { onClose() },
            contentAlignment = Alignment.BottomCenter) {
            Column(Modifier.fillMaxWidth().heightIn(max = 640.dp).clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).background(ZColor.Paper)
                .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) {}
                .windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = 18.dp, vertical = 16.dp)) {
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AcctLogo(a.key, 42)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(AccountBook.displayName(a.key), color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            MutedText(plural(a.count, if (a.type == "loan") "EMI" else "payment", if (a.type == "loan") "EMIs" else "payments"), 12)
                        }
                        val bal = if (a.type == "card") a.limit else a.balance
                        if (bal != null && !a.declined) Text(if (Privacy.hideBalance) Privacy.MASK else money(bal), color = ZColor.Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    // The newest message is shown straight away: people know an account by what its bank writes, not its number.
                    if (a.own.lastRaw.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        MutedText("Latest message from this account", 12)
                        Spacer(Modifier.height(4.dp))
                        Text(a.own.lastRaw, color = ZColor.InkSoft, fontSize = 12.sp, maxLines = if (showMsg) 20 else 4, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(ZColor.Card)
                                .clickable { showMsg = !showMsg }.padding(10.dp))
                    }
                    if (suggested != null && !a.declined) {
                        Spacer(Modifier.height(10.dp))
                        MutedText("This looks like the debit card of ${suggested.key}. It is ticked below. Save to link it, or untick it to keep it separate.", 13)
                    }
                    if (a.declined) {
                        Spacer(Modifier.height(14.dp))
                        MutedText("You said this is not your account, so its payments are not counted.", 13)
                    } else {
                        if (a.type != "loan" && a.type != "debit") {
                            Spacer(Modifier.height(12.dp))
                            MutedText("What is it?", 12)
                            Spacer(Modifier.height(6.dp))
                            ChipScroll {
                                listOf("savings" to "Savings account", "card" to "Credit card", "loan" to "Loan", "fd" to "Fixed deposit", "rd" to "Recurring deposit", "other" to "Wallet or other").forEach { (ty, lab) ->
                                    PickChip(lab, type == ty) { type = ty }
                                }
                            }
                        }
                        if (a.type == "card" && type == "card") {
                            Spacer(Modifier.height(12.dp))
                            MutedText("Card network", 12)
                            Spacer(Modifier.height(6.dp))
                            ChipScroll {
                                CardNet.names.forEach { n ->
                                    PickChip(n, net == n, lead = {
                                        CardNet.logo(n)?.let { androidx.compose.foundation.Image(painter = androidx.compose.ui.res.painterResource(it), contentDescription = null, modifier = Modifier.size(18.dp)) }
                                    }) {
                                        net = if (net == n) null else n
                                        val pick = net
                                        netScope.launch {
                                            withContext(Dispatchers.IO) { Storage.setCardNetwork(context, a.key, pick) }
                                            CardNet.map = withContext(Dispatchers.IO) { Storage.cardNetworks(context) }
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Text("Is it the same account as any of these?", color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(2.dp))
                        MutedText(if (options.isEmpty()) "Zelu found no other account like this one. Nothing to join."
                            else "Tick one if it is this account written another way, or its debit card. Leave them unticked if not.", 12)
                        options.forEach { o ->
                            val on = ticked[o.key] == true
                            Row(Modifier.fillMaxWidth().clickable { ticked[o.key] = !on }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                AcctLogo(o.key, 36)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(AccountBook.displayName(o.key), color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    val extra = (if (o.auto) "Joined by Zelu · " else if (o.numberMatch) "Number matches · " else "") + acctTypeLabel(o.type) + " · " +
                                        plural(o.stat.count, "payment", "payments") + (if (o.stat.lastDate.isNotBlank()) " · last " + money(o.stat.lastAmount) + " on " + prettyDate(o.stat.lastDate) else "")
                                    Text(extra, color = if (o.auto || o.numberMatch) ZColor.Income else ZColor.Muted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    // Its newest message, so it can be recognised without knowing the number.
                                    if (o.stat.lastRaw.isNotBlank()) Text("“" + o.stat.lastRaw.take(90) + "”", color = ZColor.InkSoft, fontSize = 11.sp,
                                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                                Box(Modifier.size(24.dp).clip(RoundedCornerShape(7.dp)).background(if (on) ZColor.Accent else Color.Transparent)
                                    .border(2.dp, if (on) ZColor.Accent else ZColor.Line, RoundedCornerShape(7.dp)), contentAlignment = Alignment.Center) {
                                    if (on) Text("✓", color = ZColor.AccentInk, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (a.declined) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SheetButton("Close", false, Modifier.weight(1f), onClose)
                        SheetButton("This is my account", true, Modifier.weight(1.4f), onRestore)
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SheetButton("Cancel", false, Modifier.weight(1f), onClose)
                        SheetButton("Save", true, Modifier.weight(1f)) {
                            val set = ticked.filter { it.value }.keys
                            val plan = AccountBook.plan(book, Storage.accountRegistry(context), a.key, set, type)
                            val names = set.filter { it !in a.parts.map { p -> p.key } }
                            val msg = when {
                                names.isNotEmpty() -> "Joined " + names.joinToString(", ") { AccountBook.displayName(it) } + " into " + AccountBook.displayName(plan.main)
                                a.parts.any { it.key !in set } -> "Separated " + a.parts.filter { it.key !in set }.joinToString(", ") { AccountBook.displayName(it.key) }
                                else -> "Saved"
                            }
                            onSave(plan, msg)
                        }
                    }
                    Text("Not my account", color = ZColor.Warn, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onDecline() }.padding(top = 14.dp, bottom = 4.dp))
                }
            }
        }
    }
}
