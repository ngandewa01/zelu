package com.koolicream.zelu

import androidx.compose.ui.res.stringResource

import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FiberNew
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable internal fun PayeesView(
    list: List<Row>, onDrill: (String, List<Row>) -> Unit,
    fixedSet: Analytics.FixedSet,
    allRows: List<Row> = emptyList(),
    onOpenPayee: (String) -> Unit = {},
) {
    val spend = list.filter { it.kind == Kind.EXPENSE }
    val payees = Analytics.byPayee(list)
    // Loan EMIs are large and fixed, so they would top every ranking: Top this month leaves them out and shows them as one line.
    val loanPayees = payees.filter { p -> p.cats.isNotEmpty() && p.cats.all { it == "EMI" } }
    val ranked = payees.filter { it !in loanPayees }
    var query by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf("amount") }
    var sortOpen by remember { mutableStateOf(false) }
    var limit by remember { mutableStateOf(10) }
    fun nameOf(r: Row) = r.counterparty?.takeIf { it.isNotBlank() } ?: r.category
    val lastDate = spend.groupBy { nameOf(it) }.mapValues { e -> e.value.maxOf { it.date } }
    // The month this view is about: the newest payment's month, so a month picked at the top is followed.
    val viewMonth = spend.maxOfOrNull { it.date }?.take(7) ?: Dates.todayMonth()

    Text(stringResource(R.string.who_you_paid), color = ZColor.Ink, fontSize = 30.sp, fontWeight = FontWeight.Bold)
    if (payees.isEmpty()) { Panel { MutedText(stringResource(R.string.no_payments_in_this_view)) }; return }

    @Composable fun payeeRow(p: Analytics.Payee) {
        val c0 = p.cats.firstOrNull() ?: "Unknown"
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onOpenPayee(p.name) }.padding(vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically) {
            MerchantTile(p.name, c0, 36.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(niceName(p.name), color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                MutedText(plural(p.count, "visit", "visits") + " · " + p.cats.joinToString(", ") { customLabel(it) }, 11)
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(money(p.amount), color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                lastDate[p.name]?.let { MutedText(prettyDate(it), 11) }
            }
        }
    }

    // Search first: typing turns the page into one list of matches.
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = query, onValueChange = { query = it; limit = 10 }, singleLine = true, shape = RoundedCornerShape(22.dp),
        placeholder = { Text(stringResource(R.string.search_payee)) }, modifier = Modifier.fillMaxWidth(),
    )
    val q = query.trim().lowercase()
    if (q.isNotEmpty()) {
        val found = payees.filter { it.name.lowercase().contains(q) }
        Panel {
            if (found.isEmpty()) MutedText(stringResource(R.string.no_payee_matches))
            else { Eyebrow(plural(found.size, "payee", "payees")); found.forEach { payeeRow(it) } }
        }
        return
    }

    // Top this month: the five everyday payees that took the most, and the loans as one line.
    Panel {
        Eyebrow(stringResource(R.string.top_this_month))
        ranked.take(5).forEach { payeeRow(it) }
        if (loanPayees.isNotEmpty()) {
            val loanRows = spend.filter { r -> r.category == "EMI" && loanPayees.any { it.name == nameOf(r) } }
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onDrill("Loan EMIs", loanRows) }.padding(vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically) {
                MerchantTile("", "EMI", 36.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.loan_emis), color = ZColor.Muted, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    MutedText(plural(loanPayees.size, "loan", "loans") + " ›", 11)
                }
                Text(money(loanPayees.sumOf { it.amount }), color = ZColor.Muted, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Regulars: who you pay month after month (milk, the maid, rent, the phone), and what they usually cost.
    val regulars = remember(allRows, viewMonth) { PatternMath.regulars(allRows, viewMonth).take(8) }
    // A regular may not be paid in the month shown, so its icon comes from its latest payment, not this month's.
    val lastCat = remember(allRows) { allRows.filter { it.kind == Kind.EXPENSE }.groupBy { nameOf(it) }.mapValues { (_, rs) -> rs.maxByOrNull { it.date }?.category ?: "Unknown" } }
    if (regulars.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        Panel {
            Eyebrow(stringResource(R.string.regulars))
            MutedText(stringResource(R.string.regulars_sub), 11)
            regulars.forEach { r ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onOpenPayee(r.name) }.padding(vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    MerchantTile(r.name, lastCat[r.name] ?: "Unknown", 36.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(niceName(r.name), color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        MutedText(stringResource(R.string.months_of, r.months, r.of), 11)
                    }
                    Text(stringResource(R.string.a_month_amount, money(r.perMonth)), color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
    }

    // New this month: first payments to someone, so an unfamiliar charge stands out.
    val firstSeen = remember(allRows) {
        val m = HashMap<String, String>()
        allRows.filter { it.kind == Kind.EXPENSE }.forEach { r -> val n = nameOf(r); val cur = m[n]; if (cur == null || r.date < cur) m[n] = r.date }
        m
    }
    val fresh = payees.filter { firstSeen[it.name]?.startsWith(viewMonth) == true }
    if (fresh.isNotEmpty() && firstSeen.size > fresh.size) {
        Spacer(Modifier.height(12.dp))
        Panel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { Eyebrow(stringResource(R.string.new_this_month)) }
                Text(fresh.size.toString(), color = ZColor.Income, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            fresh.take(6).forEach { payeeRow(it) }
        }
    }

    // Everyone, with one sort menu.
    val sortLabels = mapOf("amount" to stringResource(R.string.amount_2), "count" to stringResource(R.string.most_paid),
        "recent" to stringResource(R.string.recent), "az" to stringResource(R.string.a_to_z))
    val ordered = when (sort) {
        "count" -> payees.sortedByDescending { it.count }
        "recent" -> payees.sortedByDescending { lastDate[it.name] ?: "" }
        "az" -> payees.sortedBy { niceName(it.name).lowercase() }
        else -> payees
    }
    Spacer(Modifier.height(12.dp))
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { Eyebrow(stringResource(R.string.everyone)) }
            Box {
                Text((sortLabels[sort] ?: "") + " ▾", color = ZColor.Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { sortOpen = true }.padding(horizontal = 8.dp, vertical = 4.dp))
                androidx.compose.material3.DropdownMenu(expanded = sortOpen, onDismissRequest = { sortOpen = false }) {
                    sortLabels.forEach { (k, label) ->
                        androidx.compose.material3.DropdownMenuItem(text = { Text(label) }, onClick = { sort = k; sortOpen = false; limit = 10 })
                    }
                }
            }
        }
        ordered.take(limit).forEach { payeeRow(it) }
        if (ordered.size > limit || limit > 10) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (ordered.size > limit) Chip(stringResource(R.string.show_10_more) + (ordered.size - limit) + " left)", false) { limit += 10 }
                if (limit > 10) Chip(stringResource(R.string.show_less), false) { limit = 10 }
            }
        }
    }
}

// ---------- Investments ----------

@Composable internal fun InvestView(allRows: List<Row>, onDrill: (String, List<Row>) -> Unit) {
    val inv = Analytics.investments(allRows)
    Text(stringResource(R.string.investments), color = ZColor.Ink, fontSize = 30.sp, fontWeight = FontWeight.Bold)
    if (inv.isEmpty()) {
        Panel { MutedText(stringResource(R.string.pf_nps_and_fund_contributions)) }
        return
    }
    // What you put in is what left your account; a maturity or withdrawal coming back is shown apart, not added to it.
    val putIn = inv.filter { it.direction == Dir.DEBIT }
    val cameBack = inv.filter { it.direction == Dir.CREDIT }
    Panel {
        Eyebrow(stringResource(R.string.contributed))
        Spacer(Modifier.height(4.dp))
        Text(money(putIn.sumOf { it.amount }), color = ZColor.Income, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        if (cameBack.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.came_back_to_you, money(cameBack.sumOf { it.amount })), color = ZColor.Muted, fontSize = 13.sp,
                modifier = Modifier.clickable { onDrill("Came back to you", cameBack) })
        }
        Spacer(Modifier.height(4.dp))
        MutedText(stringResource(R.string.kept_out_of_spending_and), 11)
    }
    fun typeOf(r: Row) = r.counterparty?.takeIf { it.isNotBlank() } ?: "Investment"
    val byType = putIn.groupBy { typeOf(it) }
        .map { (name, rows) -> Triple(name, rows.sumOf { it.amount }, rows.size) }
        .sortedByDescending { it.second }
    val top = byType.firstOrNull()?.second ?: 1.0
    Panel {
        Eyebrow(stringResource(R.string.by_type))
        byType.forEachIndexed { i, (name, amt, cnt) ->
            Spacer(Modifier.height(if (i == 0) 8.dp else 14.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .clickable { onDrill(name, putIn.filter { typeOf(it) == name }) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MerchantTile(name, "Investment", 32.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(niceName(name), color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        MutedText("$cnt ${if (cnt == 1) "contribution" else "contributions"}", 11)
                    }
                    Text(money(amt), color = ZColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(7.dp))
                Meter((amt / top).toFloat(), catColor("Investment"))
            }
        }
    }
}

// ---------- Dues & Reminders ----------
