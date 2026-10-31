package com.koolicream.zelu

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Which payments are split with people (the rest of the split ones are split into categories), for the mark on each row. */
object SplitMarks {
    var people by mutableStateOf<Set<String>>(emptySet())
    // The whole payment behind each split, so a split with people still shows as one payment of its full amount.
    var totals by mutableStateOf<Map<String, Long>>(emptyMap())
    fun update(rows: List<Row>) {
        people = rows.filter { it.kind == Kind.LENT && it.id.contains('#') }.map { it.id.substringBefore('#') }.toSet()
        totals = rows.filter { it.id.contains('#') }.groupBy { it.id.substringBefore('#') }.mapValues { (_, rs) -> rs.sumOf { it.paise } }
    }
    /** For lists: a split with people is one row (your share's row, shown with the whole amount); the others' shares stay in People. */
    fun forDisplay(list: List<Row>): List<Row> {
        val shown = list.filter { it.kind == Kind.EXPENSE && it.id.contains('#') && it.id.substringBefore('#') in people }.map { it.id.substringBefore('#') }.toSet()
        return if (shown.isEmpty()) list else list.filterNot { it.kind == Kind.LENT && it.id.contains('#') && it.id.substringBefore('#') in shown }
    }
    /** The whole amount when [r] is your share of a split with people, else null. */
    fun wholeOf(r: Row): Long? = if (r.kind == Kind.EXPENSE && r.id.contains('#') && r.id.substringBefore('#') in people) totals[r.id.substringBefore('#')] else null
}

// Split asks first: share it with people, or divide it into categories (the older split).
@Composable internal fun SplitChoiceDialog(s: AppState) {
    val t = s.splitChoice ?: return
    Dialog(onDismissRequest = { s.splitChoice = null }) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(ZColor.Card).padding(18.dp)) {
            Text(stringResource(R.string.split_this_payment), color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            MutedText(niceName(t.counterparty ?: t.category) + " · " + money(t.amount), 12)
            @Composable fun option(title: String, sub: String, go: () -> Unit) {
                Spacer(Modifier.height(10.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ZColor.Tile)
                    .border(1.dp, ZColor.Line, RoundedCornerShape(16.dp)).clickable { go() }.padding(14.dp)) {
                    Text(title, color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    MutedText(sub, 12)
                }
            }
            option(stringResource(R.string.split_with_people), stringResource(R.string.split_with_people_sub)) { s.splitChoice = null; s.peopleSplitFor = t }
            option(stringResource(R.string.split_into_categories), stringResource(R.string.split_into_categories_sub)) { s.splitChoice = null; s.splitFor = t }
        }
    }
}

private const val YOU = "\u0000you"

private fun contactName(context: Context, uri: Uri): String? = try {
    context.contentResolver.query(uri, arrayOf(ContactsContract.Contacts.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) c.getString(0)?.trim()?.takeIf { it.isNotEmpty() } else null
    }
} catch (e: Exception) { CrashLog.warn("PeopleSplit.contactName", e); null }

