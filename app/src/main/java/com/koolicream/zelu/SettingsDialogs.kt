package com.koolicream.zelu

import android.content.Context
import android.os.Build
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Restore
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Dialogs for the app itself: erase, help, problem reports, privacy, backup and restore, theme.

@Composable
internal fun EraseDialog(s: AppState) {
    with(s) {
        if (eraseOpen) Dialog(onDismissRequest = { eraseOpen = false }) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp)) {
                Text("Erase everything?", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                MutedText("Deletes all Zelu data on this phone. Your SMS and saved backup files stay.", 13)
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    Text("Cancel", color = ZColor.Muted, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { eraseOpen = false }.padding(12.dp))
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Warn).clickable {
                        eraseOpen = false
                        scope.launch {
                            withContext(Dispatchers.IO) { Storage.eraseAll(context); ZeluWidget.refresh(context) }
                            Toast.makeText(context, "Erased. Open Zelu again to start fresh.", Toast.LENGTH_LONG).show()
                            (context as? android.app.Activity)?.finishAffinity()
                        }
                    }.padding(horizontal = 22.dp, vertical = 10.dp)) { Text("Erase", color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

@Composable
internal fun HelpDialog(s: AppState) {
    with(s) {
        if (helpOpen) Dialog(onDismissRequest = { helpOpen = false }) {
            // Four fix-it tiles with numbered steps, small chips for everything else, and one answer shown at the top.
            class Fix(val key: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val title: String, val steps: List<String>)
            val fixes = listOf(
                Fix("pay", Icons.Rounded.Receipt, "A payment is wrong", listOf("Open the payment from Home or Spend.", "Tap Change category.",
                    "Pick a category, or Transfer, Lent, Borrowed, Ignore.", "Zelu remembers it for the same payee.")),
                Fix("bank", Icons.Rounded.AccountBalance, "A bank is not read", listOf("Open Menu, then Messages.", "Tap Unrecognised senders.",
                    "Say whether the sender is a bank, a wallet or other.", "Its old and new messages are read from then on.")),
                Fix("acct", Icons.Rounded.CreditCard, "An account is wrong", listOf("Open Menu, then Your accounts.", "Tap the account.",
                    "Choose what it is, and tick any that are the same account.", "Tap Save. Untick later to separate them.")),
                Fix("data", Icons.Rounded.CloudUpload, "Back up or erase", listOf("Open Menu, then Backup and security.", "Back up saves one file to Downloads/Zelu, locked with a password you choose. Keep that password: Zelu cannot recover it.",
                    "Restore loads it on a new phone.", "Erase everything clears Zelu from this phone.")),
            )
            val topics = listOf(
                "Private" to "Your messages and payments stay on this phone. Zelu has no ads and no internet access, so nothing can be sent anywhere.",
                "Rescan" to "New messages are read as they arrive. Menu, Messages, Rescan reads again right now.",
                "Missed SMS" to "Money messages Zelu could not read. Menu, Messages, Missed SMS lets you add one by hand.",
                "Messages stop" to "Android may switch off permissions for an app you open rarely. In Settings, Apps, Zelu, Permissions, turn off 'Remove permissions if app isn't used'.",
                "Duplicates" to "If one payment was reported twice, hide the extra one. Keep both if you really paid twice.",
                "Dues" to "Bills, EMIs and card dues. Tap the circle when paid. AutoPay has its own tab.",
                "Split a payment" to "Open the payment, tap Change category, then Split.",
                "Leave a category out" to "Menu, Money, Categories. It stops counting as spending.",
                "Not covered" to "Zelu reads SMS only. Chat apps, RCS bank chats and bank apps are not visible to it.",
            )
            var hq by remember { mutableStateOf("") }
            var hsel by remember { mutableStateOf("fix:pay") }
            val q = hq.trim().lowercase()
            val fx = fixes.filter { q.isEmpty() || (it.title + " " + it.steps.joinToString(" ")).lowercase().contains(q) }
            val tp = topics.filter { q.isEmpty() || (it.first + " " + it.second).lowercase().contains(q) }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp).heightIn(max = 640.dp)) {
                Text("Help", color = ZColor.Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = hq, onValueChange = { hq = it }, singleLine = true, placeholder = { Text("Search help") }, modifier = Modifier.fillMaxWidth())
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    Spacer(Modifier.height(12.dp))
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(ZColor.Paper).padding(14.dp)) {
                        val f = fixes.firstOrNull { "fix:" + it.key == hsel }
                        val t = topics.firstOrNull { "topic:" + it.first == hsel }
                        if (f != null) {
                            Eyebrow(f.title)
                            f.steps.forEachIndexed { i, st ->
                                Spacer(Modifier.height(10.dp))
                                Row(verticalAlignment = Alignment.Top) {
                                    Box(Modifier.size(22.dp).clip(CircleShape).background(ZColor.Accent), contentAlignment = Alignment.Center) {
                                        Text((i + 1).toString(), color = ZColor.AccentInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Text(st, color = ZColor.InkSoft, fontSize = 14.sp, lineHeight = 20.sp)
                                }
                            }
                        } else if (t != null) {
                            Text(t.first, color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Text(t.second, color = ZColor.InkSoft, fontSize = 14.sp, lineHeight = 20.sp)
                        } else MutedText("Pick a problem or a topic. The answer shows here.", 13)
                    }
                    if (fx.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp)); Eyebrow("Fix something"); Spacer(Modifier.height(8.dp))
                        fx.chunked(2).forEach { pair ->
                            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                pair.forEach { f ->
                                    val on = hsel == "fix:" + f.key
                                    Column(Modifier.weight(1f).heightIn(min = 92.dp).clip(RoundedCornerShape(16.dp)).background(if (on) ZColor.Paper else ZColor.Card)
                                        .border(1.dp, if (on) ZColor.Accent else ZColor.Line, RoundedCornerShape(16.dp))
                                        .clickable { hsel = "fix:" + f.key }.padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                        Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(ZColor.Paper), contentAlignment = Alignment.Center) {
                                            androidx.compose.material3.Icon(f.icon, null, tint = ZColor.Accent, modifier = Modifier.size(18.dp))
                                        }
                                        Spacer(Modifier.height(10.dp))
                                        Text(f.title, color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                    if (tp.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp)); Eyebrow("Good to know"); Spacer(Modifier.height(8.dp))
                        tp.chunked(2).forEach { pair ->
                            Row(Modifier.padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                pair.forEach { (k, _) -> Chip(k, hsel == "topic:" + k) { hsel = "topic:" + k } }
                            }
                        }
                    }
                    if (fx.isEmpty() && tp.isEmpty()) MutedText("Nothing matches that. Try another word, or report it.", 13)
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Still stuck? Report a problem", color = ZColor.Accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { helpOpen = false; reportOpen = true }.padding(vertical = 8.dp))
                    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink).clickable { helpOpen = false }.padding(horizontal = 22.dp, vertical = 10.dp)) {
                        Text("Close", color = ZColor.Paper, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
internal fun ReportAddressDialog(s: AppState) {
    with(s) {
        if (emailOpen) {
            var addr by remember { mutableStateOf(Storage.reportEmail(context)) }
            Dialog(onDismissRequest = { emailOpen = false }) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp)) {
                    Text("Report address", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    MutedText("Problem reports are emailed here.", 12)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(value = addr, onValueChange = { addr = it.trim() }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        Text("Cancel", color = ZColor.Muted, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { emailOpen = false }.padding(12.dp))
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink).clickable {
                            if (addr.contains("@") && addr.contains(".")) { Storage.setReportEmail(context, addr); emailOpen = false }
                            else Toast.makeText(context, "Enter a valid email address", Toast.LENGTH_SHORT).show()
                        }.padding(horizontal = 22.dp, vertical = 10.dp)) { Text("Save", color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
                    }
                }
            }
        }
    }
}

@Composable
internal fun PrivacyDialog(s: AppState) {
    with(s) {
        if (privacyOpen) Dialog(onDismissRequest = { privacyOpen = false }) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp).heightIn(max = 620.dp)) {
                Text("Privacy policy", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                MutedText("Last updated " + PrivacyText.UPDATED, 12)
                Spacer(Modifier.height(10.dp))
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    Text(PrivacyText.SUMMARY, color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PrivacyText.facts.forEach { (t, d) ->
                            Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(ZColor.IncomeSoft).padding(10.dp)) {
                                Text("✓", color = ZColor.Income, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Unspoken)
                                Text(t, color = ZColor.Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(d, color = ZColor.InkSoft, fontSize = 11.sp)
                            }
                        }
                    }
                    PrivacyText.sections.forEach { sec ->
                        Spacer(Modifier.height(12.dp))
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ZColor.Paper).padding(14.dp)) {
                            Text(sec.title, color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            sec.points.forEach { pt ->
                                Row(Modifier.padding(top = 6.dp)) {
                                    Text("•", color = ZColor.Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(8.dp))
                                    Text(pt, color = ZColor.InkSoft, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Box(Modifier.align(Alignment.End).clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                    .clickable { privacyOpen = false }.padding(horizontal = 22.dp, vertical = 10.dp)) {
                    Text("Close", color = ZColor.Paper, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
internal fun CrashDialog(s: AppState) {
    with(s) {
        // After a crash, offer once to send the details. The email opens as a draft; nothing goes out unless the user sends it.
        if (crashAsk) {
            fun seen() { crashAsk = false; scope.launch(Dispatchers.IO) { CrashLog.markSeen(context) } }
            Dialog(onDismissRequest = { seen() }) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp)) {
                    Text("Zelu closed unexpectedly", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    MutedText("Sorry. Sending the error details (no payments or messages) helps fix it.", 13)
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.align(Alignment.End)) {
                        Box(Modifier.clip(RoundedCornerShape(20.dp)).clickable { seen() }.padding(horizontal = 16.dp, vertical = 10.dp)) {
                            Text("Not now", color = ZColor.Muted, fontWeight = FontWeight.SemiBold)
                        }
                        Box(Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                            .clickable { seen(); reportOpen = true }.padding(horizontal = 18.dp, vertical = 10.dp)) {
                            Text("Send report", color = ZColor.Paper, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ReportDialog(s: AppState) {
    with(s) {
        if (reportOpen) {
            var what by remember { mutableStateOf("") }
            var withInfo by remember { mutableStateOf(true) }
            var withMissed by remember { mutableStateOf(false) }
            val crash = remember { CrashLog.last(context) }
            var withCrash by remember { mutableStateOf(crash != null) }
            val to = remember { Storage.reportEmail(context) }
            Dialog(onDismissRequest = { reportOpen = false }) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp)
                    .heightIn(max = 620.dp).verticalScroll(rememberScrollState())) {
                    Text("Report a problem", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    MutedText("Opens an email to $to; you press Send.", 12)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(value = what, onValueChange = { what = it }, placeholder = { Text("What went wrong?") },
                        minLines = 3, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    @Composable fun tick(on: Boolean, title: String, sub: String, flip: () -> Unit) {
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).checkRow(on) { flip() }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(22.dp).clip(RoundedCornerShape(6.dp)).background(if (on) ZColor.Accent else ZColor.Paper)
                                .border(1.dp, if (on) ZColor.Accent else ZColor.Line, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                                if (on) Text("✓", color = ZColor.AccentInk, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Unspoken)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(title, color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text(sub, color = ZColor.Muted, fontSize = 12.sp)
                            }
                        }
                    }
                    tick(withInfo, "App details", "Version, phone model and counts. No names or amounts.") { withInfo = !withInfo }
                    tick(withMissed, "Messages Zelu couldn't read", "Up to 3 message texts. Long numbers and UPI names are hidden; amounts stay.") { withMissed = !withMissed }
                    if (crash != null) tick(withCrash, "Details of the last crash", "Technical error text only. No payments.") { withCrash = !withCrash }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        Text("Cancel", color = ZColor.Muted, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { reportOpen = false }.padding(12.dp))
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink).clickable {
                            scope.launch {
                                val sb = StringBuilder(what.trim()).append("\n\n")
                                if (withInfo) {
                                    val ver = try { context.packageManager.getPackageInfo(context.packageName, 0).versionName } catch (_: Exception) { "?" }
                                    sb.append("--\nZelu ").append(ver).append(" · parser ").append(Storage.PARSER_VERSION)
                                        .append("\nAndroid ").append(Build.VERSION.RELEASE).append(" · ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL)
                                        .append("\n").append(rows.size).append(" payments · ").append(reminders.size).append(" dues · ")
                                        .append(registry.size).append(" accounts decided\n")
                                }
                                if (withMissed) {
                                    val miss = withContext(Dispatchers.IO) { Ingest.unreadable(context, 3) }
                                    miss.forEach { sb.append("\n[").append(it.address).append("] ").append(Redact.message(it.body)).append("\n") }
                                }
                                if (withCrash && crash != null) sb.append("\n--\nLast crash:\n").append(crash).append("\n")
                                val mail = android.content.Intent(android.content.Intent.ACTION_SENDTO, android.net.Uri.parse("mailto:"))
                                    .putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf(to))
                                    .putExtra(android.content.Intent.EXTRA_SUBJECT, "Zelu problem report")
                                    .putExtra(android.content.Intent.EXTRA_TEXT, sb.toString())
                                try { context.startScreen(android.content.Intent.createChooser(mail, "Send report")); reportOpen = false }
                                catch (_: Exception) { Toast.makeText(context, "No email app found on this phone", Toast.LENGTH_LONG).show() }
                            }
                        }.padding(horizontal = 22.dp, vertical = 10.dp)) { Text("Write email", color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
                    }
                }
            }
        }
    }
}

@Composable
internal fun BackupPasswordDialog(s: AppState) {
    with(s) {
        if (backupPwOpen) {
            var pw by remember { mutableStateOf("") }
            var pw2 by remember { mutableStateOf("") }
            Dialog(onDismissRequest = { backupPwOpen = false }) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp)) {
                    Text(if (Backup.hasPassword(context)) "Change backup password" else "Set a backup password", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    MutedText("Locks your backups. If you forget it, they can't be opened.", 12)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(value = pw, onValueChange = { pw = it }, singleLine = true, label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = pw2, onValueChange = { pw2 = it }, singleLine = true, label = { Text("Type it again") },
                        visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        Text("Cancel", color = ZColor.Muted, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { backupPwOpen = false }.padding(12.dp))
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink).clickable {
                            when {
                                pw.length < BackupCrypto.MIN_PASSWORD -> Toast.makeText(context, "Use at least ${BackupCrypto.MIN_PASSWORD} characters", Toast.LENGTH_SHORT).show()
                                pw != pw2 -> Toast.makeText(context, "The two passwords don't match", Toast.LENGTH_SHORT).show()
                                else -> {
                                    Backup.setPassword(context, pw)
                                    backupPwOpen = false
                                    if (backupAfterPw) { backupAfterPw = false; backupNow() }
                                    else Toast.makeText(context, "Backup password saved", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }.padding(horizontal = 22.dp, vertical = 10.dp)) { Text("Save", color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
                    }
                }
            }
        }
    }
}

@Composable
internal fun RestoreDialog(s: AppState) {
    with(s) {
        restoreUri?.let { uri ->
            var needPw by remember(uri) { mutableStateOf(false) }
            var pw by remember(uri) { mutableStateOf("") }
            var wrong by remember(uri) { mutableStateOf(false) }
            Dialog(onDismissRequest = { restoreUri = null }) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp)) {
                    Text("Restore this backup?", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    MutedText("Replaces everything on this phone with the file's data.", 13)
                    if (needPw) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(value = pw, onValueChange = { pw = it; wrong = false }, singleLine = true, label = { Text("Backup password") },
                            isError = wrong, supportingText = { if (wrong) Text("That password doesn't open this backup") },
                            visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        Text("Cancel", color = ZColor.Muted, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { restoreUri = null }.padding(12.dp))
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink).clickable {
                            scope.launch {
                                val r = withContext(Dispatchers.IO) {
                                    try {
                                        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                                        if (text == null) Backup.Result.NOT_A_BACKUP else Backup.restoreFrom(context, text, pw)
                                    } catch (_: Exception) { Backup.Result.NOT_A_BACKUP }
                                }
                                when (r) {
                                    Backup.Result.NEEDS_PASSWORD -> needPw = true
                                    Backup.Result.WRONG_PASSWORD -> { needPw = true; wrong = true }
                                    else -> {
                                        restoreUri = null
                                        val ok = r == Backup.Result.OK
                                        if (ok) {
                                            val sp = context.getSharedPreferences("zelu_theme", android.content.Context.MODE_PRIVATE)
                                            ZTheme.palette = paletteIndex(sp)
                                            if (sp.contains("dark")) ZTheme.dark = sp.getBoolean("dark", false)
                                            reload()
                                        }
                                        Toast.makeText(context, if (ok) "Backup restored" else "That isn't a Zelu backup file", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }.padding(horizontal = 22.dp, vertical = 10.dp)) { Text("Restore", color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ThemeDialog(s: AppState) {
    with(s) {
        if (themeOpen) Dialog(onDismissRequest = { themeOpen = false }) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(vertical = 10.dp)
                .heightIn(max = 620.dp).verticalScroll(rememberScrollState())) {
                Text("Theme", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp))
                PALETTES.forEachIndexed { i, p ->
                    val set = if (ZTheme.dark) p.dark else p.light
                    val sel = i == ZTheme.palette
                    Row(
                        Modifier.fillMaxWidth().choice(sel) {
                            ZTheme.palette = i
                            savePalette(context.getSharedPreferences("zelu_theme", android.content.Context.MODE_PRIVATE), i)
                        }.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(Modifier.clip(RoundedCornerShape(10.dp)).border(1.dp, ZColor.Line, RoundedCornerShape(10.dp))) {
                            listOf(set.paper, set.card, set.ink, set.accent).forEach { c ->
                                Box(Modifier.size(width = 24.dp, height = 32.dp).background(Color(c)))
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Text(p.name, color = ZColor.Ink, fontSize = 15.sp,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
                        if (sel) Text("✓", color = ZColor.Accent, fontWeight = FontWeight.Bold, modifier = Unspoken)
                    }
                }
                Text("Use the sun / moon button for light or dark.", color = ZColor.Muted, fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp))
            }
        }
    }
}
