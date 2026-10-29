package com.koolicream.zelu

import androidx.compose.material.icons.Icons
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
                Text("Unrecognised senders", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                MutedText("Senders that look like payments. Say what each one is; add only ones you trust.", 12)
                Spacer(Modifier.height(10.dp))
                val sl = senderList
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    when {
                        sl == null -> MutedText("Looking through your messages…", 13)
                        sl.isEmpty() -> MutedText("None. Every payment sender is already read.", 13)
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
                                    Text("Ignore", color = ZColor.Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable {
                                        scope.launch(Dispatchers.IO) { Storage.markSmsHandled(context, "sender|" + u.address.uppercase()) }
                                        senderList = senderList?.filter { it.address != u.address }
                                    })
                                    // Opens an email draft with only the sender id, so the bank can be added for everyone. The user sends it.
                                    Text("Tell Zelu", color = ZColor.Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable {
                                        val mail = android.content.Intent(android.content.Intent.ACTION_SENDTO, android.net.Uri.parse("mailto:"))
                                            .putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf(Storage.reportEmail(context)))
                                            .putExtra(android.content.Intent.EXTRA_SUBJECT, "Zelu: please add this bank")
                                            .putExtra(android.content.Intent.EXTRA_TEXT, "Sender id: " + u.address + "\nBank name: (please type it here)\n\nNo message text is included.")
                                        try { context.startActivity(android.content.Intent.createChooser(mail, "Tell Zelu")) }
                                        catch (_: Exception) { Toast.makeText(context, "No email app found on this phone", Toast.LENGTH_LONG).show() }
                                    })
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.align(Alignment.End).clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                    .clickable { sendersOpen = false }.padding(horizontal = 22.dp, vertical = 10.dp)) {
                    Text("Close", color = ZColor.Paper, fontWeight = FontWeight.SemiBold)
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
                Text("Read everything again?", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                MutedText("Reads every message again. Your edits and added payments stay.", 13)
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    Text("Cancel", color = ZColor.Muted, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { fullConfirm = false }.padding(12.dp))
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink).clickable { fullConfirm = false; rescan(full = true, notify = true) }
                        .padding(horizontal = 22.dp, vertical = 10.dp)) { Text("Read again", color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
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
                Text("Data check", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                MutedText("Parser version ${Storage.PARSER_VERSION} · ${rows.size} transactions stored", 12)
                Spacer(Modifier.height(10.dp))
                val c = check
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    if (c == null) MutedText("Checking your bank alerts…", 13) else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Fact("Bank SMS", c.bankSms.toString()); Fact("Payments", c.txns.toString()); Fact("Balances", c.balanceAlerts.toString())
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Fact("Reminders", c.reminders.toString())
                            Fact("Missed", c.unread.toString(), if (c.unread > 0) ZColor.Warn else ZColor.Income)
                            Fact("Hidden accts", Storage.hiddenAccounts(context).size.toString())
                        }
                        Spacer(Modifier.height(14.dp))
                        Eyebrow("Do balances add up?")
                        Spacer(Modifier.height(6.dp))
                        if (c.gaps.isEmpty()) MutedText("Yes. Every account's balances add up.", 13)
                        else {
                            MutedText("A jump means a message is missing in between.", 12)
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
                    Text("Close", color = ZColor.Paper, fontWeight = FontWeight.SemiBold)
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
                Text("Missed messages", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                MutedText("Money messages Zelu couldn't read. Add the real ones as payments.", 12)
                Spacer(Modifier.height(10.dp))
                val list = unreadList
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    when {
                        list == null -> MutedText("Looking through your messages…", 13)
                        list.isEmpty() -> MutedText("Nothing missed. Every money message was understood.", 13)
                        else -> list.forEach { m ->
                            Column(Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(12.dp))
                                .background(ZColor.Paper).padding(12.dp)) {
                                MutedText(m.address + " · " + prettyDate(Dates.fmtMs(m.date)), 11)
                                Spacer(Modifier.height(4.dp))
                                Text(m.body, color = ZColor.Ink, fontSize = 12.sp, maxLines = 6, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text("Add as payment", color = ZColor.Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable { addFor = m })
                                    Text("Not a payment", color = ZColor.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.clickable {
                                            scope.launch(Dispatchers.IO) { Storage.markSmsHandled(context, Ingest.smsKey(m)) }
                                            unreadList = unreadList?.filter { it !== m }
                                            noteMissed(unreadList?.size ?: 0)
                                        })
                                    Text("Share", color = ZColor.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.clickable {
                                            val send = android.content.Intent(android.content.Intent.ACTION_SEND)
                                                .setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, m.body)
                                            try { context.startActivity(android.content.Intent.createChooser(send, "Share message")) } catch (_: Exception) {}
                                        })
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Box(Modifier.align(Alignment.End).clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                    .clickable { unreadOpen = false }.padding(horizontal = 22.dp, vertical = 10.dp)) {
                    Text("Close", color = ZColor.Paper, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
internal fun AddPaymentDialog(s: AppState) {
    with(s) {
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
                    Text("Add as payment", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    MutedText(sms.body.take(160) + if (sms.body.length > 160) "…" else "", 11)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        KindTab("spending", "Spent", !isIn) { isIn = false }
                        KindTab("income", "Received", isIn) { isIn = true }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = amtText, onValueChange = { v -> amtText = v.filter { it.isDigit() || it == '.' } }, singleLine = true,
                        placeholder = { Text("Amount ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = payee, onValueChange = { payee = it }, singleLine = true,
                        placeholder = { Text("Paid to / from") }, modifier = Modifier.fillMaxWidth())
                    if (sugg.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        ChipScroll { sugg.take(6).forEach { sName -> Chip(sName, payee == sName) { payee = sName } } }
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(Modifier.clip(RoundedCornerShape(20.dp)).border(1.dp, ZColor.Line, RoundedCornerShape(20.dp))
                        .clickable { pickDate(context, day) { day = it } }.padding(horizontal = 16.dp, vertical = 9.dp)) {
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
                            Text("Do the same for similar messages", color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Zelu learns this message's form and reads the next ones itself", color = ZColor.Muted, fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        Text("Cancel", color = ZColor.Muted, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { addFor = null }.padding(12.dp))
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink).clickable {
                            val amount = amtText.toDoubleOrNull() ?: 0.0
                            if (amount > 0) {
                                val dir = if (isIn) "credit" else "debit"
                                val name = payee.trim().ifBlank { null }
                                val acct = Parser.accountFor(sms.body, sms.address, dir)
                                val id = "man-" + Ingest.smsKey(sms).hashCode().toUInt().toString(16)
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        val own = Storage.splitList(Storage.loadSettings(context).ownAccounts)
                                        val c = Classifier.classify(Txn(id, day, amount, dir, name, null, sms.body, sms.address, acct), own)
                                        Storage.addManualTxn(context, Row(id, day, amount, dir, name, null, sms.body, sms.address, acct, c.kind, c.category, sms.date), Ingest.smsKey(sms))
                                        if (learnIt) Taught.learn(context, sms.body, sms.address, name, dir)
                                    }
                                    addFor = null
                                    if (learnIt) rescan()
                                    val l = withContext(Dispatchers.IO) { Ingest.unreadable(context) }
                                    unreadList = l; noteMissed(l.size)
                                    reload()
                                    Toast.makeText(context, "Added to your payments", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }.padding(horizontal = 22.dp, vertical = 10.dp)) { Text("Add", color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
                    }
                }
            }
        }
    }
}
