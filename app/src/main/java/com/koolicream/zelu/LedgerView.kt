@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.koolicream.zelu

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag

import androidx.compose.material.icons.rounded.MoveToInbox
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.RealEstateAgent
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.SouthWest
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

// A payment's name: the payee in readable case, or, when the message names none, what kind of payment it was
// (so a row never says its category twice).
internal fun rowTitle(t: Row): String = t.counterparty?.takeIf { it.isNotBlank() }?.let { niceName(it) } ?: when {
    t.kind == Kind.EXPENSE && (t.account.contains(" CC ") || t.account.endsWith(" CC")) -> "Card payment"
    t.kind == Kind.EXPENSE -> "Payment"
    t.kind == Kind.INCOME -> "Money in"
    else -> t.category
}

@Composable internal fun TxnRow(t: Row, highlight: Boolean, onClick: (Row) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick(t) }.padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically) {
        MerchantTile(t.counterparty ?: t.category, t.category, kind = t.kind)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(rowTitle(t), color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            val at = TxnTime.label(t)
            // A foreign-currency card spend shows what was actually charged ("USD 25.00").
            val fx = remember(t.raw) { Parser.foreign(t.raw) }
            MutedText(prettyDate(t.date) + (if (at.isNotEmpty()) ", $at" else "") + " · " + subtitleFor(t) +
                (if (fx != null) " · " + Fx.label(fx) else ""), 12)
        }
        Spacer(Modifier.width(10.dp))
        AmountText(t, highlight)
    }
}

