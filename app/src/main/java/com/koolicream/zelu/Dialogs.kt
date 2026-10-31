package com.koolicream.zelu

import androidx.compose.ui.res.stringResource

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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
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
            val total = rows.rupees()
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
            ) { Text(stringResource(R.string.close), color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
        }
    }
}

// One category's monthly limit, opened from its row on Spend. Leave it empty or press Remove to have none.
@Composable internal fun CategoryBudgetDialog(category: String, current: Double, onSave: (Double) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(if (current > 0) fmtPlain(current) else "") }
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MerchantTile("", category, 36.dp)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.category_budget_title, category), color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = text, onValueChange = { v -> text = v.filter { it.isDigit() || it == '.' } },
                singleLine = true, placeholder = { Text(stringResource(R.string.per_month)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (current > 0) Text(stringResource(R.string.remove_budget), color = ZColor.Warn, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { onSave(0.0) }.padding(horizontal = 10.dp, vertical = 8.dp))
                Spacer(Modifier.weight(1f))
                Box(Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                    .clickable { onSave(text.toDoubleOrNull() ?: 0.0) }.padding(horizontal = 22.dp, vertical = 10.dp)) {
                    Text(stringResource(R.string.save), color = ZColor.Paper, fontWeight = FontWeight.SemiBold)
                }
            }
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
            Text(stringResource(R.string.budgets), color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            MutedText(stringResource(R.string.a_monthly_limit_per_category), 12)
            Spacer(Modifier.height(12.dp))
            Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
                names.forEach { c ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        MerchantTile("", c, 30.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(c, color = ZColor.Ink, fontSize = 14.sp, modifier = Modifier.weight(1f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Chip(stringResource(R.string.fixed), flags[c] == true) { flags[c] = flags[c] != true }
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
                        placeholder = { Text(stringResource(R.string.add_your_own_e_g)) }, modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Chip(stringResource(R.string.add), false, filled = true) {
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
            ) { Text(stringResource(R.string.done), color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
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
                    Text(stringResource(R.string.savings_plan), color = ZColor.Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f))
                    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink).clickable { save() }
                        .padding(horizontal = 22.dp, vertical = 10.dp)) {
                        Text(stringResource(R.string.done), color = ZColor.Paper, fontWeight = FontWeight.SemiBold)
                    }
                }

                // 1. This month's picture
                Panel {
                    Eyebrow(stringResource(R.string.every_month))
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.you_earn) + inr(avgIn) + " and spend " + inr(avgSpent) + ".", color = ZColor.Ink,
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
                            MutedText(stringResource(R.string.spent_2) + Math.round(sp * 100) + "%", 12)
                            MutedText(stringResource(R.string.left) + Math.round((1f - sp) * 100) + "%", 12)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    MutedText(stringResource(R.string.average_of_your_last_3), 12)
                }

                // 2. SIP
                Panel {
                    Eyebrow(stringResource(R.string.your_sip))
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
                    Text(stringResource(R.string.still_free_after_sip) + (if (emiN > 0) " and EMI" else "") + ": " + inr(free),
                        color = if (free >= 0) ZColor.Income else ZColor.Warn, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    if (free < 0) MutedText(stringResource(R.string.that_is_more_than_you), 12)
                    Spacer(Modifier.height(12.dp))
                    if (!emiOpen) {
                        Text(stringResource(R.string.planning_a_loan_add_an), color = ZColor.Accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { emiOpen = true })
                    } else {
                        field("New EMI to plan for (₹ / month)", emi) { emi = it }
                        Spacer(Modifier.height(4.dp))
                        MutedText(stringResource(R.string.only_for_a_new_loan), 12)
                    }
                }

                // 3. What it becomes
                Panel {
                    Eyebrow(stringResource(R.string.what_it_becomes))
                    Spacer(Modifier.height(6.dp))
                    if (sipN > 0) {
                        Text(inr(fv), color = ZColor.Ink, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        MutedText(stringResource(R.string.in_label) + fmtPlain(yrs) + " years at " + fmtPlain(rate) + "% a year", 13)
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
                                Spacer(Modifier.width(6.dp)); MutedText(stringResource(R.string.you_put_in) + inr(invested), 12)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(ZColor.Accent))
                                Spacer(Modifier.width(6.dp)); MutedText(stringResource(R.string.growth) + inr(fv - invested), 12)
                            }
                        }
                    } else MutedText(stringResource(R.string.move_the_sip_slider_to), 13)
                    Spacer(Modifier.height(14.dp))
                    MutedText(stringResource(R.string.years), 12); Spacer(Modifier.height(6.dp))
                    ChipScroll { listOf(3, 5, 10, 15).forEach { y -> Chip(stringResource(R.string.yr, y), num(years) == y.toDouble(), true) { years = y.toString() } } }
                    Spacer(Modifier.height(10.dp))
                    MutedText(stringResource(R.string.yearly_return), 12); Spacer(Modifier.height(6.dp))
                    ChipScroll { listOf(8, 10, 12, 15).forEach { p -> Chip("$p%", num(ret) == p.toDouble(), true) { ret = p.toString() } } }
                    Spacer(Modifier.height(10.dp))
                    MutedText(stringResource(R.string.returns_are_an_assumption_not), 11)
                }

                // 4. Goal
                Panel {
                    Eyebrow(stringResource(R.string.goal_optional))
                    Spacer(Modifier.height(8.dp))
                    field("Goal amount (₹), e.g. a down payment", goal) { goal = it }
                    if (goalN > 0) {
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.to_reach) + inr(goalN) + " in " + fmtPlain(yrs) + " years you need " + inr(needed) + " a month.",
                            color = ZColor.Ink, fontSize = 15.sp)
                        Spacer(Modifier.height(6.dp))
                        when {
                            sipN <= 0 -> MutedText(stringResource(R.string.set_a_sip_above_to), 13)
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
                            MutedText(stringResource(R.string.more_than_you_have_left), 12)
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
        if (!settled && bal < total) MutedText(stringResource(R.string.of, money(total)), 11)
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
            Text(stringResource(R.string.new_category), color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Color(color).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) { androidx.compose.material3.Icon(CatIcons.forKey(icon), null, tint = Color(color), modifier = Modifier.size(24.dp)) }
                Spacer(Modifier.width(12.dp))
                OutlinedTextField(
                    value = name, onValueChange = { name = it }, singleLine = true,
                    placeholder = { Text(stringResource(R.string.category_name)) }, modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(12.dp))
            MutedText(stringResource(R.string.icon), 12)
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
            MutedText(stringResource(R.string.colour), 12)
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
            ) { Text(stringResource(R.string.create), color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
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
    val total = lentRows.rupees()
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
                Fact(stringResource(R.string.balance), money(balance), if (balance <= 0.0) ZColor.Muted else ZColor.Warn)
            }
            Spacer(Modifier.height(14.dp))
            Eyebrow(stringResource(R.string.repaid_so_far))
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
                Chip(stringResource(R.string.mark_fully_paid), false) { text = fmtPlain(total) }
                if (entered > 0) Chip(stringResource(R.string.clear), false) { text = "" }
            }
            if (lentRows.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Eyebrow(stringResource(R.string.transfers))
                Spacer(Modifier.height(4.dp))
                val sortedLent = remember(lentRows) { lentRows.newestFirst() }
                LazyColumn(Modifier.heightIn(max = 220.dp)) {
                    items(sortedLent) { TxnRow(it, false, onRowClick) }
                }
                MutedText(stringResource(R.string.not_a_loan_tap_the), 11)
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.clip(RoundedCornerShape(20.dp)).clickable { onDismiss() }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) { Text(stringResource(R.string.cancel), color = ZColor.Muted, fontWeight = FontWeight.SemiBold) }
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                        .clickable { onSave(entered); onDismiss() }.padding(horizontal = 22.dp, vertical = 10.dp),
                ) { Text(stringResource(R.string.save), color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable internal fun TxnDetailDialog(t: Row, onReclassify: (Kind, String) -> Unit, onNewCat: (((String) -> Unit)) -> Unit,
    onRetitle: (String) -> Unit = {}, onSplit: (() -> Unit)? = null,
    refundOf: Row? = null, onRefundAnswer: (Boolean) -> Unit = {},
    copyOf: Row? = null, onSameCopy: () -> Unit = {}, onTwoPayments: () -> Unit = {},
    onDismiss: () -> Unit) {
    val titleIdeas = remember(t.id) { Parser.suggestTitles(t.raw, t.counterparty) }
    // With the category picker open this is taller than a phone screen: it scrolls instead of squashing its last row.
    val maxHeight = (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp * 0.9f).dp
    val moneyIn = t.kind == Kind.INCOME || t.kind == Kind.REFUND || t.kind == Kind.BORROWED
    val unsorted = t.kind == Kind.EXPENSE && t.category == "Unknown"
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card)
                .heightIn(max = maxHeight).verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Who, how much and when: the page says nothing else until you ask.
            MerchantTile(t.counterparty ?: t.category, if (unsorted) "" else t.category, 52.dp, kind = t.kind)
            Spacer(Modifier.height(10.dp))
            var retitling by remember { mutableStateOf(false) }
            var newTitle by remember { mutableStateOf(t.counterparty ?: "") }
            Text(rowTitle(t), color = ZColor.Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { retitling = !retitling })
            MutedText(prettyDate(t.date) + (TxnTime.label(t).let { if (it.isNotEmpty()) ", $it" else "" }) +
                (if (t.account.isNotBlank()) " · " + t.account else ""), 13)
            Spacer(Modifier.height(8.dp))
            Text((if (moneyIn) "+" else "") + money(t.amount), color = if (moneyIn) ZColor.Income else ZColor.Ink, fontSize = 36.sp, fontWeight = FontWeight.Bold)
            var saved by remember { mutableStateOf<String?>(null) }
            saved?.let { Spacer(Modifier.height(6.dp)); Text("✓ $it", color = ZColor.Income, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center) }
            if (retitling) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = newTitle, onValueChange = { newTitle = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.who_was_this)) })
                if (titleIdeas.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    MutedText(stringResource(R.string.names_in_the_message), 11)
                    Spacer(Modifier.height(4.dp))
                    ChipScroll { titleIdeas.forEach { n -> Chip(n, newTitle.equals(n, ignoreCase = true)) { newTitle = n } } }
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.align(Alignment.End).clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                    .clickable {
                        if (newTitle.isNotBlank()) { onRetitle(newTitle.trim()); retitling = false; saved = "Title saved: ${newTitle.trim()}" }
                    }.padding(horizontal = 20.dp, vertical = 9.dp)) {
                    Text(stringResource(R.string.save_title), color = ZColor.Paper, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(14.dp))
            // The category is the one big button. Unsorted, it wears a red mark; tapping opens the choices.
            var editing by remember { mutableStateOf(false) }
            val catLabel = if (t.kind == Kind.EXPENSE && t.category in CustomCats.names) customLabel(t.category) else subtitleFor(t)
            Row(Modifier.clip(RoundedCornerShape(26.dp)).background(ZColor.Tile).clickable { editing = !editing }
                .padding(start = if (unsorted) 6.dp else 18.dp, end = 16.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (unsorted) {
                    Box(Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFEF4444)), contentAlignment = Alignment.Center) {
                        Text("!", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, modifier = Unspoken)
                    }
                    Spacer(Modifier.width(10.dp))
                }
                Text(if (unsorted) stringResource(R.string.unknown_label) else catLabel, color = ZColor.Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 6.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (editing) "▴" else "▾", color = ZColor.Muted, fontSize = 12.sp, modifier = Unspoken)
            }
            if (editing) {
                // Picking a category saves it, closes the picker and says so; the payment above shows the new label.
                fun pick(kind: Kind, c: String) {
                    onReclassify(kind, c); editing = false
                    saved = "Moved to ${if (c in CustomCats.names) customLabel(c) else c}" +
                        (if (kind != Kind.INCOME && kind != Kind.IGNORE && !t.counterparty.isNullOrBlank()) ", and ${t.counterparty}'s other payments too" else "")
                }
                Column(Modifier.fillMaxWidth()) {
                    Spacer(Modifier.height(10.dp))
                    MutedText("Remembered for ${t.counterparty?.let { niceName(it) } ?: "this payee"}:", 12)
                    Spacer(Modifier.height(6.dp))
                    ChipScroll {
                        Classifier.CATEGORIES.keys.forEach { c -> CatTab(c, t.category == c) { pick(if (c == "Investment") Kind.INVEST else Kind.EXPENSE, c) } }
                        CustomCats.names.forEach { c ->
                            CatTab(c, t.category == c) { pick(Kind.EXPENSE, c) }
                        }
                        KindTab("new", stringResource(R.string.new_label), false) { onNewCat { c -> pick(Kind.EXPENSE, c) } }
                    }
                    if (t.direction == Dir.CREDIT) {
                        Spacer(Modifier.height(6.dp))
                        MutedText(stringResource(R.string.money_in_category), 12)
                        Spacer(Modifier.height(6.dp))
                        ChipScroll { IncomeCats.all.forEach { c -> CatTab(c, t.category == c) { pick(Kind.INCOME, c) } } }
                    }
                    Spacer(Modifier.height(6.dp))
                    ChipScroll {
                        KindTab("income", stringResource(R.string.money_in), t.kind == Kind.INCOME && t.category == "Income") { pick(Kind.INCOME, "Income") }
                        // Money back for something you bought: it nets against that spend once matched.
                        if (t.direction == Dir.CREDIT) KindTab("refund", stringResource(R.string.refund), t.kind == Kind.REFUND) { pick(Kind.REFUND, "Refund") }
                        KindTab("transfer", stringResource(R.string.transfer), t.kind == Kind.TRANSFER) { pick(Kind.TRANSFER, "Transfer") }
                        KindTab("lent", stringResource(R.string.lent), t.kind == Kind.LENT) { pick(Kind.LENT, "Lent") }
                        KindTab("borrowed", stringResource(R.string.borrowed), t.kind == Kind.BORROWED) { pick(Kind.BORROWED, "Borrowed") }
                        KindTab("ignore", stringResource(R.string.ignore), t.kind == Kind.IGNORE) { pick(Kind.IGNORE, "Ignore") }
                        if (onSplit != null) KindTab("split", if (t.id.contains('#')) "Remove split" else "Split", false) { onSplit() }
                    }
                }
            }
            // A refund that may belong to a purchase: the purchase, and a yes or a no.
            if (refundOf != null) Column(Modifier.fillMaxWidth().padding(top = 14.dp).clip(RoundedCornerShape(16.dp)).background(ZColor.Paper).padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MerchantTile(refundOf.counterparty ?: refundOf.category, refundOf.category, 34.dp, kind = refundOf.kind)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(rowTitle(refundOf), color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        MutedText(prettyDate(refundOf.date), 12)
                    }
                    Text(money(refundOf.amount), color = ZColor.Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettleButton(stringResource(R.string.same_payment), true, Modifier.weight(1f)) { onRefundAnswer(true) }
                    SettleButton(stringResource(R.string.not_related), false, Modifier.weight(1f)) { onRefundAnswer(false) }
                }
            }
            // The same message twice: the other one, and which it is.
            if (copyOf != null) Column(Modifier.fillMaxWidth().padding(top = 14.dp).clip(RoundedCornerShape(16.dp)).background(ZColor.Paper).padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MerchantTile(copyOf.counterparty ?: copyOf.category, copyOf.category, 34.dp, kind = copyOf.kind)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(rowTitle(copyOf), color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        MutedText(prettyDate(copyOf.date) + (TxnTime.label(copyOf).let { if (it.isNotEmpty()) ", $it" else "" }), 12)
                    }
                    Text(money(copyOf.amount), color = ZColor.Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettleButton(stringResource(R.string.dup_same), true, Modifier.weight(1f)) { onSameCopy() }
                    SettleButton(stringResource(R.string.dup_two), false, Modifier.weight(1f)) { onTwoPayments() }
                }
            }
            // The message and why it got this label are one tap away, not on the page.
            var showMsg by remember { mutableStateOf(false) }
            Row(Modifier.fillMaxWidth().padding(top = 14.dp).clip(RoundedCornerShape(14.dp)).background(ZColor.Paper)
                .clickable { showMsg = !showMsg }.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.message_label), color = ZColor.InkSoft, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(if (showMsg) "▴" else "›", color = ZColor.Muted, fontSize = 16.sp, modifier = Unspoken)
            }
            if (showMsg) Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(
                    t.raw.ifBlank { "(no message text stored)" }, color = ZColor.InkSoft, fontSize = 13.sp,
                    modifier = Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState()),
                )
                Spacer(Modifier.height(10.dp))
                Eyebrow(stringResource(R.string.why_this_label))
                Spacer(Modifier.height(4.dp))
                MutedText(Classifier.explain(t.counterparty, t.raw, t.kind, t.category), 12)
            }
            Spacer(Modifier.height(16.dp))
            Box(Modifier.align(Alignment.End).clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                .clickable { onDismiss() }.padding(horizontal = 22.dp, vertical = 10.dp)) {
                Text(stringResource(R.string.close), color = ZColor.Paper, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// A yes or a no under a question on the payment page: the yes is filled, the no is outlined.
@Composable private fun SettleButton(label: String, primary: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(22.dp)).then(if (primary) Modifier.background(ZColor.Accent) else Modifier.border(1.dp, ZColor.Line, RoundedCornerShape(22.dp)))
        .clickable { onClick() }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
        Text(label, color = if (primary) ZColor.AccentInk else ZColor.InkSoft, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

// ---------- shared pieces ----------
