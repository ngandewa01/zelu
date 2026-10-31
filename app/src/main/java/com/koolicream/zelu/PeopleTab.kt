package com.koolicream.zelu

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** One person's money with you: what is left of what you lent them, and of what you borrowed from them. */
internal class PersonBalance(val name: String, val lent: Double, val lentLeft: Double, val borrowed: Double, val borrowedLeft: Double, val splits: Int) {
    val net: Double get() = lentLeft - borrowedLeft
}

/** Every person you lent to or borrowed from (splits included), with repayments taken off. All time, so balances stay right. */
internal fun personBalances(allRows: List<Row>, repaid: Map<String, Double>): List<PersonBalance> {
    fun who(r: Row) = r.counterparty?.takeIf { it.isNotBlank() } ?: "Someone"
    val lent = allRows.filter { it.kind == Kind.LENT }.groupBy { who(it) }
    val borrowed = allRows.filter { it.kind == Kind.BORROWED }.groupBy { who(it) }
    return (lent.keys + borrowed.keys).map { p ->
        val key = p.lowercase().trim()
        val l = lent[p].orEmpty().rupees(); val b = borrowed[p].orEmpty().rupees()
        PersonBalance(p, l, (l - (repaid[key] ?: 0.0)).coerceAtLeast(0.0), b, (b - (repaid["b:$key"] ?: 0.0)).coerceAtLeast(0.0),
            lent[p].orEmpty().count { it.id.contains('#') })
    }.sortedWith(compareByDescending<PersonBalance> { kotlin.math.abs(it.net) > 0.5 }.thenByDescending { kotlin.math.abs(it.net) })
}

// Dues › People: what everyone owes you and what you owe, one card per person and per group.
@Composable internal fun PeopleTab(
    allRows: List<Row>, repaid: Map<String, Double>, groups: Map<String, List<String>>, groupOf: Map<String, String>,
    onOpenLend: (String) -> Unit, onOpenBorrow: (String) -> Unit, onDrill: (String, List<Row>) -> Unit,
) {
    val people = personBalances(allRows, repaid)
    val get = people.sumOf { it.net.coerceAtLeast(0.0) }
    val pay = people.sumOf { (-it.net).coerceAtLeast(0.0) }
    Spacer(Modifier.height(12.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        @Composable fun androidx.compose.foundation.layout.RowScope.tile(label: String, amount: Double, tone: androidx.compose.ui.graphics.Color, arrow: String) {
            Column(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(ZColor.Card).border(1.dp, ZColor.Line, RoundedCornerShape(18.dp))
                .padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                MutedText(label, 12)
                Text(money(amount) + " " + arrow, color = tone, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
        tile(stringResource(R.string.you_get), get, ZColor.Income, "↙")
        tile(stringResource(R.string.you_pay), pay, ZColor.Warn, "↗")
    }
    if (people.isEmpty() && groups.isEmpty()) {
        Spacer(Modifier.height(12.dp))
        Panel { MutedText(stringResource(R.string.people_empty), 13) }
        return
    }
    val byName = people.associateBy { it.name }
    @Composable fun card(initial: String, title: String, sub: String, net: Double, members: List<String>, onClick: () -> Unit) {
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).border(1.dp, ZColor.Line, RoundedCornerShape(20.dp))
            .clickable { onClick() }.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(ZColor.Tile), contentAlignment = Alignment.Center) {
                Text(initial, color = if (net > 0.5) ZColor.Income else if (net < -0.5) ZColor.Warn else ZColor.Muted, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = ZColor.Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (members.isNotEmpty()) Row(Modifier.padding(top = 5.dp)) {
                    members.take(5).forEachIndexed { i, m ->
                        Box(Modifier.offset(x = (-6 * i).dp).size(24.dp).clip(CircleShape).background(ZColor.Tile).border(1.5.dp, ZColor.Card, CircleShape),
                            contentAlignment = Alignment.Center) {
                            Text(niceName(m).take(1).uppercase(), color = ZColor.Ink, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else MutedText(sub, 12)
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                when {
                    net > 0.5 -> { Text(money(net) + " ↙", color = ZColor.Income, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1); MutedText(stringResource(R.string.owes_you), 11) }
                    net < -0.5 -> { Text(money(-net) + " ↗", color = ZColor.Warn, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1); MutedText(stringResource(R.string.you_owe), 11) }
                    else -> { Text(stringResource(R.string.settled_tick), color = ZColor.Muted, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1); if (members.isNotEmpty()) MutedText(sub, 11) }
                }
            }
        }
    }
    groups.forEach { (g, members) ->
        val net = members.sumOf { byName[it]?.net ?: 0.0 }
        val n = groupOf.values.count { it == g }
        card(g.take(1).uppercase(), g, stringResource(R.string.group_splits, n), net, members) {
            val ids = groupOf.filterValues { it == g }.keys
            onDrill(g, allRows.filter { it.id.substringBefore('#') in ids })
        }
    }
    // People who are settled are history: folded under one line, like ended autopays.
    var showSettled by rememberSaveable { mutableStateOf(false) }
    val open = people.filter { kotlin.math.abs(it.net) > 0.5 }
    val settled = people.filter { kotlin.math.abs(it.net) <= 0.5 }
    (open + if (showSettled) settled else emptyList()).forEach { p ->
        val parts = buildList {
            if (p.splits > 0) add(plural(p.splits, "split", "splits"))
            val plainLent = p.lent - allRows.filter { it.kind == Kind.LENT && it.counterparty == p.name && it.id.contains('#') }.rupees()
            if (plainLent > 0.5) add("lent " + money(plainLent))
            if (p.borrowed > 0.5) add("borrowed " + money(p.borrowed))
        }
        card(niceName(p.name).take(1).uppercase(), niceName(p.name), parts.joinToString(" · "), p.net, emptyList()) {
            if (p.net < -0.5 || (p.lent <= 0.0 && p.borrowed > 0)) onOpenBorrow(p.name) else onOpenLend(p.name)
        }
    }
    if (settled.isNotEmpty()) Text(if (showSettled) stringResource(R.string.hide_settled) else stringResource(R.string.show_settled, settled.size),
        color = ZColor.Accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 10.dp).clip(RoundedCornerShape(8.dp)).clickable { showSettled = !showSettled }.padding(vertical = 6.dp, horizontal = 4.dp))
    Spacer(Modifier.height(12.dp))
    MutedText(stringResource(R.string.people_hint), 12)
}
