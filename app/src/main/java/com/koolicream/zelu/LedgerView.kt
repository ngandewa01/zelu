package com.koolicream.zelu

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
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.TrendingUp
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

@Composable internal fun TxnRow(t: Row, highlight: Boolean, onClick: (Row) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick(t) }.padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically) {
        MerchantTile(t.counterparty ?: t.category, t.category, kind = t.kind)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(t.counterparty?.takeIf { it.isNotBlank() } ?: t.category,
                color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            val at = TxnTime.label(t)
            MutedText("${t.date}${if (at.isNotEmpty()) " · $at" else ""} · ${subtitleFor(t)}", 11)
            // A foreign-currency card spend: show what was actually charged, and that the rupee amount is an estimate.
            val fx = remember(t.raw) { Parser.foreign(t.raw) }
            if (fx != null) {
                Spacer(Modifier.height(3.dp))
                Text("🌐 " + Fx.label(fx) + if (fx.estimated) " · approx. rupees" else "",
                    color = ZColor.Accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(ZColor.OrangeSoft)
                        .padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
        Spacer(Modifier.width(10.dp))
        AmountText(t, highlight)
    }
}

@Composable internal fun AmountText(t: Row, highlight: Boolean) {
    when (t.kind) {
        "transfer" -> Text("Transfer", color = ZColor.Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        "invest" -> Text(money(t.amount), color = ZColor.Muted, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        "ignore" -> Text("—", color = ZColor.Muted, fontSize = 14.sp)
        "income", "refund", "borrowed" ->
            Text("+" + money(t.amount), color = ZColor.Income, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        else -> Text("−" + money(t.amount),
            color = if (highlight && t.kind == "expense") ZColor.Accent else ZColor.Warn,
            fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ---------- Ledger ----------

// A credit-card bill settles spends already counted, so it is not new spending.
private fun isCardBill(r: Reminder) = r.minAmount != null || r.account.contains(" CC ") || r.account.startsWith("CC ") || r.account.endsWith(" CC")

@Composable internal fun LedgerView(
    allRows: List<Row>, list: List<Row>, f: Analytics.Filter, accounts: List<String>,
    month: String, account: String, daySel: Int, onDay: (Int) -> Unit,
    askOpen: Boolean, setAskOpen: (Boolean) -> Unit,
    decide: (String, String, String, String?, Boolean) -> Unit,
    onRowClick: (Row) -> Unit,
    onDrill: (String, List<Row>) -> Unit,
    excludeEmi: Boolean = false,
    repaid: Map<String, Double> = emptyMap(),
    onOpenLend: (String) -> Unit = {},
    onOpenBorrow: (String) -> Unit = {},
    fixed: Map<String, Double> = emptyMap(),
    onOpenFixed: () -> Unit = {},
    balances: Map<String, Pair<Double, Long>> = emptyMap(),
    onNewCat: (((String) -> Unit)) -> Unit = {},
    limits: Map<String, Pair<Double, Long>> = emptyMap(),
    reminders: List<Reminder> = emptyList(),
    onOpenCard: (String) -> Unit = {},
    onToggleEmi: () -> Unit = {},
    onEditBudget: () -> Unit = {},
    onOpenDues: () -> Unit = {},
    reminderState: Map<String, Boolean> = emptyMap(),
    refundSuggestions: List<Pair<Row, Row>> = emptyList(),
    onRefundAnswer: (String, Boolean) -> Unit = { _, _ -> },
    budgets: Map<String, Double> = emptyMap(),
    onOpenSpend: () -> Unit = {},
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
                    ) { onDrill(c, allRows.filter { it.kind == "expense" && it.category == c && it.date.startsWith(f.month) }) })
                }
                if (b > 0 && v >= b * 0.8) {
                    val over = v > b
                    alerts.add(Alert(over, v / b, CatIcons.forCategory(c) ?: Icons.Rounded.Warning,
                        c,
                        if (over) money(v - b) + " over your " + money(b) + " budget"
                        else money(b - v) + " left of your " + money(b) + " budget",
                    ) { onDrill(c, allRows.filter { it.kind == "expense" && it.category == c && it.date.startsWith(f.month) }) })
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
        val budget = Analytics.BUDGET
        val emiAmt = list.filter { it.kind == "expense" && it.category == "EMI" }.sumOf { it.amount }
        val today = Dates.today()
        // Bills still to pay this month. A card bill pays for spends already counted, so it doesn't use up the budget.
        val billsLeft = if (!liveMonth) 0.0 else reminders.filter { r ->
            !(reminderState[r.id] ?: r.done) && r.dueDate.take(7) == f.month && r.dueDate >= today && !isCardBill(r)
        }.sumOf { it.amount }
        val daysLeft = if (liveMonth) Dates.daysInMonth(f.month) - Dates.dayOfMonth() + 1 else 0
        val free = budget - s.spent - billsLeft
        if (liveMonth && budget > 0) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Eyebrow("Safe to spend" + if (excludeEmi) " · after EMI" else "")
                Text("Budget " + money(budget) + " ›", color = ZColor.Accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onEditBudget() })
            }
            Spacer(Modifier.height(4.dp))
            if (free > 0) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(money(free / daysLeft), color = ZColor.Ink, fontSize = 38.sp, fontWeight = FontWeight.Bold)
                    Text(" a day", color = ZColor.Muted, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 7.dp))
                }
                MutedText(if (daysLeft == 1) "for today, after bills still due" else "for the next $daysLeft days, after bills still due", 13)
            } else {
                Text(money(-free) + " over", color = ZColor.Warn, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                MutedText(if (billsLeft > 0) "Spending and bills still due are past your budget" else "Spending is past your budget", 13)
            }
            // The month in one bar: spent, still due, free.
            Spacer(Modifier.height(12.dp))
            val parts = listOf(s.spent to ZColor.Accent, billsLeft to ZColor.AmberInk, free.coerceAtLeast(0.0) to ZColor.Income.copy(alpha = 0.35f))
            Row(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)).background(ZColor.Paper)) {
                parts.filter { it.first > 0.5 }.forEach { (v, c) -> Box(Modifier.weight(v.toFloat()).fillMaxHeight().background(c)) }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Fact("Spent", money(s.spent))
                Fact("Bills due", money(billsLeft))
                Fact("Free", money(free.coerceAtLeast(0.0)), if (free > 0) ZColor.Income else ZColor.Warn)
            }
        } else {
            // Past months, other periods, or no budget yet: what was spent, against the month before.
            Eyebrow("Spent in " + Analytics.periodLabel(f) + if (excludeEmi) " · after EMI" else "")
            val prevMonth = if (isMonth) Dates.shiftMonth(f.month, -1) else ""
            val cutDay = if (liveMonth) Dates.dayOfMonth() else 31
            val prevSpent = if (!isMonth) 0.0 else Analytics.filter(allRows, f.copy(month = prevMonth))
                .filter { it.kind == "expense" && it.date.substring(8, 10).toInt() <= cutDay && !(excludeEmi && it.category == "EMI") }.sumOf { it.amount }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text(money(s.spent), color = ZColor.Ink, fontSize = 38.sp, fontWeight = FontWeight.Bold)
                if (prevSpent > 0 && s.spent > 0) {
                    val diff = Math.round((s.spent - prevSpent) / prevSpent * 100).toInt()
                    val down = diff <= 0
                    Box(Modifier.padding(bottom = 8.dp).clip(RoundedCornerShape(10.dp)).background(if (down) ZColor.IncomeSoft else ZColor.WarnSoft)
                        .padding(horizontal = 9.dp, vertical = 4.dp)) {
                        Text((if (down) "↓ " else "↑ ") + Math.abs(diff) + "% vs " + Dates.monthLabel(prevMonth).take(3),
                            color = if (down) ZColor.Income else ZColor.Warn, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (isMonth && budget > 0) {
                Spacer(Modifier.height(6.dp))
                Text(if (s.spent > budget) money(s.spent - budget) + " over the " + money(budget) + " budget"
                    else money(budget - s.spent) + " under the " + money(budget) + " budget",
                    color = if (s.spent > budget) ZColor.Warn else ZColor.Income, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onEditBudget() })
            } else if (liveMonth) {
                // No budget yet: suggest one from what a month usually costs.
                val usual = (1..3).map { i ->
                    Analytics.filter(allRows, f.copy(month = Dates.shiftMonth(f.month, -i)))
                        .filter { it.kind == "expense" && !(excludeEmi && it.category == "EMI") }.sumOf { it.amount }
                }.filter { it > 0 }.let { if (it.isEmpty()) 0.0 else it.average() }
                Spacer(Modifier.height(8.dp))
                Text("Set a monthly budget ›", color = ZColor.Accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onEditBudget() })
                if (usual > 0) MutedText("You usually spend " + money(usual) + " a month. Zelu then shows what you can spend each day.", 12)
            }
        }

        // Where the month is heading, and the money that came in.
        @Composable fun infoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: androidx.compose.ui.graphics.Color, label: String,
                                value: String, valueColor: androidx.compose.ui.graphics.Color, onClick: (() -> Unit)?) {
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(ZColor.Paper)
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier).padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(label, color = ZColor.InkSoft, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text(value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
        // Month-end estimate from the same numbers as above: spent, bills still due, and a normal day's spending
        // (last 30 days, big one-offs left out) for the days still to come.
        val forecast = if (!liveMonth || s.spent <= 0) 0.0 else {
            val base = allRows.filter { (account == "All" || it.account == account) && !(excludeEmi && it.category == "EMI") }
            s.spent + billsLeft + Analytics.typicalDaily(base, today) * (daysLeft - 1).coerceAtLeast(0)
        }
        val overPace = liveMonth && budget > 0 && forecast > budget
        if (forecast > 0) {
            val end = Dates.daysInMonth(f.month).toString() + " " + Dates.monthLabel(f.month).take(3)
            val verdict = when {
                budget <= 0 -> ""
                forecast > budget -> " · " + money(forecast - budget) + " over"
                else -> " · under"
            }
            infoRow(if (overPace) Icons.Rounded.Warning else Icons.Rounded.TrendingUp, if (overPace) ZColor.Warn else ZColor.Income,
                "At this pace, by $end", "≈ " + money(forecast) + verdict, if (overPace) ZColor.Warn else ZColor.Income, onOpenDues)
        }
        // Running hot: name the category that grew most against the same days last month; the why is on Spend.
        if (liveMonth && (overPace || free <= 0)) {
            val cut = Dates.dayOfMonth()
            val prev = Analytics.filter(allRows, f.copy(month = Dates.shiftMonth(f.month, -1)))
                .filter { it.kind == "expense" && it.date.substring(8, 10).toInt() <= cut }.groupBy { it.category }
                .mapValues { (_, rs) -> rs.sumOf { it.amount } }
            list.filter { it.kind == "expense" }.groupBy { it.category }.mapValues { (_, rs) -> rs.sumOf { it.amount } }
                .map { (c, v) -> c to v - (prev[c] ?: 0.0) }.filter { it.second >= 1000 }.maxByOrNull { it.second }
                ?.let { (c, up) -> infoRow(Icons.Rounded.Lightbulb, ZColor.AmberInk, "$c: " + money(up) + " more than this time last month",
                    "Why ›", ZColor.Accent, onOpenSpend) }
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
        if (emiAmt > 0 || excludeEmi) {
            Spacer(Modifier.height(10.dp))
            Text(if (excludeEmi) "EMI left out · Include ›" else "Includes " + money(emiAmt) + " of EMI · Leave out ›",
                color = ZColor.Muted, fontSize = 12.sp, modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { onToggleEmi() })
        }
    }

    // Bills before month-end (or the next one when none are left this month).
    run {
        val today = Dates.today()
        val open = reminders.filter { r -> !(reminderState[r.id] ?: r.done) && r.dueDate >= today }.sortedBy { it.dueDate }
        val nowMonth = Dates.todayMonth()
        val monthEnd = nowMonth + "-%02d".format(Dates.daysInMonth(nowMonth))
        val thisMonth = open.filter { it.dueDate <= monthEnd }
        val shownBills = thisMonth.take(2).ifEmpty { open.take(1) }
        if (shownBills.isNotEmpty()) Panel {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onOpenDues() },
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Eyebrow(if (thisMonth.isNotEmpty()) "Bills before month-end" else "Next bill")
                Text(if (thisMonth.size > 2) "All ${thisMonth.size} ›" else "All dues ›", color = ZColor.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            shownBills.forEach { r ->
                val name = r.counterparty?.takeIf { it.isNotBlank() } ?: "Bill"
                val inDays = Dates.daysBetween(today, r.dueDate)
                val whenText = "Due " + prettyDay(r.dueDate) + when (inDays) { 0 -> " · today"; 1 -> " · tomorrow"; in 2..9 -> " · in $inDays days"; else -> "" }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onOpenDues() }, verticalAlignment = Alignment.CenterVertically) {
                    MerchantTile(name, "", 34.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(name, color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        MutedText(whenText + if (isCardBill(r)) " · card bill" else "", 12)
                    }
                    Text(money(r.amount), color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Refund that looks like it belongs to an earlier spend — confirm before it nets against it.
    val snoozeCtx = LocalContext.current
    var snoozeTick by remember { mutableStateOf(0) }
    val refundAsk = if (snoozeTick >= 0 && Storage.refundSnoozed(snoozeCtx)) null else refundSuggestions.firstOrNull()
    refundAsk?.let { (rf, sp) ->
        Panel {
            Eyebrow("Needs you · refund")
            Spacer(Modifier.height(6.dp))
            Text(money(rf.amount) + " refund on " + rf.date, color = ZColor.Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            MutedText("Is it for " + (sp.counterparty?.takeIf { it.isNotBlank() } ?: sp.category) + " " + money(sp.amount) + " on " + sp.date + "?", 13)
            // The actual SMS for both, so you can check the match yourself.
            @Composable fun smsBox(label: String, r: Row) {
                Spacer(Modifier.height(10.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(ZColor.Paper)
                    .border(1.dp, ZColor.Line, RoundedCornerShape(12.dp)).padding(12.dp)) {
                    Text(label.uppercase() + " · " + r.date + (if (r.account.isNotBlank()) " · " + r.account else ""),
                        color = ZColor.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(r.raw.ifBlank { "(message text not stored)" }, color = ZColor.InkSoft, fontSize = 12.sp,
                        lineHeight = 17.sp, maxLines = 6, overflow = TextOverflow.Ellipsis)
                }
            }
            smsBox("Refund message", rf)
            smsBox("Spend it may match", sp)
            Spacer(Modifier.height(12.dp))
            ChipScroll {
                Chip("Yes, same spend", false, filled = true) { onRefundAnswer(rf.id, true) }
                Chip("No", false) { onRefundAnswer(rf.id, false) }
                Chip("Do it later", false) { Storage.snoozeRefund(snoozeCtx); snoozeTick++ }
            }
            MutedText("\"Do it later\" brings this back tomorrow.", 11)
        }
    }

    // Ask-me — tackle the biggest unknowns first.
    val pending = list.filter { it.kind == "expense" && it.category == "Unknown" }.sortedByDescending { it.amount }
    val next = pending.firstOrNull()
    // One "Needs you" card at a time keeps Home short: the refund question comes first.
    if (next != null && refundAsk == null) {
        Panel {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onDrill("Needs a category", pending) },
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Eyebrow("Needs you · ${pending.size} to go")
                if (pending.size > 1) Text("see all ›", color = ZColor.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(6.dp))
            Text(next.counterparty?.takeIf { it.isNotBlank() } ?: "Payment",
                color = ZColor.Ink, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
            MutedText("${money(next.amount)} on ${next.date}${if (next.account.isNotBlank()) " · ${next.account}" else ""}. What is this?", 13)
            Spacer(Modifier.height(10.dp))
            ChipScroll {
                Chip("Spending", false, filled = true) { setAskOpen(true) }
                Chip("Money in", false) { decide(next.id, "income", "Income", next.counterparty, false) }
                Chip("Lent", false) { decide(next.id, "lent", "Lent", next.counterparty, true) }
                Chip("Borrowed", false) { decide(next.id, "borrowed", "Borrowed", next.counterparty, true) }
                Chip("Ignore", false) { decide(next.id, "ignore", "Ignore", null, false) }
            }
            if (askOpen) {
                Spacer(Modifier.height(10.dp))
                MutedText("Pick a category (remembered for ${next.counterparty ?: "this payee"}):", 12)
                Spacer(Modifier.height(6.dp))
                ChipScroll {
                    Classifier.CATEGORIES.keys.forEach { c ->
                        Chip(c, false) { decide(next.id, if (c == "Investment") "invest" else "expense", c, next.counterparty, true) }
                    }
                    CustomCats.names.forEach { c ->
                        Chip(customLabel(c), false) { decide(next.id, "expense", c, next.counterparty, true) }
                    }
                    Chip("＋ New", false, filled = true) { onNewCat { c -> decide(next.id, "expense", c, next.counterparty, true) } }
                }
            }
        }
    }

    // Recent — horizontal strip
    Panel {
        val recent = list.filter { it.kind != "invest" }.newestFirst()
        Row(Modifier.fillMaxWidth().clickable { onDrill("All transactions", recent) },
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Eyebrow("Recent")
            Text("see all ›", color = ZColor.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(8.dp))
        if (recent.isEmpty()) MutedText("No transactions here.")
        else Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            recent.take(10).forEach { t ->
                Column(
                    Modifier.width(150.dp).clip(RoundedCornerShape(14.dp)).background(ZColor.Paper)
                        .border(1.dp, ZColor.Line, RoundedCornerShape(14.dp))
                        .clickable { onRowClick(t) }.padding(12.dp),
                ) {
                    MerchantTile(t.counterparty ?: "", t.category, 30.dp, t.kind)
                    Spacer(Modifier.height(8.dp))
                    Text(t.counterparty?.takeIf { it.isNotBlank() } ?: t.category, color = ZColor.Ink, fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (t.direction == "credit") money(t.amount) else money(t.amount), color = if (t.direction == "credit") ZColor.Income else ZColor.Ink,
                        fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    MutedText(prettyDay(t.date), 11)
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
        .filter { it !in hiddenAccts }
        .sortedByDescending { balances[it]?.first ?: -1.0 }
    var allAccountsOpen by remember { mutableStateOf(false) }
    // Accounts, cards and loans share one card with tabs, so Home stays short.
    val cardAccounts = accounts.filter { it.contains(" CC ") || it.startsWith("CC ") || it.endsWith(" CC") }
    val loanGroups = allRows.filter { it.category == "EMI" && Regex(".*\\bLoan \\d{3,4}$").matches(it.counterparty ?: "") }
        .groupBy { it.counterparty!! }
    var moneyTab by remember { mutableStateOf("accounts") }
    var closedLoans by remember { mutableStateOf(Storage.closedLoans(acctCtx)) }
    val hasAcc = acctNames.isNotEmpty(); val hasCard = cardAccounts.isNotEmpty(); val hasLoan = loanGroups.isNotEmpty()
    val moneyTabNow = when {
        moneyTab == "accounts" && hasAcc -> "accounts"
        moneyTab == "cards" && hasCard -> "cards"
        moneyTab == "loans" && hasLoan -> "loans"
        hasAcc -> "accounts"; hasCard -> "cards"; else -> "loans"
    }
    @Composable fun MoneyTabs() {
        val shown = listOf(hasAcc, hasCard, hasLoan).count { it }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            if (shown < 2) Eyebrow(when (moneyTabNow) { "accounts" -> "Accounts"; "cards" -> "Cards"; else -> "Loans" })
            else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (hasAcc) Chip("Accounts", moneyTabNow == "accounts") { moneyTab = "accounts" }
                if (hasCard) Chip("Cards", moneyTabNow == "cards") { moneyTab = "cards" }
                if (hasLoan) Chip("Loans", moneyTabNow == "loans") { moneyTab = "loans" }
            }
            // One switch for every balance and available limit on accounts and cards.
            androidx.compose.material3.Icon(
                if (Privacy.hideBalance) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, null,
                tint = if (Privacy.hideBalance) ZColor.Accent else ZColor.Muted,
                modifier = Modifier.size(34.dp).clip(CircleShape).clickable { Privacy.setBalance(acctCtx, !Privacy.hideBalance) }
                    .buttonLabel(if (Privacy.hideBalance) "Show balances" else "Hide balances").padding(6.dp))
        }
    }
    if (hasAcc && moneyTabNow == "accounts") {
        Panel {
            MoneyTabs()
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().clickable { allAccountsOpen = true },
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                MutedText("Total balance", 12)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (balances.isNotEmpty())
                        Text(if (Privacy.hideBalance) Privacy.MASK else money(balances.filterKeys { it !in hiddenAccts && it !in closedLoans && !it.contains(" DC ") }.values.sumOf { it.first }), color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text("see all ›", color = ZColor.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                acctNames.sortedBy { it in closedLoans }.forEach { a ->
                    ClosableFrame(a in closedLoans, 16.dp, Modifier.width(280.dp), onToggle = {
                        Storage.setLoanClosed(acctCtx, a, a !in closedLoans); closedLoans = Storage.closedLoans(acctCtx)
                    }) { AccountCard(a, balances[a], spentBy[a] ?: 0.0, Modifier.fillMaxWidth()) }
                }
            }
        }
    }
    if (allAccountsOpen) {
        Dialog(onDismissRequest = { allAccountsOpen = false }) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Paper).padding(16.dp)
                .heightIn(max = 640.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("All accounts", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                MutedText("Tap an account to see the SMS it came from. You can keep it or hide it.", 12)
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
                    Eyebrow("Hidden")
                    hiddenAccts.forEach { h ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(h, color = ZColor.Muted, fontSize = 14.sp)
                            Text("Show again", color = ZColor.Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold,
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
                MutedText("Zelu made this account from these messages. If they aren't about an account of yours, hide it.", 12)
                Spacer(Modifier.height(10.dp))
                val refs = sourceRefs
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    when {
                        refs == null -> MutedText("Looking for the messages…", 13)
                        refs.isEmpty() -> MutedText("No message found for it any more.", 13)
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
                    Text("Hide account", color = ZColor.Warn, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { Storage.setAccountHidden(acctCtx, a, true); hiddenAccts = Storage.hiddenAccounts(acctCtx); sourceFor = null }.padding(12.dp))
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink).clickable { sourceFor = null }
                        .padding(horizontal = 22.dp, vertical = 10.dp)) { Text("Keep", color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
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
                    val spent = cardRows.filter { it.kind == "expense" }.sumOf { it.amount }
                    val today = Dates.today()
                    val due = reminders.filter { r -> !r.done && r.dueDate >= today && r.account.takeLast(4) == a.takeLast(4) }
                        .minByOrNull { it.dueDate }
                    ClosableFrame(a in closedLoans, 18.dp, onToggle = {
                        Storage.setLoanClosed(acctCtx, a, a !in closedLoans); closedLoans = Storage.closedLoans(acctCtx)
                    }) { CardTile(a, spent, limits[a], limits["$a#total"], due, usedSince = Analytics.cardUsedSince(allRows, a, limits[a])) { onOpenCard(a) } }
                }
            }
            Spacer(Modifier.height(10.dp))
            MutedText("Tap a card to see its spends. Card spends count in your totals; paying the bill is a transfer.", 11)
        }
    }

    // Loans — found from EMI payments named after the loan ("HDFC Loan 529"), all-time.
    if (loanGroups.isNotEmpty() && moneyTabNow == "loans") {
        Panel {
            MoneyTabs()
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                loanGroups.entries.sortedWith(compareBy<Map.Entry<String, List<Row>>> { it.key in closedLoans }
                    .thenByDescending { e -> e.value.maxOf { it.date } }).forEach { (name, rs) ->
                    val last = rs.maxByOrNull { it.date }!!
                    val logo = bankLogoRes(name)
                    val closed = name in closedLoans
                    // A closed loan is frozen: cool blue instead of the usual card colours.
                    val ice = Color(0xFF6FA8D6)
                    Column(Modifier.width(220.dp).clip(RoundedCornerShape(16.dp))
                        .background(if (closed) ice.copy(alpha = 0.13f) else ZColor.Paper)
                        .border(1.dp, if (closed) ice.copy(alpha = 0.55f) else ZColor.Line, RoundedCornerShape(16.dp))
                        .clickable { onDrill(name, rs.newestFirst()) }.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                                if (logo != null) androidx.compose.foundation.Image(painter = androidx.compose.ui.res.painterResource(logo),
                                    contentDescription = null, modifier = Modifier.size(22.dp))
                                else BankMark(name)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(name, color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (closed) Text("❄ Closed", color = ice, fontSize = 11.sp, fontWeight = FontWeight.Bold) else MutedText("Loan", 11)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        MutedText(if (closed) "Last EMI" else "Monthly EMI", 11)
                        Text(money(last.amount), color = if (closed) ZColor.Muted else ZColor.Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        MutedText("Last paid " + prettyDate(last.date), 11)
                        MutedText(plural(rs.size, "EMI", "EMIs") + " · " + money(rs.sumOf { it.amount }) + " paid", 11)
                        Spacer(Modifier.height(8.dp))
                        Text(if (closed) "Reopen" else "Mark as closed", color = if (closed) ice else ZColor.Accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { Storage.setLoanClosed(acctCtx, name, !closed); closedLoans = Storage.closedLoans(acctCtx) })
                    }
                }
            }
        }
    }

    // Lending — all-time so repayments and balances stay correct across months.
    val lentAll = allRows.filter { it.kind == "lent" }
    val borrowedAll = allRows.filter { it.kind == "borrowed" }
    if (lentAll.isNotEmpty() || borrowedAll.isNotEmpty()) {
        val byPerson = lentAll.groupBy { it.counterparty?.takeIf { c -> c.isNotBlank() } ?: "Someone" }
            .map { (person, rs) ->
                val total = rs.sumOf { it.amount }
                val paid = repaid[person.lowercase().trim()] ?: 0.0
                Triple(person, total, (total - paid).coerceAtLeast(0.0))
            }.sortedByDescending { it.third }
        val owedToYou = byPerson.sumOf { it.third }
        val borrowByPerson = borrowedAll.groupBy { it.counterparty?.takeIf { c -> c.isNotBlank() } ?: "Someone" }
            .map { (person, rs) ->
                val tot = rs.sumOf { it.amount }
                val paid = repaid["b:" + person.lowercase().trim()] ?: 0.0
                Triple(person, tot, (tot - paid).coerceAtLeast(0.0))
            }.sortedByDescending { it.third }
        Panel {
            Eyebrow("People · you get / you pay")
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Fact("Owed to you", money(owedToYou), ZColor.Income)
                Fact("You owe", money(borrowByPerson.sumOf { it.third }), ZColor.Warn)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Open balances first, settled ones at the end.
                byPerson.sortedBy { it.third <= 0.0 }.forEach { (person, total, bal) ->
                    PersonCard(person, total, bal, ZColor.Income, "owes you") { onOpenLend(person) }
                }
                borrowByPerson.sortedBy { it.third <= 0.0 }.forEach { (person, total, bal) ->
                    PersonCard(person, total, bal, ZColor.Warn, "you owe") { onOpenBorrow(person) }
                }
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
        Eyebrow("Daily spend")
        Spacer(Modifier.height(8.dp))
        Calendar(month, daily, daySel, onDay)
        Spacer(Modifier.height(8.dp))
        MutedText("Darker days cost more. Tap a date to filter this page to that day.", 12)
    }
    if (showList && daySel > 0) {
        val dayStr = "%s-%02d".format(month, daySel)
        val dayRows = list.filter { it.date == dayStr }.sortedByDescending { it.amount }
        val dtot = dayRows.filter { it.kind == "expense" }.sumOf { it.amount }
        Panel {
            Eyebrow("$daySel ${Dates.monthLong(month)} · ${money(dtot)} spent")
            if (dayRows.isEmpty()) { Spacer(Modifier.height(6.dp)); MutedText("No transactions on this day.") }
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