@Composable internal fun AmountText(t: Row, highlight: Boolean) {
    when (t.kind) {
        Kind.TRANSFER -> Text(stringResource(R.string.transfer), color = ZColor.Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Kind.INVEST -> Text(money(t.amount), color = ZColor.Muted, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Kind.IGNORE -> Text("—", color = ZColor.Muted, fontSize = 14.sp)
        Kind.INCOME, Kind.REFUND, Kind.BORROWED ->
            Text("+" + money(t.amount), color = ZColor.Income, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        else -> Text("−" + money(t.amount),
            color = if (highlight && t.kind == Kind.EXPENSE) ZColor.Accent else ZColor.Ink,   // spending is normal: not red
            fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ---------- Ledger ----------

// A credit-card bill settles spends already counted, so it is not new spending.
private fun isCardBill(r: Reminder) = r.minAmount != null || r.account.contains(" CC ") || r.account.startsWith("CC ") || r.account.endsWith(" CC")

@Composable internal fun LedgerView(
    allRows: List<Row>, list: List<Row>, f: Analytics.Filter, accounts: List<String>,
    month: String, account: String, daySel: Int, onDay: (Int) -> Unit,
    onRowClick: (Row) -> Unit,
    onDrill: (String, List<Row>) -> Unit,
    excludeEmi: Boolean = false,
    repaid: Map<String, Double> = emptyMap(),
    onOpenLend: (String) -> Unit = {},
    onOpenBorrow: (String) -> Unit = {},
    fixed: Map<String, Double> = emptyMap(),
    onOpenFixed: () -> Unit = {},
    balances: Map<String, Pair<Double, Long>> = emptyMap(),
    limits: Map<String, Pair<Double, Long>> = emptyMap(),
    reminders: List<Reminder> = emptyList(),
    onOpenCard: (String) -> Unit = {},
    onToggleEmi: () -> Unit = {},
    onEditBudget: () -> Unit = {},
    onOpenDues: () -> Unit = {},
    reminderState: Map<String, Boolean> = emptyMap(),
    attention: Map<String, Attention> = emptyMap(),
    budgets: Map<String, Double> = emptyMap(),
    onOpenSpend: () -> Unit = {},
    onAdd: () -> Unit = {},
    onSetBudget: (Double, Boolean) -> Unit = { _, _ -> },
    monthlyBudget: Double = 0.0,
) {
    val s = Analytics.summarize(list)

    // Heads up — budgets nearly used or exceeded, and cards running low (current month only).
    // They show as chips on the Home card.
    val glanceAlerts = ArrayList<Triple<String, Boolean, () -> Unit>>()   // (what, over?, open)
    val glanceAlertPct = ArrayList<Int>()
    run {
        if (f.period == "month" && f.month == Dates.todayMonth()) {
            // (over?, ratio, icon, title, detail, onClick)
            data class Alert(val over: Boolean, val ratio: Double, val icon: androidx.compose.ui.graphics.vector.ImageVector,
                             val title: String, val detail: String, val go: () -> Unit)
            val alerts = ArrayList<Alert>()
            val totals = Analytics.categoryTotals(allRows, f.month, "All")
            val dayNow = Dates.dayOfMonth(); val dimNow = Dates.daysInMonth(f.month)
            budgets.forEach { (c, b) ->
                val v = totals[c] ?: 0.0
                if (b > 0 && v < b * 0.8 && dayNow >= 5 && v / b > dayNow.toDouble() / dimNow + 0.2) {
                    alerts.add(Alert(false, v / b, CatIcons.forCategory(c) ?: Icons.Rounded.Warning, c,
                        "Running ahead: " + (v / b * 100).toInt() + "% of the budget gone, " + (dayNow * 100 / dimNow) + "% of the month passed",
                    ) { onDrill(c, allRows.filter { it.kind == Kind.EXPENSE && it.category == c && it.date.startsWith(f.month) }) })
                }
                if (b > 0 && v >= b * 0.8) {
                    val over = v > b
                    alerts.add(Alert(over, v / b, CatIcons.forCategory(c) ?: Icons.Rounded.Warning,
                        c,
                        if (over) money(v - b) + " over your " + money(b) + " budget"
                        else money(b - v) + " left of your " + money(b) + " budget",
                    ) { onDrill(c, allRows.filter { it.kind == Kind.EXPENSE && it.category == c && it.date.startsWith(f.month) }) })
                }
            }
            accounts.filter { it.contains(" CC ") }.forEach { a ->
                val avail = limits[a]?.let { it.first - Analytics.cardUsedSince(allRows, a, it) }
                val total = limits["$a#total"]?.first
                if (avail != null && total != null && total > 0 && avail < total * 0.2) {
                    val used = 1.0 - avail / total
                    alerts.add(Alert(avail < total * 0.1, used, Icons.Rounded.CreditCard,
                        a + " is " + (used * 100).toInt() + "% used", money(avail) + " of " + money(total) + " left") { onOpenCard(a) })
                }
            }
            val shownAlerts = alerts.sortedByDescending { it.ratio }.take(3)
            shownAlerts.forEach { a -> glanceAlerts.add(Triple(a.title, a.over, a.go)); glanceAlertPct.add((a.ratio * 100).toInt()) }
        }
    }

    // Home looks ahead: what you can still spend, what's due and where the month will end.
    // Where the money went is on the Spend tab, so it isn't repeated here.
    Panel {
        val isMonth = f.period == "month"
        val liveMonth = isMonth && f.month == Dates.todayMonth()
        val budget = monthlyBudget
        val emiAmt = list.filter { it.kind == Kind.EXPENSE && it.category == "EMI" }.rupees()
        val today = Dates.today()
        // Bills still to pay this month, overdue ones included, settled the same way as on Dues. A card bill pays for
        // spends already counted, so it doesn't use up the budget.
        val autoPaid = remember(reminders, allRows) { autoSettledReminders(reminders, allRows) }
        val billsLeft = if (!liveMonth) 0.0 else reminders.filter { r ->
            !(reminderState[r.id] ?: (r.done || r.id in autoPaid)) && !isCardBill(r) && r.dueDate.take(7) == f.month
        }.sumOf { it.amount }
        val daysLeft = if (liveMonth) Dates.daysInMonth(f.month) - Dates.dayOfMonth() + 1 else 0
        val free = budget - s.spent - billsLeft
        // EMI often dwarfs everyday spending; one small pill leaves it out of every number on this card.
        @Composable fun emiPill() {
            if (emiAmt <= 0 && !excludeEmi) return
            Box(Modifier.clip(RoundedCornerShape(10.dp)).background(if (excludeEmi) ZColor.OrangeSoft else ZColor.Paper)
                .clickable { onToggleEmi() }.padding(horizontal = 9.dp, vertical = 4.dp)) {
                Text(if (excludeEmi) "EMI off ✓" else "Exclude EMI", color = if (excludeEmi) ZColor.Accent else ZColor.Muted,
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
        @Composable fun emiSwitch() {
            if (emiAmt <= 0 && !excludeEmi) return
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { emiPill() }
        }
        // One shape with or without a budget: what you spent, a thin bar and one pill under it. Setting a budget fills the
        // pill and the bar; nothing else on the card moves. The bolder themes draw it on a two-colour gradient.
        val hero = ZColor.Hero
        val onHero = hero != null
        val heroInk = hero?.let { Color(it.ink) } ?: ZColor.Ink
        val soft = if (onHero) heroInk.copy(alpha = 0.82f) else ZColor.Muted
        val hasBudget = isMonth && budget > 0
        val heat = if (liveMonth && hasBudget && free > 0) heatColor(free / daysLeft / (budget / Dates.daysInMonth(f.month))) else heatColor(0.0)
        val prevMonth = if (isMonth) Dates.shiftMonth(f.month, -1) else ""
        val cutDay = if (liveMonth) Dates.dayOfMonth() else 31
        val prevSpent = if (!isMonth) 0.0 else Analytics.filter(allRows, f.copy(month = prevMonth))
            .filter { it.kind == Kind.EXPENSE && it.date.substring(8, 10).toInt() <= cutDay && !(excludeEmi && it.category == "EMI") }.rupees()
        @Composable fun slotPill(text: String, tone: Color, onClick: () -> Unit) {
            Box(Modifier.clip(RoundedCornerShape(20.dp)).background(tone.copy(alpha = 0.2f)).clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 9.dp)) {
                Text(text, color = if (onHero) heroInk else tone, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
        Column(if (hero == null) Modifier.fillMaxWidth() else Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(hero.from), Color(hero.to)))).padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Eyebrow(stringResource(R.string.spent_in) + Analytics.periodLabel(f) + if (excludeEmi) " · after EMI" else "", soft)
                Spacer(Modifier.weight(1f))
                if (prevSpent > 0 && s.spent > 0) {
                    val diff = Math.round((s.spent - prevSpent) / prevSpent * 100).toInt()
                    val down = diff <= 0
                    Box(Modifier.clip(RoundedCornerShape(10.dp)).background(if (down) ZColor.IncomeSoft else ZColor.WarnSoft)
                        .padding(horizontal = 9.dp, vertical = 4.dp)) {
                        Text((if (down) "↓ " else "↑ ") + Math.abs(diff) + "% vs " + Dates.monthLabel(prevMonth).take(3),
                            color = if (down) ZColor.Income else ZColor.Warn, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Text(money(s.spent), color = if (onHero) heroInk else ZColor.Ink, fontSize = 38.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            // The month in one thin bar: spent, still due, free. With no budget it is an empty track, so the card keeps its shape.
            val spentC = if (onHero) heroInk else heat
            val parts = when {
                liveMonth && hasBudget && free > 0 -> listOf(s.spent to spentC, billsLeft to (if (onHero) heroInk.copy(alpha = 0.55f) else ZColor.AmberInk),
                    free to (if (onHero) Color.Transparent else ZColor.Line))
                liveMonth && hasBudget -> listOf(budget to spentC, -free to (if (onHero) heroInk.copy(alpha = 0.55f) else HeatOver))
                hasBudget && s.spent <= budget -> listOf(s.spent to spentC, (budget - s.spent) to Color.Transparent)
                hasBudget -> listOf(budget to spentC, (s.spent - budget) to (if (onHero) heroInk.copy(alpha = 0.55f) else HeatOver))
                else -> emptyList()
            }
            Row(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(if (onHero) heroInk.copy(alpha = 0.22f) else ZColor.Paper)) {
                parts.filter { it.first > 0.5 }.forEach { (v, c) -> Box(Modifier.weight(v.toFloat()).fillMaxHeight().background(c)) }
            }
            Spacer(Modifier.height(12.dp))
            // The pill: what is left a day, how the month ended against the budget, or a way to set one.
            Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                when {
                    liveMonth && hasBudget -> slotPill(
                        if (free > 0) money(free / daysLeft) + stringResource(R.string.a_day) else money(-free) + stringResource(R.string.over),
                        heat, onEditBudget)
                    hasBudget -> slotPill(
                        if (s.spent <= budget) stringResource(R.string.under_budget) else money(s.spent - budget) + stringResource(R.string.over),
                        if (s.spent <= budget) ZColor.Income else ZColor.Warn, onEditBudget)
                    liveMonth -> {
                        // No budget yet: one tap sets one that fits what a month usually costs.
                        val usual = (1..3).map { i ->
                            Analytics.filter(allRows, f.copy(month = Dates.shiftMonth(f.month, -i)))
                                .filter { it.kind == Kind.EXPENSE && !(excludeEmi && it.category == "EMI") }.rupees()
                        }.filter { it > 0 }.let { if (it.isEmpty()) 0.0 else it.average() }
                        val pick = if (usual > 0) Math.ceil(usual * 1.05 / 5000.0) * 5000.0 else 0.0
                        val tone = if (onHero) heroInk else ZColor.Accent
                        Box(Modifier.clip(RoundedCornerShape(20.dp)).border(1.5.dp, tone, RoundedCornerShape(20.dp))
                            .clickable { if (pick > 0) onSetBudget(pick, false) else onEditBudget() }
                            .padding(horizontal = 16.dp, vertical = 9.dp)) {
                            Text(if (pick > 0) stringResource(R.string.set_budget_to, money(pick)) else stringResource(R.string.set_budget),
                                color = tone, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }
        }
        emiSwitch()

        // A budget far below what you really spend keeps this card red every month. Once, Zelu suggests one that fits
        // your last 3 months, leaving EMI out when EMI is a big part of it. "Keep" hides it for good.
        if (liveMonth && budget > 0) {
            val hintCtx = LocalContext.current
            val hints = remember { hintCtx.getSharedPreferences("zelu_hints", android.content.Context.MODE_PRIVATE) }
            var fitSeen by remember { mutableStateOf(hints.getBoolean("budget_fit", false)) }
            val fit = remember(allRows, budget, excludeEmi) {
                val months = (1..3).map { Dates.shiftMonth(Dates.todayMonth(), -it) }
                val spends = months.map { m -> allRows.filter { it.kind == Kind.EXPENSE && it.date.startsWith(m) } }.filter { it.isNotEmpty() }
                if (spends.isEmpty()) null else {
                    val all = spends.map { rs -> rs.rupees() }.average()
                    val emi = spends.map { rs -> rs.filter { it.category == "EMI" }.rupees() }.average()
                    val everyday = all - emi
                    // EMI is a big part of it: the budget should cover everyday spending with EMI left out.
                    val leaveEmi = emi > all * 0.2
                    val base = if (leaveEmi || excludeEmi) everyday else all
                    val pick = Math.ceil(base * 1.05 / 5000.0) * 5000.0
                    // Too low: well under what a usual month costs, or EMI dwarfs the budget and isn't left out yet.
                    val tooLow = budget < base * 0.75 || (leaveEmi && !excludeEmi && budget < all * 0.75)
                    if (tooLow && pick > 0) Triple(pick, leaveEmi, everyday to emi) else null
                }
            }
            if (!fitSeen && fit != null) {
                val (pick, leaveEmi, parts) = fit
                Spacer(Modifier.height(12.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(ZColor.Tile).border(1.dp, ZColor.Line, RoundedCornerShape(14.dp))
                    .padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Icon(Icons.Rounded.Lightbulb, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.your_budget_looks_too_low), color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(4.dp))
                    MutedText(if (leaveEmi) "You usually spend " + money(parts.first) + " a month on everyday things. EMIs (" +
                        money(parts.second) + ") are fixed, so leave them out." else "You usually spend " + money(parts.first + parts.second) + " a month.", 13)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                            .clickable { onSetBudget(pick, leaveEmi); hints.edit().putBoolean("budget_fit", true).apply(); fitSeen = true }
                            .padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.budget) + money(pick) + if (leaveEmi) ", no EMI" else "", color = ZColor.Paper, fontSize = 13.sp,
                                fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                        Box(Modifier.clip(RoundedCornerShape(20.dp)).border(1.dp, ZColor.Line, RoundedCornerShape(20.dp))
                            .clickable { hints.edit().putBoolean("budget_fit", true).apply(); fitSeen = true }
                            .padding(horizontal = 20.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.keep), color = ZColor.InkSoft, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        // A bill that is overdue or due in the next 3 days shows here as one line; the full list lives on Dues.
        run {
            val soonEnd = Dates.shiftDays(today, 3)
            val soon = reminders.filter { r -> !(reminderState[r.id] ?: (r.done || r.id in autoPaid)) && r.dueDate <= soonEnd &&
                r.dueDate.take(7) >= Dates.shiftMonth(Dates.todayMonth(), -1) }.sortedBy { it.dueDate }
            soon.firstOrNull()?.let { r ->
                val inDays = Dates.daysBetween(today, r.dueDate)
                val name = niceName(r.counterparty?.takeIf { it.isNotBlank() } ?: "Bill")
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(ZColor.Tile).clickable { onOpenDues() }
                    .padding(horizontal = 10.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Icon(Icons.Rounded.Schedule, null, tint = if (inDays < 0) ZColor.Warn else Color(0xFFEAB308),
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(name, color = ZColor.Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false))
                    Text(" " + money(r.amount) + " · " + when { inDays < 0 -> "overdue"; inDays == 0 -> "due today"; inDays == 1 -> "due tomorrow"; else -> "due in $inDays days" } +
                        (if (soon.size > 1) " · +${soon.size - 1}" else ""),
                        color = if (inDays < 0) ZColor.Warn else ZColor.InkSoft, fontSize = 13.sp, maxLines = 1, softWrap = false)
                    Spacer(Modifier.weight(1f))
                    Text("›", color = ZColor.Accent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        // A quiet line: icon, words, and an optional link. No box, so the card stays light.
        @Composable fun noteRow(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: androidx.compose.ui.graphics.Color, text: String,
                                action: String?, onClick: (() -> Unit)?) {
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier).padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(text, color = ZColor.InkSoft, fontSize = 13.sp, maxLines = 1, modifier = Modifier.weight(1f))
                if (action != null) Text(action, color = ZColor.Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
        // Month-end estimate from the same numbers as above: spent, bills still due, and a normal day's spending
        // (last 30 days, big one-offs left out) for the days still to come.
        val forecast = if (!liveMonth || s.spent <= 0) 0.0 else {
            val base = allRows.filter { (account == "All" || it.account == account) && !(excludeEmi && it.category == "EMI") }
            s.spent + billsLeft + Analytics.typicalDaily(base, today) * (daysLeft - 1).coerceAtLeast(0)
        }
        val overPace = liveMonth && budget > 0 && forecast > budget
        val alreadyOver = liveMonth && budget > 0 && free <= 0
        // Once the budget is gone the forecast says nothing new, so it shows only while there is still room.
        if (forecast > 0 && !alreadyOver) {
            val end = Dates.daysInMonth(f.month).toString() + " " + Dates.monthLabel(f.month).take(3)
            val verdict = when {
                budget <= 0 -> ""
                forecast > budget -> " · " + money(forecast - budget) + " over"
                else -> " · on track"
            }
            noteRow(Icons.AutoMirrored.Rounded.TrendingUp, if (overPace) ZColor.AmberInk else ZColor.Income,
                "≈ " + money(forecast) + " by $end" + verdict, null, onOpenDues)
        }
        // Running hot: name the category that grew most against the same days last month; the why is on Spend.
        if (overPace || alreadyOver) {
            val cut = Dates.dayOfMonth()
            val prev = Analytics.filter(allRows, f.copy(month = Dates.shiftMonth(f.month, -1)))
                .filter { it.kind == Kind.EXPENSE && it.date.substring(8, 10).toInt() <= cut }.groupBy { it.category }
                .mapValues { (_, rs) -> rs.rupees() }
            list.filter { it.kind == Kind.EXPENSE }.groupBy { it.category }.mapValues { (_, rs) -> rs.rupees() }
                .map { (c, v) -> c to v - (prev[c] ?: 0.0) }.filter { it.second >= 1000 }.maxByOrNull { it.second }
                ?.let { (c, up) -> noteRow(Icons.Rounded.Lightbulb, ZColor.AmberInk, "$c +" + money(up) + " vs last month",
                    "Why ›", onOpenSpend) }
        }
        if (glanceAlerts.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                glanceAlerts.forEachIndexed { i, (what, over, go) ->
                    val tone = if (over) ZColor.Warn else ZColor.AmberInk
                    val soft = if (over) ZColor.WarnSoft else ZColor.AmberSoft
                    Box(Modifier.clip(RoundedCornerShape(10.dp)).background(soft).clickable { go() }.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text(what + " " + glanceAlertPct[i] + "%", color = tone, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
    }

    // Recent — horizontal strip
    Panel {
        val recent = list.filter { it.kind != Kind.INVEST }.newestFirst()
        // A payment that wants a look wears a small mark on its card (red: nothing sorted it; amber: a refund or a message
        // twice to check). Tapping the card opens the payment, where it is settled. Those come first, so none is lost.
        val strip = Analytics.recentStrip(recent, attention)
        Row(Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Eyebrow(stringResource(R.string.recent))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink).clickable { onAdd() }.testTag("add-transaction").padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Icon(Icons.Rounded.Add, null, tint = ZColor.Paper, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.add), color = ZColor.Paper, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.see_all), color = ZColor.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onDrill("All transactions", recent) })
            }
        }
        Spacer(Modifier.height(8.dp))
        if (recent.isEmpty()) MutedText(stringResource(R.string.no_transactions_here))
        else Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            strip.forEach { t ->
                Column(
                    Modifier.width(150.dp).clip(RoundedCornerShape(14.dp)).background(ZColor.Paper)
                        .border(1.dp, ZColor.Line, RoundedCornerShape(14.dp))
                        .clickable { onRowClick(t) }.padding(12.dp),
                ) {
                    // Like a bank app's card: icon, which way the money went, name, amount and when.
                    val moneyIn = t.kind == Kind.INCOME || t.kind == Kind.REFUND || t.kind == Kind.BORROWED
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        MerchantTile(t.counterparty ?: "", t.category, 36.dp, t.kind)
                        Spacer(Modifier.weight(1f))
                        val mark = attention[t.id]
                        if (mark != null) {
                            Box(Modifier.size(22.dp).clip(CircleShape).background(if (mark == Attention.UNKNOWN) Color(0xFFEF4444) else Color(0xFFD97706))
                                .spoken(stringResource(when (mark) { Attention.UNKNOWN -> R.string.mark_unsorted; Attention.REFUND -> R.string.mark_refund; Attention.DUPLICATE -> R.string.mark_duplicate })),
                                contentAlignment = Alignment.Center) {
                                Text(when (mark) { Attention.UNKNOWN -> "!"; Attention.REFUND -> "?"; Attention.DUPLICATE -> "=" },
                                    color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, modifier = Unspoken)
                            }
                        } else androidx.compose.material3.Icon(if (moneyIn) Icons.Rounded.SouthWest else Icons.Rounded.NorthEast,
                            if (moneyIn) "Money in" else "Spent", tint = if (moneyIn) ZColor.Income else ZColor.Muted,
                            modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(rowTitle(t), color = ZColor.Ink, fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text((if (moneyIn) "+" else "") + money(t.amount), color = if (moneyIn) ZColor.Income else ZColor.Ink,
                        fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    val at = TxnTime.label(t)
                    MutedText(if (t.category == "Unknown" && t.kind == Kind.EXPENSE) stringResource(R.string.unknown_label) else (if (at.isNotEmpty()) "$at · " else "") + prettyDay(t.date), 11)
                }
            }
        }
    }
    
    // Accounts — latest balance + spend this period per account, swipeable
    val spentBy = Analytics.spentByAccount(list, accounts).toMap()
    val acctCtx = LocalContext.current
    val acctScope = rememberCoroutineScope()
    var hiddenAccts by remember { mutableStateOf(Storage.hiddenAccounts(acctCtx)) }
    var sourceFor by remember { mutableStateOf<String?>(null) }
    var sourceRefs by remember { mutableStateOf<List<Ingest.Ref>?>(null) }
    val acctNames = (balances.keys + spentBy.keys).distinct().filter { !(it.contains(" CC ") || it.startsWith("CC ") || it.endsWith(" CC")) }
        // A debit card is not an account of its own: it counts under the savings account you link it to.
        .filter { !it.contains(" DC ") }
        .filter { a -> a.split(' ').none { it == "FD" || it == "RD" } }   // fixed and recurring deposits have their own tab
        .filter { it !in hiddenAccts }
        .sortedByDescending { balances[it]?.first ?: -1.0 }
    var allAccountsOpen by remember { mutableStateOf(false) }
    // Accounts, cards and loans share one card with tabs, so Home stays short.
    val cardAccounts = accounts.filter { it.contains(" CC ") || it.startsWith("CC ") || it.endsWith(" CC") }
    val loanGroups = allRows.filter { it.category == "EMI" && Regex(".*\\bLoan \\d{3,4}$").matches(it.counterparty ?: "") }
        .groupBy { it.counterparty!! }
    var moneyTab by remember { mutableStateOf("accounts") }
    var closedLoans by remember { mutableStateOf(Storage.closedLoans(acctCtx)) }
    // Accounts marked (or recognised) as a fixed or recurring deposit: "HDFC FD 0025", "2653 FD".
    val depositNames = accounts.filter { a -> a != "All" && a !in hiddenAccts && a.split(' ').any { it == "FD" || it == "RD" } }
    val hasAcc = acctNames.isNotEmpty(); val hasCard = cardAccounts.isNotEmpty(); val hasLoan = loanGroups.isNotEmpty(); val hasDep = depositNames.isNotEmpty()
    val moneyTabNow = when {
        moneyTab == "accounts" && hasAcc -> "accounts"
        moneyTab == "cards" && hasCard -> "cards"
        moneyTab == "loans" && hasLoan -> "loans"
        moneyTab == "deposits" && hasDep -> "deposits"
        hasAcc -> "accounts"; hasCard -> "cards"; hasLoan -> "loans"; else -> "deposits"
    }
    @Composable fun MoneyTabs() {
        val shown = listOf(hasAcc, hasCard, hasLoan, hasDep).count { it }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            if (shown < 2) Eyebrow(when (moneyTabNow) { "accounts" -> stringResource(R.string.accounts); "cards" -> "Cards"; "deposits" -> stringResource(R.string.deposits); else -> "Loans" })
            // The tabs share the row equally, so they stay on one line next to the eye button.
            else Box(Modifier.weight(1f)) { TabRowWrap {
                if (hasAcc) IconTab(Icons.Rounded.AccountBalance, stringResource(R.string.accounts), 0xFF3B82F6, 0xFF60A5FA, moneyTabNow == "accounts", compact = true, modifier = Modifier.weight(1f)) { moneyTab = "accounts" }
                if (hasCard) IconTab(Icons.Rounded.CreditCard, stringResource(R.string.cards), 0xFFFF6B1A, 0xFFFF8A3D, moneyTabNow == "cards", compact = true, modifier = Modifier.weight(1f)) { moneyTab = "cards" }
                if (hasLoan) IconTab(Icons.Rounded.RealEstateAgent, stringResource(R.string.loans), 0xFF64748B, 0xFF94A3B8, moneyTabNow == "loans", compact = true, modifier = Modifier.weight(1f)) { moneyTab = "loans" }
                if (hasDep) IconTab(Icons.Rounded.Savings, stringResource(R.string.deposits), 0xFF14B8A6, 0xFF2DD4BF, moneyTabNow == "deposits", compact = true, modifier = Modifier.weight(1f)) { moneyTab = "deposits" }
            } }
            // One switch for every balance and available limit on accounts and cards.
            androidx.compose.material3.Icon(
                if (Privacy.hideBalance) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, null,
                tint = if (Privacy.hideBalance) ZColor.Accent else ZColor.Muted,
                modifier = Modifier.size(30.dp).clip(CircleShape).clickable { Privacy.setBalance(acctCtx, !Privacy.hideBalance) }
                    .buttonLabel(if (Privacy.hideBalance) "Show balances" else "Hide balances").padding(5.dp))
        }
    }
    if (hasAcc && moneyTabNow == "accounts") {
        Panel {
            MoneyTabs()
            Spacer(Modifier.height(10.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                acctNames.sortedBy { it in closedLoans }.forEach { a ->
                    ClosableFrame(a in closedLoans, 16.dp, Modifier.width(280.dp), onToggle = {
                        Storage.setLoanClosed(acctCtx, a, a !in closedLoans); closedLoans = Storage.closedLoans(acctCtx)
                    }) { AccountCard(a, balances[a], spentBy[a] ?: 0.0, Modifier.fillMaxWidth()) }
                }
                // Every account, with where each one came from, sits at the end of the strip.
                Box(Modifier.align(Alignment.CenterVertically).clip(RoundedCornerShape(16.dp))
                    .border(1.dp, ZColor.Line, RoundedCornerShape(16.dp)).clickable { allAccountsOpen = true }
                    .padding(horizontal = 18.dp, vertical = 14.dp)) {
                    Text(stringResource(R.string.see_all_2), color = ZColor.Accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
    if (allAccountsOpen) {
        Dialog(onDismissRequest = { allAccountsOpen = false }) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Paper).padding(16.dp)
                .heightIn(max = 640.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.all_accounts), color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                MutedText(stringResource(R.string.tap_one_to_see_its), 12)
                acctNames.sortedBy { it in closedLoans }.forEach { a ->
                    ClosableFrame(a in closedLoans, 16.dp, Modifier.fillMaxWidth(), onToggle = {
                        Storage.setLoanClosed(acctCtx, a, a !in closedLoans); closedLoans = Storage.closedLoans(acctCtx)
                    }) {
                        AccountCard(a, balances[a], spentBy[a] ?: 0.0, Modifier.fillMaxWidth().clickable {
                            sourceFor = a; sourceRefs = null
                            acctScope.launch { sourceRefs = withContext(Dispatchers.IO) { Ingest.referencesFor(acctCtx, a, allRows) } }
                        })
                    }
                }
                if (hiddenAccts.isNotEmpty()) {
                    Eyebrow(stringResource(R.string.hidden))
                    hiddenAccts.forEach { h ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(h, color = ZColor.Muted, fontSize = 14.sp)
                            Text(stringResource(R.string.show_again), color = ZColor.Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { Storage.setAccountHidden(acctCtx, h, false); hiddenAccts = Storage.hiddenAccounts(acctCtx) }.padding(8.dp))
                        }
                    }
                }
            }
        }
    }
    sourceFor?.let { a ->
        Dialog(onDismissRequest = { sourceFor = null }) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp).heightIn(max = 640.dp)) {
                Text(a, color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                MutedText(stringResource(R.string.made_from_these_messages_not), 12)
                Spacer(Modifier.height(10.dp))
                val refs = sourceRefs
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    when {
                        refs == null -> MutedText(stringResource(R.string.looking_for_the_messages), 13)
                        refs.isEmpty() -> MutedText(stringResource(R.string.no_message_found_for_it), 13)
                        else -> refs.forEach { r ->
                            Column(Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(12.dp)).background(ZColor.Paper).padding(12.dp)) {
                                MutedText(r.why + " · " + r.sender + " · " + (if (r.date.length == 10) prettyDate(r.date) else r.date), 11)
                                Spacer(Modifier.height(4.dp))
                                Text(r.body, color = ZColor.Ink, fontSize = 12.sp, maxLines = 8, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.hide_account), color = ZColor.Warn, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { Storage.setAccountHidden(acctCtx, a, true); hiddenAccts = Storage.hiddenAccounts(acctCtx); sourceFor = null }.padding(12.dp))
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink).clickable { sourceFor = null }
                        .padding(horizontal = 22.dp, vertical = 10.dp)) { Text(stringResource(R.string.keep), color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }

    // Cards
    if (cardAccounts.isNotEmpty() && moneyTabNow == "cards") {
        Panel {
            MoneyTabs()
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                cardAccounts.sortedBy { it in closedLoans }.forEach { a ->
                    // Card totals follow only the date range, not the Show/Category filter.
                    val cardRows = Analytics.filter(allRows, f.copy(account = a, type = "all", category = "All"))
                    val spent = cardRows.filter { it.kind == Kind.EXPENSE }.rupees()
                    val today = Dates.today()
                    val due = reminders.filter { r -> !r.done && r.dueDate >= today && r.account.takeLast(4) == a.takeLast(4) }
                        .minByOrNull { it.dueDate }
                    ClosableFrame(a in closedLoans, 18.dp, onToggle = {
                        Storage.setLoanClosed(acctCtx, a, a !in closedLoans); closedLoans = Storage.closedLoans(acctCtx)
                    }) { CardTile(a, spent, limits[a], limits["$a#total"], due, usedSince = Analytics.cardUsedSince(allRows, a, limits[a])) { onOpenCard(a) } }
                }
            }
        }
    }

    // Fixed and recurring deposits: accounts marked (or recognised) as one. A deposit has no balance of its own, so each card
    // shows its latest payment and how many there are; tap one for its payments.
    if (hasDep && moneyTabNow == "deposits") {
        Panel {
            MoneyTabs()
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                depositNames.forEach { a ->
                    val rs = allRows.filter { it.account == a }.newestFirst()
                    val last = rs.firstOrNull()
                    Column(Modifier.width(260.dp).clip(RoundedCornerShape(16.dp)).background(ZColor.Paper)
                        .border(1.dp, ZColor.Line, RoundedCornerShape(16.dp)).clickable { onDrill(a, rs) }.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AcctLogo(a)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(AccountBook.displayName(a), color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(acctTypeLabel(AccountBook.guessType(a)), color = ZColor.Muted, fontSize = 12.sp)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        if (last != null) {
                            Text(privateMoney(last.amount), color = ZColor.Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            MutedText(plural(rs.size, "payment", "payments") + " · " + stringResource(R.string.last) + prettyDay(last.date), 12)
                        } else MutedText(stringResource(R.string.no_payments_in_this_view), 12)
                    }
                }
            }
        }
    }

    // Loans — found from EMI payments named after the loan ("HDFC Loan 529"), all-time.
    if (loanGroups.isNotEmpty() && moneyTabNow == "loans") {
        Panel {
            MoneyTabs()
            Spacer(Modifier.height(10.dp))
            // Open loans as cards: the EMI is the one big number. Closed ones fold into a line each underneath.
            val byLatest = loanGroups.entries.sortedByDescending { e -> e.value.maxOf { it.date } }
            fun loanTitle(name: String): Pair<String, String> {
                val m = Regex("(.*)\\bLoan (\\d{3,4})$").find(name)
                return if (m == null) niceName(name) to "Loan" else (niceName(m.groupValues[1].trim()) + " loan") to ("•• " + m.groupValues[2])
            }
            val openLoans = byLatest.filter { it.key !in closedLoans }
            if (openLoans.isNotEmpty()) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                openLoans.forEach { (name, rs) ->
                    val last = rs.maxByOrNull { it.date }!!
                    val logo = bankLogoRes(name)
                    val (title, sub) = loanTitle(name)
                    ClosableFrame(false, 16.dp, Modifier.width(260.dp), onToggle = {
                        Storage.setLoanClosed(acctCtx, name, true); closedLoans = Storage.closedLoans(acctCtx)
                    }) {
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ZColor.Paper)
                            .border(1.dp, ZColor.Line, RoundedCornerShape(16.dp)).clickable { onDrill(name, rs.newestFirst()) }.padding(14.dp)) {
                            Row(Modifier.padding(end = 30.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(Color.White).border(1.dp, ZColor.Line, RoundedCornerShape(11.dp)),
                                    contentAlignment = Alignment.Center) {
                                    if (logo != null) androidx.compose.foundation.Image(painter = androidx.compose.ui.res.painterResource(logo),
                                        contentDescription = null, modifier = Modifier.size(26.dp))
                                    else BankMark(name)
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(title, color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(sub, color = ZColor.Muted, fontSize = 12.sp)
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(stringResource(R.string.emi_every_month), color = ZColor.Muted, fontSize = 12.sp)
                            Text(privateMoney(last.amount), color = ZColor.Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.paid, rs.size), color = ZColor.Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(" · " + privateMoney(rs.rupees()), color = ZColor.Muted, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                Text(stringResource(R.string.last) + prettyDay(last.date), color = ZColor.Muted, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            byLatest.filter { it.key in closedLoans }.forEach { (name, rs) ->
                val ice = Color(0xFF6FA8D6)
                val (title, sub) = loanTitle(name)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, ZColor.Line, RoundedCornerShape(12.dp))
                    .clickable { onDrill(name, rs.newestFirst()) }.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("❄", color = ice, fontSize = 14.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("$title $sub · closed · ${plural(rs.size, "EMI", "EMIs")}", color = ZColor.Muted, fontSize = 13.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(stringResource(R.string.reopen), color = ZColor.Accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { Storage.setLoanClosed(acctCtx, name, false); closedLoans = Storage.closedLoans(acctCtx) }
                            .padding(horizontal = 4.dp, vertical = 2.dp))
                }
            }
        }
    }

    // Lending — all-time so repayments and balances stay correct across months.
    val lentAll = allRows.filter { it.kind == Kind.LENT }
    val borrowedAll = allRows.filter { it.kind == Kind.BORROWED }
    if (lentAll.isNotEmpty() || borrowedAll.isNotEmpty()) {
        val byPerson = lentAll.groupBy { it.counterparty?.takeIf { c -> c.isNotBlank() } ?: "Someone" }
            .map { (person, rs) ->
                val total = rs.rupees()
                val paid = repaid[person.lowercase().trim()] ?: 0.0
                Triple(person, total, (total - paid).coerceAtLeast(0.0))
            }.sortedByDescending { it.third }
        val owedToYou = byPerson.sumOf { it.third }
        val borrowByPerson = borrowedAll.groupBy { it.counterparty?.takeIf { c -> c.isNotBlank() } ?: "Someone" }
            .map { (person, rs) ->
                val tot = rs.rupees()
                val paid = repaid["b:" + person.lowercase().trim()] ?: 0.0
                Triple(person, tot, (tot - paid).coerceAtLeast(0.0))
            }.sortedByDescending { it.third }
        // One compact row per person: who, which way, how much is left and how much has come back.
        val owe = borrowByPerson.sumOf { it.third }
        val people = byPerson.map { Triple(it, true, it.third <= 0.0) } + borrowByPerson.map { Triple(it, false, it.third <= 0.0) }
        val open = people.filter { !it.third }
        var peopleAll by remember { mutableStateOf(false) }
        Panel {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Eyebrow(stringResource(R.string.people))
                Spacer(Modifier.weight(1f))
                if (owedToYou > 0) Text(money(owedToYou), color = ZColor.Income, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                if (owedToYou > 0) Text(stringResource(R.string.to_get), color = ZColor.Muted, fontSize = 13.sp)
                if (owedToYou > 0 && owe > 0) Text(" · ", color = ZColor.Muted, fontSize = 13.sp)
                if (owe > 0) Text(money(owe), color = ZColor.Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                if (owe > 0) Text(stringResource(R.string.to_pay), color = ZColor.Muted, fontSize = 13.sp)
            }
            val shown = if (peopleAll) people.sortedBy { it.third } else open.take(3)
            shown.forEach { (p, theyOwe, settled) ->
                val (person, total, bal) = p
                val tone = if (theyOwe) ZColor.Income else ZColor.Ink
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .clickable { if (theyOwe) onOpenLend(person) else onOpenBorrow(person) }.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(36.dp).clip(CircleShape).background(ZColor.Tile), contentAlignment = Alignment.Center) {
                        Text(niceName(person).take(1).uppercase(), color = if (theyOwe) ZColor.Income else ZColor.Warn,
                            fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    // Name across the full width, with how much came back under it; the amount and who owes whom on the right.
                    Column(Modifier.weight(1f)) {
                        Text(niceName(person), color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val back = total - bal
                        if (!settled && back >= 1) {
                            Spacer(Modifier.height(5.dp))
                            Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(ZColor.Line)) {
                                Box(Modifier.fillMaxWidth((back / total).toFloat().coerceIn(0f, 1f)).fillMaxHeight()
                                    .background(if (theyOwe) ZColor.Income else ZColor.Accent))
                            }
                            Spacer(Modifier.height(3.dp))
                            Text(money(back) + " of " + money(total) + " back", color = ZColor.Muted, fontSize = 12.sp, maxLines = 1)
                        } else Text(if (settled) "Settled" else if (theyOwe) "Lent " + money(total) else "Borrowed " + money(total),
                            color = ZColor.Muted, fontSize = 12.sp, maxLines = 1)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(money(if (settled) total else bal), color = if (settled) ZColor.Muted else tone,
                            fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(if (settled) "settled" else if (theyOwe) "owes you" else "you owe", color = ZColor.Muted, fontSize = 12.sp)
                    }
                }
            }
            if (people.size > shown.size || peopleAll) {
                Spacer(Modifier.height(8.dp))
                Text(if (peopleAll) "Show less" else "See all ›", color = ZColor.Accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.End).clip(RoundedCornerShape(8.dp)).clickable { peopleAll = !peopleAll }.padding(4.dp))
            }
        }
    }

}

@Composable internal fun DailyCalendarBlock(
    allRows: List<Row>, list: List<Row>, f: Analytics.Filter, month: String, account: String,
    daySel: Int, onDay: (Int) -> Unit, onRowClick: (Row) -> Unit,
    showCal: Boolean = true, showList: Boolean = true,
) {
    val bigIds = Analytics.bigSpendIds(list)
    val daily = Analytics.dailyTotals(allRows, month, account)
    if (showCal) Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Eyebrow(stringResource(R.string.daily_spend))
            InfoDot(stringResource(R.string.darker_days_cost_more_tap))
        }
        Spacer(Modifier.height(8.dp))
        Calendar(month, daily, daySel, onDay)
    }
    if (showList && daySel > 0) {
        val dayStr = "%s-%02d".format(month, daySel)
        val dayRows = list.filter { it.date == dayStr }.sortedByDescending { it.amount }
        val dtot = dayRows.filter { it.kind == Kind.EXPENSE }.rupees()
        Panel {
            Eyebrow(stringResource(R.string.spent, daySel, Dates.monthLong(month), money(dtot)))
            if (dayRows.isEmpty()) { Spacer(Modifier.height(6.dp)); MutedText(stringResource(R.string.no_transactions_on_this_day)) }
            else dayRows.forEach { TxnRow(it, bigIds.contains(it.id), onRowClick) }
        }
    }
}

@Composable internal fun Calendar(month: String, daily: DoubleArray, daySel: Int, onDay: (Int) -> Unit) {
    val dim = Dates.daysInMonth(month)
    val max = (daily.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
    val (y, mo) = month.split("-").map { it.toInt() }
    val cal = java.util.GregorianCalendar(Dates.IST); cal.clear(); cal.set(y, mo - 1, 1)
    val firstWeekday = (cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7 // Mon=0
    val cells = ArrayList<Int>() // 0 = blank
    repeat(firstWeekday) { cells.add(0) }
    for (d in 1..dim) cells.add(d)
    while (cells.size % 7 != 0) cells.add(0)

    Row(Modifier.fillMaxWidth()) {
        listOf("M", "T", "W", "T", "F", "S", "S").forEach {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { MutedText(it, 10) }
        }
    }
    cells.chunked(7).forEach { week ->
        Row(Modifier.fillMaxWidth().padding(top = 5.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            week.forEach { d ->
                if (d == 0) {
                    Box(Modifier.weight(1f).aspectRatio(1f))
                } else {
                    val v = daily[d]
                    val alpha = if (v <= 0) 0f else (0.18f + (v / max).toFloat() * 0.72f)
                    val bg = if (alpha == 0f) Color.Transparent else ZColor.Accent.copy(alpha = alpha)
                    Box(
                        Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(9.dp))
                            .background(bg)
                            .border(if (daySel == d) 2.dp else 0.dp, if (daySel == d) ZColor.Ink else Color.Transparent, RoundedCornerShape(9.dp))
                            .clickable { onDay(d) },
                        contentAlignment = Alignment.Center,
                    ) { Text("$d", color = ZColor.InkSoft, fontSize = 11.sp) }
                }
            }
        }
    }
}

// ---------- Buckets ----------
