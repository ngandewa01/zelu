package com.koolicream.zelu

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.res.stringResource

// ---- the pages that open from a payee or a category: a month-by-month chart, and that month's payments ----

private const val CHART_MONTHS = 12

/** "Oct 2026" for "2026-10". */
internal fun monthName(m: String): String = MONTH_ABBR[m.substring(5, 7).toInt() - 1] + " " + m.take(4)

/**
 * One bar per month, the newest at the right, scrolling sideways when there are more than fit. Only the chosen month is
 * bright and carries its amount, so the chart is quiet; a month with nothing is a small dot, not a gap. A dashed line
 * marks a budget when there is one. Tapping a bar chooses that month.
 */
@Composable internal fun MonthChart(
    months: List<String>, values: List<Double>, color: Color, selected: Int, onSelect: (Int) -> Unit, budget: Double = 0.0,
    fit: Boolean = false,
) {
    val max = (values.maxOrNull() ?: 0.0).coerceAtLeast(budget).coerceAtLeast(1.0)
    val scroll = rememberScrollState()
    LaunchedEffect(months.size) { scroll.scrollTo(scroll.maxValue) }
    val plot = 112.dp
    val labelH = 24.dp
    // In the strip that fits the width there is no room for the amount over a bar; the card shows it big above.
    val tagH = if (fit) 6.dp else 24.dp
    val lineColor = ZColor.Muted
    // A usual month: the average of the months that have payments, drawn as a thin line through every bar.
    val paid = values.filter { it > 0 }
    val avg = if (paid.size > 1) PatternMath.median(paid) else 0.0
    // fit: every month shares the width (Spend's twelve months), so nothing scrolls sideways.
    Row(if (fit) Modifier.fillMaxWidth() else Modifier.fillMaxWidth().horizontalScroll(scroll), horizontalArrangement = Arrangement.spacedBy(if (fit) 4.dp else 6.dp)) {
        months.forEachIndexed { i, m ->
            val v = values.getOrElse(i) { 0.0 }
            val on = i == selected
            Column(
                (if (fit) Modifier.weight(1f) else Modifier.width(38.dp)).height(tagH + plot + 4.dp + labelH)
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onSelect(i) }
                    .drawBehind {
                        if (avg > 0) {
                            val y = size.height - labelH.toPx() - 4.dp.toPx() - (avg / max).toFloat() * plot.toPx()
                            drawLine(color.copy(alpha = 0.9f), Offset(-3.dp.toPx(), y), Offset(size.width + 3.dp.toPx(), y), strokeWidth = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 5f)))
                        }
                        if (budget > 0) {
                            val y = size.height - labelH.toPx() - 4.dp.toPx() - (budget / max).toFloat() * plot.toPx()
                            drawLine(lineColor, Offset(-3.dp.toPx(), y), Offset(size.width + 3.dp.toPx(), y), strokeWidth = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
                        }
                    },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom,
            ) {
                Box(Modifier.height(tagH), contentAlignment = Alignment.BottomCenter) {
                    if (on && v > 0 && !fit) Box(Modifier.clip(RoundedCornerShape(8.dp)).background(ZColor.Ink).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text(shortAmt(v), color = ZColor.Paper, fontSize = if (fit) 9.sp else 11.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, softWrap = false)
                    }
                }
                val target: Dp = if (v > 0) (plot * (v / max).toFloat()).coerceAtLeast(6.dp) else 5.dp
                val h by animateDpAsState(target, tween(520, easing = FastOutSlowInEasing), label = "bar")
                Box((if (fit) Modifier.fillMaxWidth(0.8f) else Modifier.width(30.dp)).height(h)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                    .background(if (v > 0) color.copy(alpha = if (on) 1f else 0.38f) else ZColor.Line))
                Spacer(Modifier.height(4.dp))
                Box(Modifier.height(labelH), contentAlignment = Alignment.Center) {
                    Text(MONTH_ABBR[m.substring(5, 7).toInt() - 1].let { if (fit && !on) it.take(1) else it }, color = if (on) ZColor.Ink else ZColor.Muted, fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
        }
    }
}

// The big number for the chosen month, and how it sits against a usual month (the average of the months with payments).
@Composable private fun MonthHeadline(values: List<Double>, selected: Int) {
    val v = values.getOrElse(selected) { 0.0 }
    val paid = values.filter { it > 0 }
    val usual = PatternMath.median(paid)
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Text(money(v), color = ZColor.Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        if (usual > 0 && v > 0 && paid.size > 1) {
            val d = Math.round((v - usual) / usual * 100).toInt()
            if (d != 0) Text("  " + (if (d > 0) "▲ " else "▼ ") + kotlin.math.abs(d) + "% " + stringResource(R.string.vs_usual),
                color = if (d > 0) ZColor.Warn else ZColor.Income, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 5.dp))
        }
    }
    if (paid.size > 1) MutedText(stringResource(R.string.average_a_month, money(usual)), 12)
    }
}

