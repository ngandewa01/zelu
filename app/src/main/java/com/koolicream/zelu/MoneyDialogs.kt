package com.koolicream.zelu

import androidx.compose.ui.res.stringResource

import androidx.compose.material.icons.Icons
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Dialogs about payments and money: budgets, card limits, categories, splits, duplicates, and the ones opened
// from a payment or a list of payments.

@Composable
internal fun MonthlyBudgetDialog(s: AppState) {
    with(s) {
        if (budgetDlg) {
            var text by remember { mutableStateOf(fmtPlain(monthlyBudget)) }
            Dialog(onDismissRequest = { budgetDlg = false }) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp)) {
                    Text(stringResource(R.string.monthly_budget), color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    MutedText(stringResource(R.string.your_monthly_limit_0_for), 12)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = text, onValueChange = { v -> text = v.filter { it.isDigit() || it == '.' } },
                        singleLine = true, placeholder = { Text(stringResource(R.string.per_month_0_none)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(14.dp))
                    Box(
                        Modifier.align(Alignment.End).clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                            .clickable {
                                val v = text.toDoubleOrNull() ?: 0.0
                                scope.launch {
                                    withContext(Dispatchers.IO) { Storage.saveMonthlyBudget(context, v) }
                                    monthlyBudget = v
                                }
                                budgetDlg = false
                            }.padding(horizontal = 22.dp, vertical = 10.dp),
                    ) { Text(stringResource(R.string.save), color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

@Composable
internal fun CardLimitDialog(s: AppState) {
    with(s) {
        limitAskFor?.let { acct ->
            var txt by remember(acct) { mutableStateOf("") }
            Dialog(onDismissRequest = { limitAskFor = null }) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp)) {
                    Text(stringResource(R.string.card_limit), color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    MutedText(stringResource(R.string.your_bank_s_messages_for) + acct + " don't state the available limit. Enter what your bank app shows now. If a later message states it, Zelu uses that instead.", 12)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(value = txt, onValueChange = { v -> txt = v.filter { it.isDigit() || it == '.' } }, singleLine = true,
                        placeholder = { Text(stringResource(R.string.available_limit)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.cancel), color = ZColor.Muted, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { limitAskFor = null }.padding(12.dp))
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink).clickable {
                            val v = txt.toDoubleOrNull() ?: 0.0
                            if (v > 0) scope.launch {
                                withContext(Dispatchers.IO) { Storage.setManualLimit(context, acct, v) }
                                limits = withContext(Dispatchers.IO) { Storage.loadLimits(context) }
                                manualLimits = withContext(Dispatchers.IO) { Storage.manualLimitKeys(context) }
                                limitAskFor = null
                            }
                        }.padding(horizontal = 22.dp, vertical = 10.dp)) { Text(stringResource(R.string.save), color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
                    }
                }
            }
        }
    }
}

@Composable
internal fun CategoriesDialog(s: AppState) {
    with(s) {
        if (catsOpen) Dialog(onDismissRequest = { catsOpen = false }) {
            val names = (rows.filter { it.kind == Kind.EXPENSE || it.category in catOff }.map { it.category } + catOff).distinct().filter { it.isNotBlank() }.sorted()
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp).heightIn(max = 620.dp)) {
                var catTab by remember { mutableStateOf(0) }
                Text(stringResource(R.string.categories), color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KindTab("spending", stringResource(R.string.spending), catTab == 0) { catTab = 0 }
                    KindTab("income", stringResource(R.string.income), catTab == 1) { catTab = 1 }
                }
                Spacer(Modifier.height(10.dp))
                if (catTab == 1) IncomeCategoriesBody() else {
                MutedText(stringResource(R.string.tap_to_count_a_category), 12)
                Spacer(Modifier.height(10.dp))
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    names.forEach { c ->
                        val counts = c !in catOff
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable {
                            scope.launch {
                                withContext(Dispatchers.IO) { Storage.setCategoryCounts(context, c, !counts) }
                                catOff = withContext(Dispatchers.IO) { Storage.categoriesOff(context) }
                                refreshRows()
                            }
                        }.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            CatDot(c, 30.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(c, color = if (counts) ZColor.Ink else ZColor.Muted, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text(if (counts) "Counted" else "Not counted", color = if (counts) ZColor.Income else ZColor.Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.align(Alignment.End).clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                    .clickable { catsOpen = false }.padding(horizontal = 22.dp, vertical = 10.dp)) {
                    Text(stringResource(R.string.close), color = ZColor.Paper, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
internal fun SplitDialog(s: AppState) {
    with(s) {
        splitFor?.let { t ->
            val amts = remember(t.id) { mutableStateListOf(fmtPlain(t.amount), "") }
            val cats = remember(t.id) { mutableStateListOf(t.category, t.category) }
            val assigned = amts.sumOf { it.toDoubleOrNull() ?: 0.0 }
            val left = t.amount - assigned
            val ok = amts.size >= 2 && amts.all { (it.toDoubleOrNull() ?: 0.0) > 0 } && Math.abs(left) < 0.5
            Dialog(onDismissRequest = { splitFor = null }) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp)
                    .heightIn(max = 640.dp).verticalScroll(rememberScrollState())) {
                    Text(stringResource(R.string.split_payment), color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    MutedText((t.counterparty ?: t.category) + " · " + money(t.amount) + ". Give each part an amount and a category.", 12)
                    Spacer(Modifier.height(10.dp))
                    for (i in amts.indices) {
                        DropdownField("Part ${i + 1}", cats[i], CAT_OPTS.drop(1), catIcons = true, valueKey = cats[i]) { cats[i] = it }
                        OutlinedTextField(value = amts[i], onValueChange = { v -> amts[i] = v.filter { it.isDigit() || it == '.' } }, singleLine = true,
                            placeholder = { Text(stringResource(R.string.amount)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                    }
                    Text(if (Math.abs(left) < 0.5) "All assigned" else if (left > 0) money(left) + " left to assign" else money(-left) + " too much",
                        color = if (Math.abs(left) < 0.5) ZColor.Income else ZColor.Warn, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        if (amts.size < 5) Text(stringResource(R.string.add_part), color = ZColor.Accent, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { amts.add(if (left > 0) fmtPlain(left) else ""); cats.add(t.category) }.padding(vertical = 8.dp))
                        else Spacer(Modifier.width(1.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.cancel), color = ZColor.Muted, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { splitFor = null }.padding(12.dp))
                            Spacer(Modifier.width(6.dp))
                            Box(Modifier.clip(RoundedCornerShape(20.dp)).background(if (ok) ZColor.Ink else ZColor.Line).clickable {
                                if (ok) {
                                    val parts = amts.indices.map { (amts[it].toDoubleOrNull() ?: 0.0) to cats[it] }
                                    scope.launch {
                                        withContext(Dispatchers.IO) { Storage.setSplit(context, t.id, parts) }
                                        refreshRows()
                                        splitFor = null
                                        Toast.makeText(context, context.getString(R.string.split_into_parts, parts.size), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }.padding(horizontal = 22.dp, vertical = 10.dp)) { Text(stringResource(R.string.save), color = if (ok) ZColor.Paper else ZColor.Muted, fontWeight = FontWeight.SemiBold) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun PaymentDialogs(s: AppState, v: Filtered) {
    with(s) { with(v) {
        drill?.let { (title, snap) ->
            // The list is taken when it opens; show each payment as it is now, so a change made from it shows at once.
            val byId = rows.associateBy { it.id }
            DrillDialog(title, snap.map { byId[it.id] ?: it }, onRowClick = { detailRow = it }, onDismiss = { drill = null })
        }

        if (savingsOpen) SavingsDialog(rows, onDismiss = { savingsOpen = false })
        budgetFor?.let { c ->
            CategoryBudgetDialog(c, budgets[c] ?: 0.0, onSave = { amt -> setBudget(c, amt); budgetFor = null }, onDismiss = { budgetFor = null })
        }
        if (budgetsOpen) {
            val spendCats = Analytics.byCategory(list).map { it.first }
            val allCats = (fixed.keys + spendCats + budgets.keys + Classifier.CATEGORIES.keys + CustomCats.names).distinct()
            BudgetsDialog(allCats, budgets, fixed, onSet = { c, a -> setBudget(c, a) }, onFixed = { c, a -> setFixed(c, a) },
                onDismiss = { budgetsOpen = false }, monthly = monthlyBudget, onMonthly = { budgetsOpen = false; budgetDlg = true })
        }

        lendPerson?.let { person ->
            val pRows = rows.filter { it.kind == Kind.LENT && (it.counterparty ?: "Someone") == person }
            LendingDialog(
                person = person,
                lentRows = pRows,
                repaidSoFar = repaid[person.lowercase().trim()] ?: 0.0,
                onRowClick = { lendPerson = null; detailRow = it },
                onSave = { amt -> setRepayment(person.lowercase().trim(), amt) },
                onDismiss = { lendPerson = null },
            )
        }

        borrowPerson?.let { person ->
            val pRows = rows.filter { it.kind == Kind.BORROWED && (it.counterparty ?: "Someone") == person }
            LendingDialog(
                person = person,
                lentRows = pRows,
                repaidSoFar = repaid["b:" + person.lowercase().trim()] ?: 0.0,
                onRowClick = { borrowPerson = null; detailRow = it },
                onSave = { amt -> setRepayment("b:" + person.lowercase().trim(), amt) },
                onDismiss = { borrowPerson = null },
                youOwe = true,
            )
        }

        detailRow?.let { opened ->
            // Stays open after a change and shows the payment as saved.
            // After a split or its removal the payment's id changes (x becomes x#1, or back): find it again so the page stays open.
            val r = rows.firstOrNull { it.id == opened.id }
                ?: rows.firstOrNull { it.id.substringBefore('#') == opened.id.substringBefore('#') && it.kind != Kind.LENT } ?: opened
            val splitParts = if (r.id.contains('#')) rows.filter { it.id.contains('#') && it.id.substringBefore('#') == r.id.substringBefore('#') }.sortedBy { it.id } else emptyList()
            // What this payment may need settled: a purchase its refund may belong to, or the payment it may be a copy of.
            val refundOf = refundSuggestions.firstOrNull { it.first.id == r.id }?.second
            val copyOf = Analytics.possibleDuplicates(rows, dupOk).firstOrNull { it.first.id == r.id || it.second.id == r.id }
                ?.let { (a, b) -> if (a.id == r.id) b else a }
            // How often this payee was paid, and its history is one tap away, even for a first payment.
            val payee = r.counterparty?.takeIf { it.isNotBlank() }
            val visits = if (payee == null) 0 else rows.count { it.kind == Kind.EXPENSE && it.counterparty == payee }
            TxnDetailDialog(
                r,
                visits = visits, onOpenPayee = if (payee != null && visits >= 1) ({ detailRow = null; drill = null; open(Page.Payee(payee)) }) else null,
                refundOf = refundOf, onRefundAnswer = { yes -> answerRefund(r.id, yes) },
                copyOf = copyOf, onSameCopy = { hideDuplicate(r, copyOf!!) }, onTwoPayments = { keepBothDuplicates(r, copyOf!!) },
                onReclassify = { kind, cat -> decide(r.id, kind, cat, r.counterparty, kind != Kind.INCOME && kind != Kind.IGNORE) },
                onNewCat = { cb -> newCatCallback = cb },
                onRetitle = { title -> retitle(r.id, title) },
                onSplit = if (r.kind == Kind.EXPENSE || r.id.contains('#')) ({
                    // The payment page stays open: it shows the split (or the whole payment again) once saved.
                    if (r.id.contains('#')) removeSplit(r) else splitChoice = r
                }) else null,
                onDelete = { detailRow = null; deletePayment(r) },
                onLink = { what -> detailRow = null; linkFor = r to what },
                onRemoveSplit = if (r.id.contains('#')) ({ removeSplit(r) }) else null,
                splitParts = splitParts,
                onCount = { on -> setCounted(r, on) },
                onDismiss = { detailRow = null },
            )
        }
        linkFor?.let { (t, what) ->
            // Candidates nearest first: a refund comes after its purchase (within two months), a purchase before its
            // refund; a duplicate is the same amount the same way within three days.
            val cands = remember(rows, t.id, what) {
                fun days(a: String, b: String) = kotlin.math.abs(Dates.daysBetween(a, b))
                when (what) {
                    "refund" -> rows.filter { it.direction == Dir.CREDIT && it.date >= t.date && Dates.daysBetween(t.date, it.date) <= 60 && it.kind != Kind.IGNORE }
                    "purchase" -> rows.filter { it.direction == Dir.DEBIT && it.kind == Kind.EXPENSE && it.date <= t.date && Dates.daysBetween(it.date, t.date) <= 60 }
                    else -> rows.filter { it.id != t.id && it.direction == t.direction && it.paise == t.paise && days(it.date, t.date) <= 3 && it.kind != Kind.IGNORE }
                }.sortedWith(compareBy<Row> { if (it.counterparty != null && it.counterparty == t.counterparty) 0 else 1 }
                    .thenBy { kotlin.math.abs(it.paise - t.paise) }.thenBy { days(it.date, t.date) }).take(25)
            }
            Dialog(onDismissRequest = { linkFor = null }) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(18.dp).heightIn(max = 620.dp)
                    .verticalScroll(rememberScrollState())) {
                    Text(stringResource(when (what) { "refund" -> R.string.link_refund; "purchase" -> R.string.link_purchase; else -> R.string.link_duplicate }),
                        color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    MutedText(niceName(t.counterparty ?: t.category) + " · " + money(t.amount) + " · " + prettyDate(t.date), 12)
                    Spacer(Modifier.height(8.dp))
                    if (cands.isEmpty()) MutedText(stringResource(R.string.link_none), 13)
                    cands.forEach { c ->
                        TxnRow(c, false) { picked ->
                            when (what) {
                                "refund" -> linkRefund(picked, t)
                                "purchase" -> linkRefund(t, picked)
                                else -> linkDuplicate(t, picked)
                            }
                            linkFor = null
                        }
                    }
                }
            }
        }
        newCatCallback?.let { cb ->
            NewCategoryDialog(
                onCreate = { name, emoji, color ->
                    scope.launch {
                        withContext(Dispatchers.IO) { Storage.addCustomCat(context, name, emoji, color) }
                        CustomCats.map = withContext(Dispatchers.IO) { Storage.loadCustomCats(context) }
                        cb(name)
                        newCatCallback = null
                    }
                },
                onDismiss = { newCatCallback = null },
            )
        }
    } }
}
