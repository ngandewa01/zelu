package com.koolicream.zelu

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Payments
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Dialogs about reading messages: unrecognised senders, reading everything again, the data check, missed messages
// and adding one by hand.

@Composable
internal fun SendersDialog(s: AppState) {
    with(s) {
        if (sendersOpen) Dialog(onDismissRequest = { sendersOpen = false }) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp).heightIn(max = 620.dp)) {
                Text(stringResource(R.string.unrecognised_senders), color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                MutedText(stringResource(R.string.senders_that_look_like_payments), 12)
                Spacer(Modifier.height(10.dp))
                val sl = senderList
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    when {
                        sl == null -> MutedText(stringResource(R.string.looking_through_your_messages), 13)
                        sl.isEmpty() -> MutedText(stringResource(R.string.none_every_payment_sender_is), 13)
                        else -> sl.forEach { u ->
                            Column(Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(12.dp)).background(ZColor.Paper).padding(12.dp)) {
                                Text(u.address, color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                MutedText(plural(u.count, "message", "messages") + " that look like payments", 11)
                                Spacer(Modifier.height(4.dp))
                                u.samples.forEach { s2 -> Text(s2, color = ZColor.InkSoft, fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis); Spacer(Modifier.height(3.dp)) }
                                Spacer(Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    listOf("bank" to "Bank", "wallet" to "Wallet", "other" to "Other").forEach { (ty, lab) ->
                                        Text(lab, color = ZColor.Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
                                            scope.launch {
                                                withContext(Dispatchers.IO) { Storage.trustSender(context, u.address, ty) }
                                                senderList = senderList?.filter { it.address != u.address }
                                                rescan(notify = true)
                                            }
                                        })
                                    }
                                    Text(stringResource(R.string.ignore), color = ZColor.Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable {
                                        scope.launch(Dispatchers.IO) { Storage.markSmsHandled(context, "sender|" + u.address.uppercase()) }
                                        senderList = senderList?.filter { it.address != u.address }
                                    })
                                    // Opens an email draft with only the sender id, so the bank can be added for everyone. The user sends it.
                                    Text(stringResource(R.string.tell_zelu), color = ZColor.Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable {
                                        val mail = android.content.Intent(android.content.Intent.ACTION_SENDTO, android.net.Uri.parse("mailto:"))
                                            .putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf(Storage.reportEmail(context)))
                                            .putExtra(android.content.Intent.EXTRA_SUBJECT, "Zelu: please add this bank")
                                            .putExtra(android.content.Intent.EXTRA_TEXT, "Sender id: " + u.address + "\nBank name: (please type it here)\n\nNo message text is included.")
                                        try { context.startScreen(android.content.Intent.createChooser(mail, "Tell Zelu")) }
                                        catch (_: Exception) { Toast.makeText(context, context.getString(R.string.no_email_app_found_on), Toast.LENGTH_LONG).show() }
                                    })
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.align(Alignment.End).clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                    .clickable { sendersOpen = false }.padding(horizontal = 22.dp, vertical = 10.dp)) {
                    Text(stringResource(R.string.close), color = ZColor.Paper, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
internal fun ReadAgainDialog(s: AppState) {
    with(s) {
        if (fullConfirm) Dialog(onDismissRequest = { fullConfirm = false }) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp)) {
                Text(stringResource(R.string.read_everything_again), color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                MutedText(stringResource(R.string.reads_every_message_again_your), 13)
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.cancel), color = ZColor.Muted, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { fullConfirm = false }.padding(12.dp))
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink).clickable { fullConfirm = false; rescan(full = true, notify = true) }
                        .padding(horizontal = 22.dp, vertical = 10.dp)) { Text(stringResource(R.string.read_again), color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

@Composable
internal fun DataCheckDialog(s: AppState) {
    with(s) {
        if (checkOpen) Dialog(onDismissRequest = { checkOpen = false }) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp).heightIn(max = 620.dp)) {
                Text(stringResource(R.string.data_check), color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                MutedText(stringResource(R.string.parser_version_transactions_stored, Storage.PARSER_VERSION, rows.size), 12)
                Spacer(Modifier.height(10.dp))
                val c = check
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    if (c == null) MutedText(stringResource(R.string.checking_your_bank_alerts), 13) else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Fact(stringResource(R.string.bank_sms), c.bankSms.toString()); Fact(stringResource(R.string.payments), c.txns.toString()); Fact(stringResource(R.string.balances), c.balanceAlerts.toString())
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Fact(stringResource(R.string.reminders), c.reminders.toString())
                            Fact(stringResource(R.string.missed), c.unread.toString(), if (c.unread > 0) ZColor.Warn else ZColor.Income)
                            Fact(stringResource(R.string.hidden_accts), Storage.hiddenAccounts(context).size.toString())
                        }
                        Spacer(Modifier.height(14.dp))
                        Eyebrow(stringResource(R.string.do_balances_add_up))
                        Spacer(Modifier.height(6.dp))
                        if (c.gaps.isEmpty()) MutedText(stringResource(R.string.yes_every_account_s_balances), 13)
                        else {
                            MutedText(stringResource(R.string.a_jump_means_a_message), 12)
                            Spacer(Modifier.height(6.dp))
                            c.gaps.forEach { (a, n) ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(a, color = ZColor.Ink, fontSize = 14.sp)
                                    Text(if (n == 1) "1 gap" else "$n gaps", color = ZColor.Warn, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        val last = Backup.lastAuto(context)
                        MutedText(if (last > 0) "Last weekly backup: " + java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.US).format(java.util.Date(last))
                        else "No weekly backup saved yet.", 12)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Box(Modifier.align(Alignment.End).clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                    .clickable { checkOpen = false }.padding(horizontal = 22.dp, vertical = 10.dp)) {
                    Text(stringResource(R.string.close), color = ZColor.Paper, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
internal fun MissedDialog(s: AppState) {
    with(s) {
        if (unreadOpen) Dialog(onDismissRequest = { unreadOpen = false }) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp)
                .heightIn(max = 620.dp)) {
                Text(stringResource(R.string.missed_messages), color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                MutedText(stringResource(R.string.money_messages_zelu_couldn_t), 12)
                Spacer(Modifier.height(10.dp))
                val list = unreadList
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    when {
                        list == null -> MutedText(stringResource(R.string.looking_through_your_messages), 13)
                        list.isEmpty() -> MutedText(stringResource(R.string.nothing_missed_every_money_message), 13)
                        else -> list.forEach { m ->
                            Column(Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(12.dp))
                                .background(ZColor.Paper).padding(12.dp)) {
                                MutedText(m.address + " · " + prettyDate(Dates.fmtMs(m.date)), 11)
                                Spacer(Modifier.height(4.dp))
                                Text(m.body, color = ZColor.Ink, fontSize = 12.sp, maxLines = 6, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text(stringResource(R.string.add_as_payment), color = ZColor.Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable { addFor = m })
                                    Text(stringResource(R.string.not_a_payment), color = ZColor.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.clickable {
                                            scope.launch(Dispatchers.IO) { Storage.markSmsHandled(context, Ingest.smsKey(m)) }
                                            unreadList = unreadList?.filter { it !== m }
                                            noteMissed(unreadList?.size ?: 0)
                                        })
                                    Text(stringResource(R.string.share), color = ZColor.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.clickable {
                                            val send = android.content.Intent(android.content.Intent.ACTION_SEND)
                                                .setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, m.body)
                                            try { context.startScreen(android.content.Intent.createChooser(send, "Share message")) } catch (e: Exception) { CrashLog.warn("MessageDialogs.MissedDialog", e) }
                                        })
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Box(Modifier.align(Alignment.End).clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                    .clickable { unreadOpen = false }.padding(horizontal = 22.dp, vertical = 10.dp)) {
                    Text(stringResource(R.string.close), color = ZColor.Paper, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
internal fun AddPaymentDialog(s: AppState) {
    with(s) {
        // Pickers open a window on the screen, so they take the screen's context, not the AppState's.
        val screen = androidx.compose.ui.platform.LocalContext.current
        addFor?.let { sms ->
            val credit = Regex("""(?i)\b(credited|received|deposited|refund(ed)?)\b""").containsMatchIn(sms.body)
            var amtText by remember(sms) { mutableStateOf(Parser.firstAmount(sms.body)?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "") }
            var isIn by remember(sms) { mutableStateOf(credit) }
            val sugg = remember(sms) { Parser.suggestTitles(sms.body, null) }
            var payee by remember(sms) { mutableStateOf(sugg.firstOrNull() ?: "") }
            var day by remember(sms) { mutableStateOf(Dates.fmtMs(sms.date)) }
            var learnIt by remember(sms) { mutableStateOf(true) }
            Dialog(onDismissRequest = { addFor = null }) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp)
                    .heightIn(max = 620.dp).verticalScroll(rememberScrollState())) {
                    Text(stringResource(R.string.add_as_payment), color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    MutedText(sms.body.take(160) + if (sms.body.length > 160) "…" else "", 11)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KindTab("spending", stringResource(R.string.spent_3), !isIn) { isIn = false }
                        KindTab("income", stringResource(R.string.received), isIn) { isIn = true }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = amtText, onValueChange = { v -> amtText = v.filter { it.isDigit() || it == '.' } }, singleLine = true,
                        placeholder = { Text(stringResource(R.string.amount)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = payee, onValueChange = { payee = it }, singleLine = true,
                        placeholder = { Text(stringResource(R.string.paid_to_from)) }, modifier = Modifier.fillMaxWidth())
                    if (sugg.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        ChipScroll { sugg.take(6).forEach { sName -> Chip(sName, payee == sName) { payee = sName } } }
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(Modifier.clip(RoundedCornerShape(20.dp)).border(1.dp, ZColor.Line, RoundedCornerShape(20.dp))
                        .clickable { pickDate(screen, day) { day = it } }.padding(horizontal = 16.dp, vertical = 9.dp)) {
                        Text(prettyDate(day), color = ZColor.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).checkRow(learnIt) { learnIt = !learnIt }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(22.dp).clip(RoundedCornerShape(6.dp)).background(if (learnIt) ZColor.Accent else ZColor.Paper)
                            .border(1.dp, if (learnIt) ZColor.Accent else ZColor.Line, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                            if (learnIt) Text("✓", color = ZColor.AccentInk, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Unspoken)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.do_the_same_for_similar), color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(stringResource(R.string.zelu_learns_this_message_s), color = ZColor.Muted, fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.cancel), color = ZColor.Muted, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { addFor = null }.padding(12.dp))
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink).clickable {
                            val amount = amtText.toDoubleOrNull() ?: 0.0
                            if (amount > 0) {
                                val dir = if (isIn) Dir.CREDIT else Dir.DEBIT
                                val name = payee.trim().ifBlank { null }
                                val acct = Parser.accountFor(sms.body, sms.address, dir)
                                val id = "man-" + Ingest.smsKey(sms).hashCode().toUInt().toString(16)
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        val own = Storage.splitList(Storage.loadSettings(context).ownAccounts)
                                        val c = Classifier.classify(Txn(id, day, paiseOf(amount), dir, name, null, sms.body, sms.address, acct), own)
                                        Storage.addManualTxn(context, Row(id, day, paiseOf(amount), dir, name, null, sms.body, sms.address, acct, c.kind, c.category, sms.date), Ingest.smsKey(sms))
                                        if (learnIt) Taught.learn(context, sms.body, sms.address, name, dir)
                                    }
                                    addFor = null
                                    if (learnIt) rescan()
                                    val l = withContext(Dispatchers.IO) { Ingest.unreadable(context) }
                                    unreadList = l; noteMissed(l.size)
                                    reload()
                                    Toast.makeText(context, context.getString(R.string.added_to_your_payments), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }.padding(horizontal = 22.dp, vertical = 10.dp)) { Text(stringResource(R.string.add), color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
                    }
                }
            }
        }
    }
}

// A payment with no message behind it: cash, or anything Zelu never saw. It is kept like a payment added from a missed message.
@Composable
internal fun AddTransactionDialog(s: AppState) {
    with(s) {
        if (!addNew) return
        val screen = androidx.compose.ui.platform.LocalContext.current
        val cash = stringResource(R.string.cash_label)
        var isIn by remember { mutableStateOf(false) }
        var amtText by remember { mutableStateOf("") }
        var payee by remember { mutableStateOf("") }
        var day by remember { mutableStateOf(Dates.today()) }
        var account by remember { mutableStateOf(cash) }
        var category by remember { mutableStateOf<String?>(null) }
        var note by remember { mutableStateOf("") }
        var pickAccount by remember { mutableStateOf(false) }
        // Every account and card Zelu knows (from payments and balances), as on the Accounts screen; cash first.
        val accountNames = remember(rows, balances) {
            listOf(cash) + (rows.map { it.account } + balances.keys).filter { it.isNotBlank() && it != cash }.distinct().sorted()
        }

        fun save(another: Boolean) {
            val amount = amtText.toDoubleOrNull() ?: 0.0
            if (amount <= 0) return
            val dir = if (isIn) Dir.CREDIT else Dir.DEBIT
            val name = payee.trim().ifBlank { null }
            val id = "cash-" + java.util.UUID.randomUUID().toString().take(12)
            // The chosen day at the time of day it is now, so the row sorts and shows a time like any other.
            val cal = Dates.parse(day).apply {
                val now = java.util.GregorianCalendar(Dates.IST)
                set(java.util.Calendar.HOUR_OF_DAY, now.get(java.util.Calendar.HOUR_OF_DAY))
                set(java.util.Calendar.MINUTE, now.get(java.util.Calendar.MINUTE))
            }
            val ms = cal.timeInMillis
            val acct = if (account == cash) cash else account
            val text = note.trim().ifBlank { context.getString(R.string.added_by_you_note) }
            scope.launch {
                withContext(Dispatchers.IO) {
                    val own = Storage.splitList(Storage.loadSettings(context).ownAccounts)
                    val guess = Classifier.classify(Txn(id, day, paiseOf(amount), dir, name, null, text, "manual", acct, ms), own)
                    val kind = when {
                        isIn -> Kind.INCOME
                        category == "Investment" -> Kind.INVEST
                        else -> Kind.EXPENSE
                    }
                    val cat = category ?: if (isIn) "Income" else guess.category
                    Storage.addManualTxn(context, Row(id, day, paiseOf(amount), dir, name, null, text, "manual", acct, kind, cat, ms), null)
                }
                reload()
                Toast.makeText(context, context.getString(R.string.added_one), Toast.LENGTH_SHORT).show()
                if (another) { amtText = ""; payee = ""; note = ""; category = null } else addNew = false
            }
        }

        if (pickAccount) Dialog(onDismissRequest = { pickAccount = false }) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(vertical = 16.dp)
                .heightIn(max = 640.dp).verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.select_source_account), color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                accountNames.forEach { a ->
                    AccountRow(a, cash, Modifier.fillMaxWidth().clickable { account = a; pickAccount = false }.testTag("account-" + a).padding(horizontal = 20.dp, vertical = 10.dp))
                }
            }
        }
        Dialog(onDismissRequest = { addNew = false }) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp)
                .heightIn(max = 640.dp).verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.add_a_transaction), color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KindTab("spending", stringResource(R.string.spent_3), !isIn) { isIn = false; category = null }
                    KindTab("income", stringResource(R.string.received), isIn) { isIn = true; category = null }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = amtText, onValueChange = { v -> amtText = v.filter { it.isDigit() || it == '.' } }, singleLine = true,
                    placeholder = { Text(stringResource(R.string.amount)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("add-amount"))
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = payee, onValueChange = { payee = it }, singleLine = true,
                    placeholder = { Text(stringResource(R.string.paid_to_from)) }, modifier = Modifier.fillMaxWidth().testTag("add-payee"))
                Spacer(Modifier.height(10.dp))
                Box(Modifier.clip(RoundedCornerShape(20.dp)).border(1.dp, ZColor.Line, RoundedCornerShape(20.dp))
                    .clickable { pickDate(screen, day) { day = it } }.padding(horizontal = 16.dp, vertical = 9.dp)) {
                    Text(prettyDate(day), color = ZColor.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(12.dp))
                Eyebrow(stringResource(R.string.account_label))
                Spacer(Modifier.height(6.dp))
                AccountRow(account, cash, Modifier.clip(RoundedCornerShape(14.dp)).border(1.dp, ZColor.Line, RoundedCornerShape(14.dp))
                    .clickable { pickAccount = true }.testTag("add-account").padding(10.dp), arrow = true)
                Spacer(Modifier.height(12.dp))
                Eyebrow(stringResource(R.string.category))
                Spacer(Modifier.height(6.dp))
                val cats = if (isIn) IncomeCats.all else Classifier.CATEGORIES.keys.toList() + CustomCats.names
                // Its own scroll position per kind, so switching from Spent to Received doesn't open the income list part-way along.
                androidx.compose.runtime.key(isIn) {
                    ChipScroll { cats.forEach { c -> Chip(c, category == c) { category = if (category == c) null else c } } }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = note, onValueChange = { note = it }, singleLine = true,
                    placeholder = { Text(stringResource(R.string.note_optional)) }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(14.dp))
                // Cancel and Save share one row; "Save & add another" has its own line, so no button is squeezed on a narrow phone.
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.cancel), color = ZColor.Muted, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { addNew = false }.padding(12.dp))
                    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink).clickable { save(false) }.testTag("add-save").padding(horizontal = 28.dp, vertical = 10.dp)) {
                        Text(stringResource(R.string.save), color = ZColor.Paper, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                    }
                }
                Text(stringResource(R.string.save_and_add_another), color = ZColor.Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally).clickable { save(true) }.padding(12.dp))
            }
        }
    }
}

// An account as the app shows it elsewhere: its bank's logo, the bank and the number (or "Cash").
@Composable
private fun AccountRow(account: String, cash: String, modifier: Modifier, arrow: Boolean = false) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        if (account == cash) Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(ZColor.Accent), contentAlignment = Alignment.Center) {
            androidx.compose.material3.Icon(Icons.Rounded.Payments, null, tint = ZColor.AccentInk, modifier = Modifier.size(22.dp))
        } else AcctLogo(account)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            if (account == cash) Text(cash, color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            else {
                val p = account.trim().split(" ")
                val number = p.last().takeIf { p.size > 1 && it.any { c -> c.isDigit() } }
                val bank = if (number != null) p.dropLast(1).joinToString(" ") else account
                Text(bank, color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                if (number != null) MutedText(number, 13)
            }
        }
        if (arrow) androidx.compose.material3.Icon(Icons.Rounded.ArrowDropDown, null, tint = ZColor.Muted)
    }
}