@Composable private fun PageBar(title: String, subtitle: String, onBack: () -> Unit, trailing: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TopIcon(stringResource(R.string.back), onClick = onBack) { Text("‹", color = ZColor.Ink, fontSize = 22.sp, modifier = Unspoken) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = ZColor.Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            MutedText(subtitle, 12)
        }
        trailing()
    }
}

// The month a page opens on: the newest month that has a payment, else the newest month shown.
private fun openMonth(values: List<Double>): Int = values.indexOfLast { it > 0 }.takeIf { it >= 0 } ?: (values.size - 1)

private fun lastMonthOf(rows: List<Row>): String = maxOf(rows.maxOfOrNull { it.date.take(7) } ?: "", Dates.todayMonth())

/** Everything about one payee: how often, how much, and the chart of every month, with the chosen month's payments below. */
@Composable internal fun PayeePage(
    name: String, history: List<Row>, others: List<String>,
    onBack: () -> Unit, onRowClick: (Row) -> Unit,
    onRecategorize: (String?, String, Kind, String) -> Unit, onNewCat: (((String) -> Unit)) -> Unit,
    onRename: (String) -> Unit,
) {
    val months = remember(history) { Analytics.monthsUpTo(lastMonthOf(history), CHART_MONTHS) }
    // The chart counts what counts: a payment switched off is in the list below but not in the bars.
    val values = remember(history) { Analytics.monthlyTotals(history.filter { it.kind != Kind.IGNORE }, months) }
    var sel by remember(name) { mutableStateOf(openMonth(values)) }
    val primary = remember(history) { history.groupBy { it.category }.maxByOrNull { it.value.size }?.key ?: "Unknown" }
    val inMonth = remember(history, sel) { SplitMarks.forDisplay(history.filter { it.date.startsWith(months[sel]) }).newestFirst() }
    val rest = remember(history, sel) { SplitMarks.forDisplay(history.filter { !it.date.startsWith(months[sel]) }).newestFirst() }
    var showRest by remember(name) { mutableStateOf(false) }
    val withPayee = history.firstOrNull { !it.counterparty.isNullOrBlank() }
    var menu by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var newName by remember(name) { mutableStateOf(name) }
    val total = history.sumOf { it.amount }
    Box(Modifier.fillMaxSize().background(ZColor.Paper)) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 46.dp, bottom = 40.dp)) {
            item {
                PageBar(niceName(name), primary + " · " + history.size + " " + stringResource(if (history.size == 1) R.string.visit else R.string.visits), onBack) {
                    Box {
                        TopIcon(stringResource(R.string.more), onClick = { menu = true }) { Text("⋯", color = ZColor.Ink, fontSize = 20.sp, modifier = Unspoken) }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            if (withPayee != null) DropdownMenuItem(text = { Text(stringResource(R.string.change_category)) },
                                onClick = { menu = false; editing = true; renaming = false })
                            DropdownMenuItem(text = { Text(stringResource(R.string.rename_or_merge)) }, onClick = { menu = false; renaming = true; editing = false })
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FactCard(stringResource(R.string.visits_label), history.size.toString(), Modifier.weight(1f))
                    FactCard(stringResource(R.string.average), money(total / history.size.coerceAtLeast(1)), Modifier.weight(1f))
                    FactCard(stringResource(R.string.last_paid), prettyDate(history.maxOf { it.date }), Modifier.weight(1f))
                }
                if (renaming) {
                    Spacer(Modifier.height(12.dp))
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ZColor.Card).padding(14.dp)) {
                        MutedText(stringResource(R.string.rename_it_or_type_another), 12)
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(value = newName, onValueChange = { newName = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        if (others.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            MutedText(stringResource(R.string.merge_into), 11)
                            Spacer(Modifier.height(4.dp))
                            ChipScroll { others.forEach { o -> Chip(o, false) { newName = o } } }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Text(stringResource(R.string.cancel), color = ZColor.Muted, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable { renaming = false }.padding(10.dp))
                            Box(Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                                .clickable { if (newName.isNotBlank()) { renaming = false; onRename(newName) } }.padding(horizontal = 20.dp, vertical = 9.dp)) {
                                Text(stringResource(R.string.save_name), color = ZColor.Paper, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                if (editing && withPayee != null) {
                    fun pick(kind: Kind, cat: String) { editing = false; onRecategorize(withPayee.counterparty, withPayee.id, kind, cat) }
                    Spacer(Modifier.height(12.dp))
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ZColor.Card).padding(14.dp)) {
                        MutedText(stringResource(R.string.applies_to_every_payment_to, name), 12)
                        Spacer(Modifier.height(6.dp))
                        ChipScroll {
                            Classifier.CATEGORIES.keys.forEach { c -> CatTab(c, false) { pick(if (c == "Investment") Kind.INVEST else Kind.EXPENSE, c) } }
                            CustomCats.names.forEach { c -> CatTab(c, false) { pick(Kind.EXPENSE, c) } }
                            KindTab("new", stringResource(R.string.new_label), false) { onNewCat { c -> pick(Kind.EXPENSE, c) } }
                        }
                        Spacer(Modifier.height(6.dp))
                        ChipScroll {
                            KindTab("income", stringResource(R.string.money_in), false) { pick(Kind.INCOME, "Income") }
                            KindTab("lent", stringResource(R.string.lent), false) { pick(Kind.LENT, "Lent") }
                            KindTab("borrowed", stringResource(R.string.borrowed), false) { pick(Kind.BORROWED, "Borrowed") }
                            KindTab("ignore", stringResource(R.string.ignore), false) { pick(Kind.IGNORE, "Ignore") }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(ZColor.Card).padding(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 10.dp)) {
                    MonthHeadline(values, sel)
                    MonthChart(months, values, catColor(primary), sel, { sel = it })
                }
                Spacer(Modifier.height(14.dp))
                Eyebrow(monthName(months[sel]))
                Spacer(Modifier.height(4.dp))
                if (inMonth.isEmpty()) MutedText(stringResource(R.string.nothing_in_month, monthName(months[sel])))
            }
            items(inMonth, key = { it.id }) { r -> TxnRow(r, false, onRowClick) }
            // Every visit stays reachable, even ones older than the chart: the rest fold under one heading.
            if (rest.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(10.dp))
                    FoldHeading(stringResource(R.string.other_months), rest.size, showRest) { showRest = !showRest }
                }
                if (showRest) items(rest, key = { "o" + it.id }) { r -> TxnRow(r, false, onRowClick) }
            }
        }
    }
}

/** One spending category: the chart of every month with its budget, and the chosen month's payments below. */
@Composable internal fun CategoryPage(
    category: String, history: List<Row>, budget: Double, onSetBudget: () -> Unit,
    onBack: () -> Unit, onRowClick: (Row) -> Unit,
) {
    val months = remember(history) { Analytics.monthsUpTo(lastMonthOf(history), CHART_MONTHS) }
    // The chart counts what counts: a payment switched off is in the list below but not in the bars.
    val values = remember(history) { Analytics.monthlyTotals(history.filter { it.kind != Kind.IGNORE }, months) }
    var sel by remember(category) { mutableStateOf(openMonth(values)) }
    val inMonth = remember(history, sel) { SplitMarks.forDisplay(history.filter { it.date.startsWith(months[sel]) }).newestFirst() }
    Box(Modifier.fillMaxSize().background(ZColor.Paper)) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 46.dp, bottom = 40.dp)) {
            item {
                PageBar(category, monthName(months[sel]), onBack) {
                    Text(if (budget > 0) stringResource(R.string.budget_amount, money(budget)) else stringResource(R.string.set_budget_action),
                        color = ZColor.Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                        modifier = Modifier.clip(RoundedCornerShape(20.dp)).border(1.5.dp, ZColor.Accent, RoundedCornerShape(20.dp))
                            .clickable { onSetBudget() }.padding(horizontal = 13.dp, vertical = 7.dp))
                }
                Spacer(Modifier.height(14.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(ZColor.Card).padding(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 10.dp)) {
                    MonthHeadline(values, sel)
                    MonthChart(months, values, catColor(category), sel, { sel = it }, budget)
                }
                Spacer(Modifier.height(14.dp))
                Eyebrow(if (inMonth.isEmpty()) monthName(months[sel]) else stringResource(R.string.n_payments, inMonth.size))
                Spacer(Modifier.height(4.dp))
                if (inMonth.isEmpty()) MutedText(stringResource(R.string.nothing_in_month, monthName(months[sel])))
            }
            items(inMonth, key = { it.id }) { r -> TxnRow(r, false, onRowClick) }
        }
    }
}

@Composable private fun FactCard(label: String, value: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(ZColor.Card).padding(horizontal = 11.dp, vertical = 9.dp)) {
        Text(label, color = ZColor.Muted, fontSize = 11.sp, maxLines = 1)
        Text(value, color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
