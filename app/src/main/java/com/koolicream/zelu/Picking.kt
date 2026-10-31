package com.koolicream.zelu

import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup

/**
 * Picking several payments at once: hold a payment in any list to start, tap others to add or take them away, then
 * move them all to one category. Kept here, not per list, so every list (Home, Spend, a payee, a category) shares it.
 */
object Picking {
    val picked = mutableStateMapOf<String, Row>()
    val active: Boolean get() = picked.isNotEmpty()
    fun toggle(r: Row) { if (picked.containsKey(r.id)) picked.remove(r.id) else picked[r.id] = r }
    fun has(r: Row) = picked.containsKey(r.id)
    fun clear() = picked.clear()
}

/** The tick box shown in place of a payment's icon while picking. */
@Composable internal fun PickBox(on: Boolean) {
    Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(24.dp).clip(RoundedCornerShape(7.dp))
            .background(if (on) ZColor.Accent else ZColor.Paper)
            .border(2.dp, if (on) ZColor.Accent else ZColor.Line, RoundedCornerShape(7.dp)),
            contentAlignment = Alignment.Center) {
            if (on) Text("✓", color = ZColor.AccentInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** The bar at the bottom while picking: how many and how much, and the one thing to do with them. */
@Composable internal fun PickBar(onMove: (List<Row>, Kind, String, Boolean) -> Unit, onNewCat: (((String) -> Unit)) -> Unit) {
    if (!Picking.active) return
    var choosing by remember { mutableStateOf(false) }
    val rows = Picking.picked.values.toList()
    Popup(alignment = Alignment.BottomCenter) {
        Row(Modifier.windowInsetsPadding(WindowInsets.navigationBars).padding(start = 14.dp, end = 14.dp, bottom = 12.dp).fillMaxWidth()
            .clip(RoundedCornerShape(18.dp)).background(ZColor.Ink).padding(start = 6.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).clickable { Picking.clear() }, contentAlignment = Alignment.Center) {
                Text("✕", color = ZColor.Paper, fontSize = 16.sp)
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.n_selected, rows.size), color = ZColor.Paper, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(money(rows.sumOf { it.amount }), color = ZColor.Paper.copy(alpha = 0.7f), fontSize = 12.sp)
            }
            Box(Modifier.clip(RoundedCornerShape(12.dp)).background(ZColor.Accent).clickable { choosing = true }
                .padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(stringResource(R.string.change_category) + " ›", color = ZColor.AccentInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
    if (choosing) {
        var remember by remember { mutableStateOf(false) }
        fun pick(kind: Kind, c: String) { choosing = false; onMove(rows, kind, c, remember); Picking.clear() }
        val out = rows.any { it.direction == Dir.DEBIT }
        val inn = rows.any { it.direction == Dir.CREDIT }
        Dialog(onDismissRequest = { choosing = false }) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(18.dp)) {
                Text(stringResource(R.string.move_n_payments_to, rows.size), color = ZColor.Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                PickList(buildList {
                    if (out) add(stringResource(R.string.pick_spending) to (
                        Classifier.CATEGORIES.keys.map { c -> catPick(c, false) { pick(if (c == "Investment") Kind.INVEST else Kind.EXPENSE, c) } } +
                            CustomCats.names.map { c -> catPick(c, false) { pick(Kind.EXPENSE, c) } } +
                            kindPick("new", stringResource(R.string.new_label), false) { onNewCat { c -> pick(Kind.EXPENSE, c) } }))
                    if (inn) add(stringResource(R.string.pick_money_in) to (IncomeCats.all.map { c -> catPick(c, false) { pick(Kind.INCOME, c) } } +
                        kindPick("income", stringResource(R.string.other_income), false) { pick(Kind.INCOME, "Income") }))
                    add(stringResource(R.string.pick_other) to buildList {
                        add(kindPick("transfer", stringResource(R.string.transfer), false) { pick(Kind.TRANSFER, "Transfer") })
                        if (out && !inn) add(kindPick("lent", stringResource(R.string.lent), false) { pick(Kind.LENT, "Lent") })
                        if (inn && !out) add(kindPick("refund", stringResource(R.string.refund), false) { pick(Kind.REFUND, "Refund") })
                    })
                })
                Spacer(Modifier.height(10.dp))
                // Off by default: only the ticked payments change unless you say their payees' other payments should too.
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { remember = !remember }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(20.dp).clip(RoundedCornerShape(6.dp)).background(if (remember) ZColor.Accent else ZColor.Paper)
                        .border(2.dp, if (remember) ZColor.Accent else ZColor.Line, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                        if (remember) Text("✓", color = ZColor.AccentInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(stringResource(R.string.also_remember_payees), color = ZColor.Ink, fontSize = 13.sp)
                }
                Spacer(Modifier.width(1.dp))
            }
        }
    }
}

/**
 * "Apply to similar messages?": the payments whose message reads like the one you just changed (same bank, words and
 * place), each with a tick box, all ticked to start. Only the ticked ones change. With "new messages too" on, later
 * messages like these follow the same choice.
 */
@Composable internal fun SimilarDialog(ask: AppState.SimilarAsk, onApply: (Set<String>, Boolean) -> Unit, onDismiss: () -> Unit) {
    val ticked = remember(ask) { mutableStateMapOf<String, Boolean>().apply { ask.others.forEach { put(it.id, true) } } }
    var future by remember(ask) { mutableStateOf(true) }
    val what = ask.title ?: ask.category?.let { if (it in CustomCats.names) customLabel(it) else it } ?: ""
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(18.dp)) {
            Text(stringResource(R.string.similar_title, ask.others.size), color = ZColor.Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            MutedText(stringResource(R.string.similar_sub, what), 12)
            Spacer(Modifier.height(10.dp))
            Column(Modifier.heightIn(max = 340.dp).verticalScroll(rememberScrollState())) {
                ask.others.forEach { r ->
                    val on = ticked[r.id] == true
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { ticked[r.id] = !on }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        PickBox(on)
                        Column(Modifier.weight(1f)) {
                            Text(prettyDate(r.date) + " · " + (if (r.category.isNotBlank()) r.category else ""), color = ZColor.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text(r.raw, color = ZColor.Muted, fontSize = 11.sp, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(money(r.amount), color = ZColor.Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { future = !future }.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(20.dp).clip(RoundedCornerShape(6.dp)).background(if (future) ZColor.Accent else ZColor.Paper)
                    .border(2.dp, if (future) ZColor.Accent else ZColor.Line, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                    if (future) Text("✓", color = ZColor.AccentInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Text(stringResource(R.string.similar_future), color = ZColor.Ink, fontSize = 13.sp)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.only_this_one), color = ZColor.Muted, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { onDismiss() }.padding(12.dp))
                Spacer(Modifier.width(6.dp))
                val n = ticked.count { it.value }
                Box(Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink).clickable { onApply(ticked.filterValues { it }.keys, future) }
                    .padding(horizontal = 18.dp, vertical = 10.dp)) {
                    Text(stringResource(R.string.apply_to_n, n), color = ZColor.Paper, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