// Splitting one payment with people: who (recent people, groups, or one picked from contacts), how (equally, by amount or
// by share), and a line that says what you pay and what the others owe. Saving turns their shares into money lent to them.
@OptIn(ExperimentalLayoutApi::class)
@Composable internal fun PeopleSplitDialog(s: AppState) {
    val t = s.peopleSplitFor ?: return
    val context = LocalContext.current
    val total = t.amount
    val people = remember(t.id) { mutableStateListOf(YOU) }
    val typed = remember(t.id) { mutableStateMapOf<String, String>() }
    var mode by remember(t.id) { mutableStateOf("eq") }
    var groupName by remember(t.id) { mutableStateOf("") }
    var naming by remember(t.id) { mutableStateOf(false) }
    val youLabel = stringResource(R.string.you_label)
    // People you have shared money with before, and everyone in your groups, so a split is a couple of taps.
    val recent = remember(s.rows, s.groups) {
        (s.rows.filter { it.kind == Kind.LENT || it.kind == Kind.BORROWED }.mapNotNull { it.counterparty?.takeIf { c -> c.isNotBlank() } } +
            s.groups.values.flatten()).groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.map { it.key }.take(10)
    }
    val extra = remember(t.id) { mutableStateListOf<String>() }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        val name = uri?.let { contactName(context, it) } ?: return@rememberLauncherForActivityResult
        if (name !in people) { people.add(name); typed.clear() }
        if (name !in recent && name !in extra) extra.add(name)
    }
    fun toggle(name: String) { if (name in people) people.remove(name) else people.add(name); typed.clear() }
    val n = people.size
    fun share(p: String): Double = when (mode) {
        "amt" -> typed[p]?.toDoubleOrNull() ?: Math.round(total / n * 100) / 100.0
        "pct" -> total * ((typed[p]?.toDoubleOrNull() ?: (100.0 / n)) / 100.0)
        else -> total / n
    }
    val shares = people.associateWith { share(it) }
    val left = total - shares.values.sum()
    val ok = n >= 2 && kotlin.math.abs(left) < 0.5 && shares.values.all { it >= 0 }
    val chosenGroup = s.groups.entries.firstOrNull { (_, m) -> m.isNotEmpty() && m.toSet() == people.filter { it != YOU }.toSet() }?.key

    Dialog(onDismissRequest = { s.peopleSplitFor = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxSize().background(ZColor.Paper).verticalScroll(rememberScrollState()).padding(start = 18.dp, end = 18.dp, top = 28.dp, bottom = 32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TopIcon(stringResource(R.string.back), onClick = { s.peopleSplitFor = null }) { Text("‹", color = ZColor.Ink, fontSize = 22.sp, modifier = Unspoken) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.split_amount_title, money(total)), color = ZColor.Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    MutedText(niceName(t.counterparty ?: t.category) + " · " + prettyDate(t.date), 12)
                }
            }
            Spacer(Modifier.height(14.dp))
            Panel {
                Eyebrow(stringResource(R.string.split_with_label))
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    s.groups.forEach { (g, members) ->
                        val on = g == chosenGroup
                        SplitChip("👥 $g", on) {
                            if (on) people.removeAll(members) else members.forEach { if (it !in people) people.add(it) }
                            typed.clear()
                        }
                    }
                    (recent + extra).distinct().forEach { p -> SplitChip(niceName(p), p in people) { toggle(p) } }
                    SplitChip(stringResource(R.string.from_contacts), false) {
                        try { pick.launch(null) } catch (e: Exception) { CrashLog.warn("PeopleSplit.pick", e) }
                    }
                }
                // A few people picked together can be kept as a group, to pick them in one tap next time.
                if (n >= 3 && chosenGroup == null) {
                    Spacer(Modifier.height(10.dp))
                    if (!naming) Text(stringResource(R.string.save_as_group), color = ZColor.Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { naming = true }.padding(vertical = 4.dp))
                    else Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = groupName, onValueChange = { groupName = it }, singleLine = true, modifier = Modifier.weight(1f),
                            placeholder = { Text(stringResource(R.string.group_name)) })
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.save), color = ZColor.Accent, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(10.dp))
                            .clickable { if (groupName.isNotBlank()) { s.saveGroup(groupName.trim(), people.filter { it != YOU }); naming = false } }.padding(10.dp))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Panel {
                Eyebrow(stringResource(R.string.split_how))
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(ZColor.Tile).padding(3.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    listOf("eq" to R.string.split_equally, "amt" to R.string.split_by_amount, "pct" to R.string.split_by_share).forEach { (k, label) ->
                        Box(Modifier.weight(1f).clip(RoundedCornerShape(15.dp)).background(if (mode == k) ZColor.Card else ZColor.Tile)
                            .clickable { mode = k; typed.clear() }.padding(vertical = 9.dp), contentAlignment = Alignment.Center) {
                            Text(stringResource(label), color = if (mode == k) ZColor.Ink else ZColor.Muted, fontSize = 13.sp,
                                fontWeight = if (mode == k) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
                        }
                    }
                }
                people.forEach { p ->
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val label = if (p == YOU) youLabel else niceName(p)
                        Box(Modifier.size(34.dp).clip(CircleShape).background(if (p == YOU) ZColor.Accent else ZColor.Tile), contentAlignment = Alignment.Center) {
                            Text(label.take(1).uppercase(), color = if (p == YOU) ZColor.Paper else ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(label, color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (mode == "eq") Text(money(shares[p] ?: 0.0), color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        else {
                            val shown = typed[p] ?: if (mode == "amt") fmtPlain(Math.round(total / n * 100) / 100.0) else fmtPlain(Math.round(1000.0 / n) / 10.0)
                            OutlinedTextField(value = shown, onValueChange = { v -> typed[p] = v.filter { it.isDigit() || it == '.' } }, singleLine = true,
                                modifier = Modifier.width(96.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                suffix = { Text(if (mode == "pct") "%" else "") })
                        }
                        if (p != YOU) Text("✕", color = ZColor.Muted, fontSize = 15.sp, modifier = Modifier.clip(CircleShape).clickable { people.remove(p); typed.clear() }.padding(8.dp))
                        else Spacer(Modifier.width(31.dp))
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    when {
                        n < 2 -> MutedText(stringResource(R.string.split_add_someone), 13)
                        kotlin.math.abs(left) >= 0.5 -> Text(if (left > 0) stringResource(R.string.left_to_assign, money(left)) else stringResource(R.string.too_much, money(-left)),
                            color = ZColor.Warn, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        else -> {
                            Text(stringResource(R.string.you_pay_amount, money(shares[YOU] ?: 0.0)), color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.others_owe_amount, money(total - (shares[YOU] ?: 0.0))), color = ZColor.Income, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Box(Modifier.clip(RoundedCornerShape(20.dp)).background(if (ok) ZColor.Ink else ZColor.Line).clickable {
                    if (ok) {
                        s.savePeopleSplit(t, shares[YOU] ?: 0.0, people.filter { it != YOU }.map { it to (shares[it] ?: 0.0) }, chosenGroup)
                        s.peopleSplitFor = null
                        android.widget.Toast.makeText(context, context.getString(R.string.split_saved_people), android.widget.Toast.LENGTH_SHORT).show()
                    }
                }.padding(horizontal = 24.dp, vertical = 11.dp)) {
                    Text(stringResource(R.string.save), color = if (ok) ZColor.Paper else ZColor.Muted, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable private fun SplitChip(label: String, on: Boolean, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(18.dp)).background(if (on) ZColor.Accent.copy(alpha = 0.16f) else ZColor.Tile)
        .border(1.dp, if (on) ZColor.Accent else ZColor.Line, RoundedCornerShape(18.dp)).clickable { onClick() }.padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(label, color = ZColor.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}
