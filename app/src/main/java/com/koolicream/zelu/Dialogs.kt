package com.koolicream.zelu

import androidx.compose.material.icons.Icons
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.KeyboardType
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable internal fun DrillDialog(title: String, rows: List<Row>, onRowClick: (Row) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp),
        ) {
            Text(title, color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            val total = rows.sumOf { it.amount }
            MutedText("${rows.size} ${if (rows.size == 1) "entry" else "entries"} · ${money(total)}", 12)
            Spacer(Modifier.height(10.dp))
            // A category or a search can hold hundreds of payments: only the ones on screen are drawn.
            val sorted = remember(rows) { rows.newestFirst() }
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                items(sorted) { TxnRow(it, false, onRowClick) }
            }
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier.align(Alignment.End).clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                    .clickable { onDismiss() }.padding(horizontal = 22.dp, vertical = 10.dp),
            ) { Text("Close", color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable internal fun BudgetsDialog(
    cats: List<String>, budgets: Map<String, Double>, fixed: Map<String, Double>,
    onSet: (String, Double) -> Unit, onFixed: (String, Double) -> Unit, onDismiss: () -> Unit,
) {
    val names = remember { mutableStateListOf<String>().apply { addAll(cats) } }
    val drafts = remember {
        mutableStateMapOf<String, String>().apply { cats.forEach { c -> (budgets[c] ?: fixed[c])?.let { put(c, fmtPlain(it)) } } }
    }
    val flags = remember { mutableStateMapOf<String, Boolean>().apply { fixed.keys.forEach { put(it, true) } } }
    var newName by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp)) {
            Text("Budgets", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            MutedText("A monthly limit per category. Mark same-every-month costs as Fixed.", 12)
            Spacer(Modifier.height(12.dp))
            Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
                names.forEach { c ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        MerchantTile("", c, 30.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(c, color = ZColor.Ink, fontSize = 14.sp, modifier = Modifier.weight(1f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Chip("Fixed", flags[c] == true) { flags[c] = flags[c] != true }
                        Spacer(Modifier.width(6.dp))
                        OutlinedTextField(
                            value = drafts[c] ?: "",
                            onValueChange = { v -> drafts[c] = v.filter { it.isDigit() || it == '.' } },
                            singleLine = true, placeholder = { Text("—") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(100.dp),
                        )
                    }
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newName, onValueChange = { newName = it }, singleLine = true,
                        placeholder = { Text("Add your own (e.g. Maid)") }, modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Chip("Add", false, filled = true) {
                        val n = newName.trim()
                        if (n.isNotEmpty() && n !in names) { names.add(0, n); flags[n] = true; newName = "" }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier.align(Alignment.End).clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                    .clickable {
                        names.forEach { c ->
                            val a = drafts[c]?.replace(",", "")?.toDoubleOrNull() ?: 0.0
                            if ((budgets[c] ?: 0.0) != a) onSet(c, a)
                            val fx = flags[c] == true && a > 0
                            if (fx && (fixed[c] ?: 0.0) != a) onFixed(c, a)
                            if (!fx && fixed.containsKey(c)) onFixed(c, 0.0)
                        }
                        onDismiss()
                    }.padding(horizontal = 22.dp, vertical = 10.dp),
            ) { Text("Done", color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable internal fun SavingsDialog(rows: List<Row>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val saved = remember { Storage.loadPlan(context) }
    var sip by remember { mutableStateOf(saved["sip"] ?: "") }
    var emi by remember { mutableStateOf(saved["emi"] ?: "") }
    var goal by remember { mutableStateOf(saved["goal"] ?: "") }
    var years by remember { mutableStateOf(saved["years"] ?: "5") }
    var ret by remember { mutableStateOf(saved["ret"] ?: "12") }
    var emiOpen by remember { mutableStateOf((saved["emi"] ?: "").isNotBlank()) }
    val months = (1..3).map { Dates.shiftMonth(Dates.todayMonth(), -it) }
    val mi = Analytics.monthlySpendIncome(rows, months, "All")
    val avgIn = mi.sumOf { it.third } / 3.0
    val avgSpent = mi.sumOf { it.second } / 3.0
    val left = avgIn - avgSpent
    fun num(t: String) = t.replace(",", "").toDoubleOrNull() ?: 0.0
    val sipN = num(sip); val emiN = num(emi); val goalN = num(goal)
    val yrs = num(years).coerceIn(1.0, 40.0); val rate = num(ret).coerceIn(0.0, 30.0)
    val free = left - sipN - emiN
    val n = (yrs * 12).toInt(); val r = rate / 1200.0
    val factor = sipFactor(n, r)
    val fv = sipN * factor
    val invested = sipN * n
    val needed = if (goalN > 0 && factor > 0) goalN / factor else 0.0
    val goalMonths = if (goalN > 0 && sipN > 0) (1..600).firstOrNull { sipN * sipFactor(it, r) >= goalN } else null
    fun save() {
        Storage.savePlan(context, mapOf("sip" to sip, "emi" to emi, "goal" to goal, "years" to years, "ret" to ret))
        onDismiss()
    }
    val sliderMax = (Math.ceil(maxOf(left, sipN, 10000.0) / 1000.0) * 1000.0).toFloat()
    @Composable fun field(label: String, v: String, set: (String) -> Unit) {
        OutlinedTextField(
            value = v, onValueChange = { x -> set(x.filter { it.isDigit() || it == '.' }) }, singleLine = true,
            label = { Text(label) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
    }
    Dialog(onDismissRequest = { save() }, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(ZColor.Paper)) {
            Column(
                Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 18.dp, end = 18.dp, top = 40.dp, bottom = 28.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Savings plan", color = ZColor.Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f))
                    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink).clickable { save() }
                        .padding(horizontal = 22.dp, vertical = 10.dp)) {
                        Text("Done", color = ZColor.Paper, fontWeight = FontWeight.SemiBold)
                    }
                }

                // 1. This month's picture
                Panel {
                    Eyebrow("Every month")
                    Spacer(Modifier.height(8.dp))
                    Text("You earn " + inr(avgIn) + " and spend " + inr(avgSpent) + ".", color = ZColor.Ink,
                        fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(if (left >= 0) inr(left) + " is left to save or invest." else "You overspend by " + inr(-left) + ".",
                        color = if (left >= 0) ZColor.Income else ZColor.Warn, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    if (avgIn > 0 && left >= 0) {
                        val sp = (avgSpent / avgIn).toFloat().coerceIn(0.001f, 0.999f)
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))) {
                            Box(Modifier.weight(sp).fillMaxHeight().background(ZColor.InkSoft))
                            Box(Modifier.weight(1f - sp).fillMaxHeight().background(ZColor.Income))
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            MutedText("Spent " + Math.round(sp * 100) + "%", 12)
                            MutedText("Left " + Math.round((1f - sp) * 100) + "%", 12)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    MutedText("Average of your last 3 months, EMIs included.", 12)
                }

                // 2. SIP
                Panel {
                    Eyebrow("Your SIP")
                    Spacer(Modifier.height(6.dp))
                    Text(inr(sipN) + " a month", color = ZColor.Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    androidx.compose.material3.Slider(
                        value = sipN.toFloat().coerceIn(0f, sliderMax),
                        onValueChange = { sip = (Math.round(it / 1000f) * 1000).toString() },
                        valueRange = 0f..sliderMax, steps = (sliderMax / 1000f).toInt() - 1,
                        colors = androidx.compose.material3.SliderDefaults.colors(
                            thumbColor = ZColor.Accent, activeTrackColor = ZColor.Accent,
                            inactiveTrackColor = ZColor.Line, activeTickColor = Color.Transparent,
                            inactiveTickColor = Color.Transparent),
                    )
                    Text("Still free after SIP" + (if (emiN > 0) " and EMI" else "") + ": " + inr(free),
                        color = if (free >= 0) ZColor.Income else ZColor.Warn, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    if (free < 0) MutedText("That is more than you have left each month.", 12)
                    Spacer(Modifier.height(12.dp))
                    if (!emiOpen) {
                        Text("+ Planning a loan? Add an EMI", color = ZColor.Accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { emiOpen = true })
                    } else {
                        field("New EMI to plan for (₹ / month)", emi) { emi = it }
                        Spacer(Modifier.height(4.dp))
                        MutedText("Only for a new loan; current EMIs are already counted.", 12)
                    }
                }

                // 3. What it becomes
                Panel {
                    Eyebrow("What it becomes")
                    Spacer(Modifier.height(6.dp))
                    if (sipN > 0) {
                        Text(inr(fv), color = ZColor.Ink, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        MutedText("in " + fmtPlain(yrs) + " years at " + fmtPlain(rate) + "% a year", 13)
                        Spacer(Modifier.height(12.dp))
                        val inv = (invested / fv).toFloat().coerceIn(0.001f, 0.999f)
                        Row(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))) {
                            Box(Modifier.weight(inv).fillMaxHeight().background(ZColor.InkSoft))
                            Box(Modifier.weight(1f - inv).fillMaxHeight().background(ZColor.Accent))
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(ZColor.InkSoft))
                                Spacer(Modifier.width(6.dp)); MutedText("You put in " + inr(invested), 12)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(ZColor.Accent))
                                Spacer(Modifier.width(6.dp)); MutedText("Growth " + inr(fv - invested), 12)
                            }
                        }
                    } else MutedText("Move the SIP slider to see how it grows.", 13)
                    Spacer(Modifier.height(14.dp))
                    MutedText("Years", 12); Spacer(Modifier.height(6.dp))
                    ChipScroll { listOf(3, 5, 10, 15).forEach { y -> Chip("$y yr", num(years) == y.toDouble(), true) { years = y.toString() } } }
                    Spacer(Modifier.height(10.dp))
                    MutedText("Yearly return", 12); Spacer(Modifier.height(6.dp))
                    ChipScroll { listOf(8, 10, 12, 15).forEach { p -> Chip("$p%", num(ret) == p.toDouble(), true) { ret = p.toString() } } }
                    Spacer(Modifier.height(10.dp))
                    MutedText("Returns are an assumption, not a promise.", 11)
                }

                // 4. Goal
                Panel {
                    Eyebrow("Goal (optional)")
                    Spacer(Modifier.height(8.dp))
                    field("Goal amount (₹), e.g. a down payment", goal) { goal = it }
                    if (goalN > 0) {
                        Spacer(Modifier.height(12.dp))
                        Text("To reach " + inr(goalN) + " in " + fmtPlain(yrs) + " years you need " + inr(needed) + " a month.",
                            color = ZColor.Ink, fontSize = 15.sp)
                        Spacer(Modifier.height(6.dp))
                        when {
                            sipN <= 0 -> MutedText("Set a SIP above to see how long it takes.", 13)
                            sipN >= needed - 0.5 -> Text(
                                "Your " + inr(sipN) + " SIP gets you there in " + (goalMonths?.let { yrMo(it) } ?: "time") + " ✓",
                                color = ZColor.Income, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            else -> Text(
                                inr(needed - sipN) + " short each month. At your SIP it takes " +
                                    (goalMonths?.let { yrMo(it) } ?: "over 50 years") + ".",
                                color = ZColor.Warn, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        if (needed > left - emiN) {
                            Spacer(Modifier.height(4.dp))
                            MutedText("More than you have left each month. Add years or lower the goal.", 12)
                        }
                    }
                }
            }
        }
    }
}

@Composable internal fun PersonCard(person: String, total: Double, bal: Double, color: Color, verb: String, onClick: () -> Unit) {
    val settled = bal <= 0.0
    Column(
        Modifier.width(150.dp).clip(RoundedCornerShape(16.dp)).background(ZColor.Paper)
            .border(1.dp, ZColor.Line, RoundedCornerShape(16.dp)).clickable { onClick() }.padding(12.dp),
    ) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(17.dp)).background(color.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            Text(person.trim().take(1).uppercase(), color = color, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text(niceName(person), color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        MutedText(verb, 11)
        Spacer(Modifier.height(4.dp))
        Text(if (settled) "Settled ✓" else money(bal), color = if (settled) ZColor.Muted else color,
            fontSize = 16.sp, fontWeight = FontWeight.Bold)
        if (!settled && bal < total) MutedText("of ${money(total)}", 11)
    }
}

@Composable internal fun NewCategoryDialog(onCreate: (String, String, Long) -> Unit, onDismiss: () -> Unit) {
    val colors = listOf(0xFFE57373, 0xFFF4A261, 0xFFE9C46A, 0xFF81B29A, 0xFF4DB6AC, 0xFF64B5F6, 0xFF7986CB, 0xFFBA68C8, 0xFFA1887F, 0xFF90A4AE)
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("cart") }
    var color by remember { mutableStateOf(colors[5]) }
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp)
            .heightIn(max = 640.dp).verticalScroll(rememberScrollState())) {
            Text("New category", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Color(color).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) { androidx.compose.material3.Icon(CatIcons.forKey(icon), null, tint = Color(color), modifier = Modifier.size(24.dp)) }
                Spacer(Modifier.width(12.dp))
                OutlinedTextField(
                    value = name, onValueChange = { name = it }, singleLine = true,
                    placeholder = { Text("Category name") }, modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(12.dp))
            MutedText("Icon", 12)
            Spacer(Modifier.height(6.dp))
            CatIcons.picker.keys.toList().chunked(6).forEach { rowKeys ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    rowKeys.forEach { k ->
                        Box(
                            Modifier.size(42.dp).clip(RoundedCornerShape(12.dp))
                                .background(if (k == icon) Color(color).copy(alpha = 0.2f) else ZColor.Paper)
                                .border(if (k == icon) 2.dp else 0.dp, Color(color), RoundedCornerShape(12.dp))
                                .choice(k == icon) { icon = k }.spoken(k + " icon"),
                            contentAlignment = Alignment.Center,
                        ) { androidx.compose.material3.Icon(CatIcons.forKey(k), null,
                            tint = if (k == icon) Color(color) else ZColor.InkSoft, modifier = Modifier.size(22.dp)) }
                    }
                    repeat(6 - rowKeys.size) { Spacer(Modifier.size(42.dp)) }
                }
            }
            Spacer(Modifier.height(10.dp))
            MutedText("Colour", 12)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                colors.forEach { c ->
                    Box(
                        Modifier.size(30.dp).clip(RoundedCornerShape(15.dp)).background(Color(c))
                            .border(if (c == color) 3.dp else 0.dp, ZColor.Ink, RoundedCornerShape(15.dp))
                            .clickable { color = c },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier.align(Alignment.End).clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                    .clickable { if (name.trim().isNotEmpty()) onCreate(name.trim(), "i:$icon", color) }
                    .padding(horizontal = 22.dp, vertical = 10.dp),
            ) { Text("Create", color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable internal fun LendRow(
    person: String, total: Double, bal: Double, color: Color, verb: String, onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onClick() }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(niceName(person), color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            MutedText(
                if (bal <= 0.0) "Settled · ${money(total)}"
                else if (bal < total) "${money(total - bal)} of ${money(total)} cleared"
                else verb,
                11,
            )
        }
        Text(money(bal), color = if (bal <= 0.0) ZColor.Muted else color, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(6.dp))
        Text("›", color = ZColor.Muted, fontSize = 16.sp)
    }
}

@Composable internal fun LendingDialog(
    person: String, lentRows: List<Row>, repaidSoFar: Double,
    onRowClick: (Row) -> Unit, onSave: (Double) -> Unit, onDismiss: () -> Unit,
    youOwe: Boolean = false,
) {
    val total = lentRows.sumOf { it.amount }
    var text by remember { mutableStateOf(if (repaidSoFar > 0) fmtPlain(repaidSoFar) else "") }
    val entered = text.replace(",", "").toDoubleOrNull() ?: 0.0
    val balance = (total - entered).coerceAtLeast(0.0)
    val headWord = if (youOwe) "Borrowed" else "Lent"
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp)) {
            Text(niceName(person), color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            MutedText("$headWord ${money(total)} in ${lentRows.size} ${if (lentRows.size == 1) "transfer" else "transfers"}", 12)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Fact(headWord, money(total))
                Fact(if (youOwe) "You repaid" else "Repaid", money(entered), ZColor.Income)
                Fact("Balance", money(balance), if (balance <= 0.0) ZColor.Muted else ZColor.Warn)
            }
            Spacer(Modifier.height(14.dp))
            Eyebrow("Repaid so far")
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { v -> text = v.filter { it.isDigit() || it == '.' } },
                singleLine = true,
                placeholder = { Text("0") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Mark fully paid", false) { text = fmtPlain(total) }
                if (entered > 0) Chip("Clear", false) { text = "" }
            }
            if (lentRows.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Eyebrow("Transfers")
                Spacer(Modifier.height(4.dp))
                val sortedLent = remember(lentRows) { lentRows.newestFirst() }
                LazyColumn(Modifier.heightIn(max = 220.dp)) {
                    items(sortedLent) { TxnRow(it, false, onRowClick) }
                }
                MutedText("Not a loan? Tap the transfer to change it.", 11)
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.clip(RoundedCornerShape(20.dp)).clickable { onDismiss() }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) { Text("Cancel", color = ZColor.Muted, fontWeight = FontWeight.SemiBold) }
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                        .clickable { onSave(entered); onDismiss() }.padding(horizontal = 22.dp, vertical = 10.dp),
                ) { Text("Save", color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable internal fun TxnDetailDialog(t: Row, onReclassify: (String, String) -> Unit, onNewCat: (((String) -> Unit)) -> Unit,
    onRetitle: (String) -> Unit = {}, onSplit: (() -> Unit)? = null, onDismiss: () -> Unit) {
    val titleIdeas = remember(t.id) { Parser.suggestTitles(t.raw, t.counterparty) }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MerchantTile(t.counterparty ?: t.category, t.category, kind = t.kind)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(rowTitle(t), color = ZColor.Ink,
                        fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    MutedText(prettyDate(t.date) + (TxnTime.label(t).let { if (it.isNotEmpty()) ", $it" else "" }) + " · " + subtitleFor(t), 12)
                }
                Spacer(Modifier.width(10.dp))
                AmountText(t, false)
            }
            if (t.account.isNotBlank()) { Spacer(Modifier.height(8.dp)); MutedText("Account: ${t.account}", 12) }
            Spacer(Modifier.height(16.dp))
            Eyebrow("From this message")
            Spacer(Modifier.height(6.dp))
            Text(
                t.raw.ifBlank { "(no message text stored)" }, color = ZColor.InkSoft, fontSize = 13.sp,
                modifier = Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState()),
            )
            Spacer(Modifier.height(14.dp))
            Eyebrow("Why this label")
            Spacer(Modifier.height(4.dp))
            MutedText(Classifier.explain(t.counterparty, t.raw, t.kind, t.category), 12)
            var saved by remember { mutableStateOf<String?>(null) }
            saved?.let { Spacer(Modifier.height(8.dp)); Text("✓ $it", color = ZColor.Income, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.height(12.dp))
            var retitling by remember { mutableStateOf(false) }
            var newTitle by remember { mutableStateOf(t.counterparty ?: "") }
            Box(Modifier.clip(RoundedCornerShape(20.dp)).border(1.dp, ZColor.Line, RoundedCornerShape(20.dp))
                .clickable { retitling = !retitling }.padding(horizontal = 16.dp, vertical = 9.dp)) {
                Text(if (retitling) "Cancel" else "Edit title", color = ZColor.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            if (retitling) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = newTitle, onValueChange = { newTitle = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Who was this?") })
                if (titleIdeas.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    MutedText("Names in the message:", 11)
                    Spacer(Modifier.height(4.dp))
                    ChipScroll { titleIdeas.forEach { n -> Chip(n, newTitle.equals(n, ignoreCase = true)) { newTitle = n } } }
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.align(Alignment.End).clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                    .clickable {
                        if (newTitle.isNotBlank()) { onRetitle(newTitle.trim()); retitling = false; saved = "Title saved: ${newTitle.trim()}" }
                    }.padding(horizontal = 20.dp, vertical = 9.dp)) {
                    Text("Save title", color = ZColor.Paper, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(16.dp))
            var editing by remember { mutableStateOf(false) }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.clip(RoundedCornerShape(20.dp)).border(1.dp, ZColor.Line, RoundedCornerShape(20.dp))
                        .clickable { editing = !editing }.padding(horizontal = 16.dp, vertical = 9.dp),
                ) { Text(if (editing) "Cancel" else "Change category", color = ZColor.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                Box(
                    Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                        .clickable { onDismiss() }.padding(horizontal = 22.dp, vertical = 10.dp),
                ) { Text("Close", color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
            }
            if (editing) {
                // Picking a category saves it, closes the picker and says so; the payment above shows the new label.
                fun pick(kind: String, c: String) {
                    onReclassify(kind, c); editing = false
                    saved = "Moved to ${if (c in CustomCats.names) customLabel(c) else c}" +
                        (if (kind != "income" && kind != "ignore" && !t.counterparty.isNullOrBlank()) ", and ${t.counterparty}'s other payments too" else "")
                }
                Spacer(Modifier.height(10.dp))
                MutedText("Remembered for ${t.counterparty?.let { niceName(it) } ?: "this payee"}:", 12)
                Spacer(Modifier.height(6.dp))
                ChipScroll {
                    Classifier.CATEGORIES.keys.forEach { c -> CatTab(c, t.category == c) { pick(if (c == "Investment") "invest" else "expense", c) } }
                    CustomCats.names.forEach { c ->
                        CatTab(c, t.category == c) { pick("expense", c) }
                    }
                    KindTab("new", "New", false) { onNewCat { c -> pick("expense", c) } }
                }
                if (t.direction == "credit") {
                    Spacer(Modifier.height(6.dp))
                    MutedText("Money-in category:", 12)
                    Spacer(Modifier.height(6.dp))
                    ChipScroll { IncomeCats.all.forEach { c -> CatTab(c, t.category == c) { pick("income", c) } } }
                }
                Spacer(Modifier.height(6.dp))
                ChipScroll {
                    KindTab("income", "Money in", t.kind == "income" && t.category == "Income") { pick("income", "Income") }
                    // Money back for something you bought: it nets against that spend once matched.
                    if (t.direction == "credit") KindTab("refund", "Refund", t.kind == "refund") { pick("refund", "Refund") }
                    KindTab("transfer", "Transfer", t.kind == "transfer") { pick("transfer", "Transfer") }
                    KindTab("lent", "Lent", t.kind == "lent") { pick("lent", "Lent") }
                    KindTab("borrowed", "Borrowed", t.kind == "borrowed") { pick("borrowed", "Borrowed") }
                    KindTab("ignore", "Ignore", t.kind == "ignore") { pick("ignore", "Ignore") }
                    if (onSplit != null) KindTab("split", if (t.id.contains('#')) "Remove split" else "Split", false) { onSplit() }
                }
            }
        }
    }
}

// ---------- shared pieces ----------
