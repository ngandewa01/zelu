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
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.automirrored.rounded.CallSplit
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
            // A part of a split shows a small mark: people for a split with people, a fork for one split into categories.
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (t.id.contains('#')) {
                    val withPeople = t.id.substringBefore('#') in SplitMarks.people
                    androidx.compose.material3.Icon(if (withPeople) Icons.Rounded.Group else Icons.AutoMirrored.Rounded.CallSplit,
                        stringResource(if (withPeople) R.string.split_with_people else R.string.split_into_categories),
                        tint = ZColor.Muted, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                }
                val whole = SplitMarks.wholeOf(t)
                MutedText(prettyDate(t.date) + (if (at.isNotEmpty()) ", $at" else "") + " · " + subtitleFor(t) +
                    (if (whole != null) " · " + stringResource(R.string.yours_amount, money(t.amount)) else "") +
                    (if (fx != null) " · " + Fx.label(fx) else ""), 12)
            }
        }
        Spacer(Modifier.width(10.dp))
        // Your share of a split with people: the row shows the whole payment, your part is in the line under the name.
        AmountText(SplitMarks.wholeOf(t)?.let { t.copy(paise = it) } ?: t, highlight)
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
    onOpenPeople: () -> Unit = {},
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
            Box(Modifier.clip(RoundedCornerShape(20.dp)).background(if (excludeEmi) ZColor.OrangeSoft else ZColor.Paper)
                .clickable { onToggleEmi() }.padding(horizontal = 16.dp, vertical = 9.dp)) {
                Text(if (excludeEmi) "EMI off ✓" else "Exclude EMI", color = if (excludeEmi) ZColor.Accent else ZColor.Muted,
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
        // One shape with or without a budget: what you spent, a thin bar and one pill under it. Setting a budget fills the
        // pill and the bar; nothing else on the card moves. The bolder themes draw it on a two-colour gradient.
        val hero = ZColor.Hero
        val onHero = hero != null
        val heroInk = hero?.let { Color(it.ink) } ?: ZColor.Ink
        val soft = if (onHero) heroInk.copy(alpha = 0.82f) else ZColor.Muted
        val hasBudget = isMonth && budget > 0
        // heatColor: 2.0 is plenty of room (green), 0.0 is none (red); here, the share of the budget that is left, doubled.
        val heat = heatColor(if (hasBudget) 2.0 * (1.0 - s.spent / budget) else 2.0)
        val prevMonth = if (isMonth) Dates.shiftMonth(f.month, -1) else ""
        val cutDay = if (liveMonth) Dates.dayOfMonth() else 31
        val prevSpent = if (!isMonth) 0.0 else Analytics.filter(allRows, f.copy(month = prevMonth))
            .filter { it.kind == Kind.EXPENSE && it.date.substring(8, 10).toInt() <= cutDay && !(excludeEmi && it.category == "EMI") }.rupees()
        // Month-end estimate from the same numbers as above: spent, bills still due, and a normal day's spending
        // (last 30 days, big one-offs left out) for the days still to come.
        val forecast = if (!liveMonth || s.spent <= 0) 0.0 else {
            val base = allRows.filter { (account == "All" || it.account == account) && !(excludeEmi && it.category == "EMI") }
            s.spent + billsLeft + Analytics.typicalDaily(base, today) * (daysLeft - 1).coerceAtLeast(0)
        }
        Column(if (hero == null) Modifier.fillMaxWidth() else Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(hero.from), Color(hero.to)))).padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // The label gives way to the chip, which stays on one line.
                Box(Modifier.weight(1f).padding(end = 8.dp)) {
                    Eyebrow(stringResource(R.string.spent_label), soft)
                }
                if (prevSpent > 0 && s.spent > 0) {
                    val diff = Math.round((s.spent - prevSpent) / prevSpent * 100).toInt()
                    val down = diff <= 0
                    Box(Modifier.clip(RoundedCornerShape(10.dp)).background(if (down) ZColor.IncomeSoft else ZColor.WarnSoft)
                        .padding(horizontal = 9.dp, vertical = 4.dp)) {
                        Text((if (down) "↓ " else "↑ ") + Math.abs(diff) + "% vs " + Dates.monthLabel(prevMonth).take(3),
                            color = if (down) ZColor.Income else ZColor.Warn, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                }
            }
            // The number itself says how much of the budget is gone: 20 steps from green to red. With no budget it stays plain.
            Row(verticalAlignment = Alignment.Bottom) {
                if (onHero && hasBudget) Box(Modifier.padding(end = 8.dp, bottom = 14.dp).size(10.dp).clip(CircleShape).background(heat))
                Text(money(s.spent), color = if (onHero) heroInk else if (hasBudget) heat else ZColor.Ink, fontSize = 38.sp, fontWeight = FontWeight.Bold)
            }
            // The month as one line: spent so far, where it is heading, and the budget. Tapping it edits the budget.
            if (isMonth && s.spent > 0) {
                val dim = Dates.daysInMonth(f.month)
                val untilDay = if (liveMonth) Dates.dayOfMonth() else dim
                val perDay = DoubleArray(dim + 1)
                list.filter { it.kind == Kind.EXPENSE }.forEach { perDay[it.date.substring(8, 10).toInt().coerceIn(1, dim)] += it.paise / 100.0 }
                val running = ArrayList<Double>(untilDay)
                var acc = 0.0
                for (d in 1..untilDay) { acc += perDay[d]; running.add(acc) }
                val lineColor = if (onHero) heroInk else if (hasBudget) heat else ZColor.Ink
                Spacer(Modifier.height(10.dp))
                SpendLine(running, dim, untilDay, if (liveMonth) forecast else 0.0, if (hasBudget) budget else 0.0, lineColor,
                    if (onHero) heroInk else ZColor.Ink, soft, if (onHero) heroInk.copy(alpha = 0.18f) else ZColor.Line,
                    Modifier.clip(RoundedCornerShape(12.dp)).clickable { onEditBudget() })
            }
            // One row below: a link to set a budget when there is none, and the EMI switch at the other end.
            val showEmi = emiAmt > 0 || excludeEmi
            if ((!hasBudget && isMonth) || showEmi) {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (!hasBudget && isMonth) Text(stringResource(R.string.set_budget_action), color = if (onHero) heroInk else ZColor.Accent, fontSize = 14.sp,
                        fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { onEditBudget() }.padding(vertical = 8.dp, horizontal = 4.dp))
                    Spacer(Modifier.weight(1f))
                    emiPill()
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
        val recent = SplitMarks.forDisplay(list.filter { it.kind != Kind.INVEST }).newestFirst()
        // A payment that wants a look wears a small mark on its card (red: nothing sorted it; amber: a refund or a message
        // twice to check). Tapping the card opens the payment, where it is settled. The cards stay in date and time order.
        val strip = recent.take(10)
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
                        val mark = attention[t.id]
                        MerchantTile(t.counterparty ?: "", if (mark == Attention.UNKNOWN) "" else t.category, 36.dp, t.kind)
                        Spacer(Modifier.weight(1f))
                        if (mark != null) {
                            Box(Modifier.size(22.dp).clip(CircleShape).background(if (mark == Attention.UNKNOWN) ZColor.WarnSoft else ZColor.AmberSoft)
                                .spoken(stringResource(when (mark) { Attention.UNKNOWN -> R.string.mark_unsorted; Attention.REFUND -> R.string.mark_refund; Attention.DUPLICATE -> R.string.mark_duplicate })),
                                contentAlignment = Alignment.Center) {
                                Text(when (mark) { Attention.UNKNOWN -> "!"; Attention.REFUND -> "?"; Attention.DUPLICATE -> "=" },
                                    color = if (mark == Attention.UNKNOWN) ZColor.Warn else ZColor.AmberInk, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, modifier = Unspoken)
                            }
                        } else androidx.compose.material3.Icon(if (moneyIn) Icons.Rounded.SouthWest else Icons.Rounded.NorthEast,
                            if (moneyIn) "Money in" else "Spent", tint = if (moneyIn) ZColor.Income else ZColor.Muted,
                            modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(rowTitle(t), color = ZColor.Ink, fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text((if (moneyIn) "+" else "") + money(SplitMarks.wholeOf(t)?.let { it / 100.0 } ?: t.amount), color = if (moneyIn) ZColor.Income else ZColor.Ink,
                        fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    val at = TxnTime.label(t)
                    // A split shows its mark here too: people for a split with people, a fork for categories.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (t.id.contains('#')) {
                            val withPeople = t.id.substringBefore('#') in SplitMarks.people
                            androidx.compose.material3.Icon(if (withPeople) Icons.Rounded.Group else Icons.AutoMirrored.Rounded.CallSplit,
                                stringResource(if (withPeople) R.string.split_with_people else R.string.split_into_categories),
                                tint = ZColor.Muted, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(4.dp))
                        }
                        MutedText(if (t.category == "Unknown" && t.kind == Kind.EXPENSE) stringResource(R.string.unknown_label) else (if (at.isNotEmpty()) "$at · " else "") + prettyDay(t.date), 11)
                    }
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

    // People: one line with what you are owed and what you owe; the people themselves are on Dues › People.
    val peopleBal = remember(allRows, repaid) { personBalances(allRows, repaid) }
    if (peopleBal.isNotEmpty()) {
        val get = peopleBal.sumOf { it.net.coerceAtLeast(0.0) }
        val pay = peopleBal.sumOf { (-it.net).coerceAtLeast(0.0) }
        Panel {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onOpenPeople() }, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Eyebrow(stringResource(R.string.people_card))
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.people_card_sub, money(get), money(pay)), color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Text("›", color = ZColor.Accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
        val dayRows = SplitMarks.forDisplay(list.filter { it.date == dayStr }).sortedByDescending { it.amount }
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
                    Box(Modifier.weight(1f).aspectRatio(0.8f))
                } else {
                    val v = daily[d]
                    val alpha = if (v <= 0) 0f else (0.18f + (v / max).toFloat() * 0.72f)
                    val bg = if (alpha == 0f) Color.Transparent else ZColor.Accent.copy(alpha = alpha)
                    Box(
                        Modifier.weight(1f).aspectRatio(0.8f).clip(RoundedCornerShape(9.dp))
                            .background(bg)
                            .border(if (daySel == d) 2.dp else 0.dp, if (daySel == d) ZColor.Ink else Color.Transparent, RoundedCornerShape(9.dp))
                            .clickable { onDay(d) },
                        contentAlignment = Alignment.Center,
                    ) {
                        // The day and what it cost; on a dark (heavy) day the text turns light so it stays readable.
                        val ink = if (alpha > 0.55f) ZColor.Paper else ZColor.InkSoft
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$d", color = ink, fontSize = 11.sp, lineHeight = 13.sp, fontWeight = if (daySel == d) FontWeight.Bold else FontWeight.Normal)
                            if (v > 0) Text("₹" + shortAmt(v), color = ink, fontSize = 9.sp, lineHeight = 10.sp, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        }
    }
}

// ---------- Buckets ----------
