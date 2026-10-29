package com.koolicream.zelu

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
                    Text("Monthly budget", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    MutedText("Your monthly limit. 0 for none.", 12)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = text, onValueChange = { v -> text = v.filter { it.isDigit() || it == '.' } },
                        singleLine = true, placeholder = { Text("₹ per month (0 = none)") },
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
                    ) { Text("Save", color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
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
                    Text("Card limit", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    MutedText("Your bank's messages for " + acct + " don't state the available limit. Enter what your bank app shows now. If a later message states it, Zelu uses that instead.", 12)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(value = txt, onValueChange = { v -> txt = v.filter { it.isDigit() || it == '.' } }, singleLine = true,
                        placeholder = { Text("Available limit ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        Text("Cancel", color = ZColor.Muted, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { limitAskFor = null }.padding(12.dp))
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.clip(RoundedCornerShape(20.dp)).background(ZColor.Ink).clickable {
                            val v = txt.toDoubleOrNull() ?: 0.0
                            if (v > 0) scope.launch {
                                withContext(Dispatchers.IO) { Storage.setManualLimit(context, acct, v) }
                                limits = withContext(Dispatchers.IO) { Storage.loadLimits(context) }
                                manualLimits = withContext(Dispatchers.IO) { Storage.manualLimitKeys(context) }
                                limitAskFor = null
                            }
                        }.padding(horizontal = 22.dp, vertical = 10.dp)) { Text("Save", color = ZColor.Paper, fontWeight = FontWeight.SemiBold) }
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
                Text("Categories", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KindTab("spending", "Spending", catTab == 0) { catTab = 0 }
                    KindTab("income", "Income", catTab == 1) { catTab = 1 }
                }
                Spacer(Modifier.height(10.dp))
                if (catTab == 1) IncomeCategoriesBody() else {
                MutedText("Tap to count a category as spending or leave it out of totals.", 12)
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
                    Text("Close", color = ZColor.Paper, fontWeight = FontWeight.SemiBold)
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
                    Text("Split payment", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    MutedText((t.counterparty ?: t.category) + " · " + money(t.amount) + ". Give each part an amount and a category.", 12)
                    Spacer(Modifier.height(10.dp))
                    for (i in amts.indices) {
                        DropdownField("Part ${i + 1}", cats[i], CAT_OPTS.drop(1), catIcons = true, valueKey = cats[i]) { cats[i] = it }
                        OutlinedTextField(value = amts[i], onValueChange = { v -> amts[i] = v.filter { it.isDigit() || it == '.' } }, singleLine = true,
                            placeholder = { Text("Amount ₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                    }
                    Text(if (Math.abs(left) < 0.5) "All assigned" else if (left > 0) money(left) + " left to assign" else money(-left) + " too much",
                        color = if (Math.abs(left) < 0.5) ZColor.Income else ZColor.Warn, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        if (amts.size < 5) Text("＋ Add part", color = ZColor.Accent, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { amts.add(if (left > 0) fmtPlain(left) else ""); cats.add(t.category) }.padding(vertical = 8.dp))
                        else Spacer(Modifier.width(1.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Cancel", color = ZColor.Muted, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { splitFor = null }.padding(12.dp))
                            Spacer(Modifier.width(6.dp))
                            Box(Modifier.clip(RoundedCornerShape(20.dp)).background(if (ok) ZColor.Ink else ZColor.Line).clickable {
                                if (ok) {
                                    val parts = amts.indices.map { (amts[it].toDoubleOrNull() ?: 0.0) to cats[it] }
                                    scope.launch {
                                        withContext(Dispatchers.IO) { Storage.setSplit(context, t.id, parts) }
                                        refreshRows()
                                        splitFor = null
                                        Toast.makeText(context, "Split into ${parts.size} parts", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }.padding(horizontal = 22.dp, vertical = 10.dp)) { Text("Save", color = if (ok) ZColor.Paper else ZColor.Muted, fontWeight = FontWeight.SemiBold) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun DuplicatesDialog(s: AppState) {
    with(s) {
        if (dupOpen) {
            val pairs = Analytics.possibleDuplicates(rows, dupOk)
            if (pairs.isNotEmpty()) Dialog(onDismissRequest = { dupOpen = false }) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(ZColor.Card).padding(20.dp).heightIn(max = 620.dp)) {
                    Text("Possible duplicates", color = ZColor.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    MutedText("Same amount, account and day. Hide the extra if it's one payment.", 12)
                    Spacer(Modifier.height(10.dp))
                    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                        pairs.forEach { (a, b) ->
                            Column(Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(12.dp)).background(ZColor.Paper).padding(12.dp)) {
                                listOf(a, b).forEach { r ->
                                    Text((r.counterparty ?: r.category) + " · " + money(r.amount), color = ZColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    MutedText(prettyDate(r.date) + " · " + r.account + " · from " + r.source, 11)
                                    Spacer(Modifier.height(3.dp))
                                    Text(r.raw, color = ZColor.InkSoft, fontSize = 12.sp, maxLines = 6, overflow = TextOverflow.Ellipsis)
                                    Spacer(Modifier.height(8.dp))
                                }
                                Spacer(Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                                    Text("Hide the extra one", color = ZColor.Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
                                        fun info(r: Row) = (if (!r.counterparty.isNullOrBlank()) 1 else 0) + (if (!r.ref.isNullOrBlank()) 1 else 0)
                                        val drop = if (info(b) > info(a)) a else b
                                        decide(drop.id, Kind.IGNORE, "Duplicate", null, false)
                                    })
                                    Text("Keep both", color = ZColor.Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable {
                                        scope.launch {
                                            withContext(Dispatchers.IO) { Storage.markDupOk(context, a.id + "|" + b.id) }
                                            dupOk = withContext(Dispatchers.IO) { Storage.dupOk(context) }
                                        }
                                    })
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.align(Alignment.End).clip(RoundedCornerShape(20.dp)).background(ZColor.Ink)
                        .clickable { dupOpen = false }.padding(horizontal = 22.dp, vertical = 10.dp)) {
                        Text("Close", color = ZColor.Paper, fontWeight = FontWeight.SemiBold)
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
        if (budgetsOpen) {
            val spendCats = Analytics.byCategory(list).map { it.first }
            val allCats = (fixed.keys + spendCats + budgets.keys + Classifier.CATEGORIES.keys + CustomCats.names).distinct()
            BudgetsDialog(allCats, budgets, fixed, onSet = { c, a -> setBudget(c, a) }, onFixed = { c, a -> setFixed(c, a) },
                onDismiss = { budgetsOpen = false })
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
            val r = rows.firstOrNull { it.id == opened.id } ?: opened
            TxnDetailDialog(
                r,
                onReclassify = { kind, cat -> decide(r.id, kind, cat, r.counterparty, kind != Kind.INCOME && kind != Kind.IGNORE) },
                onNewCat = { cb -> newCatCallback = cb },
                onRetitle = { title -> retitle(r.id, title) },
                onSplit = if (r.kind == Kind.EXPENSE || r.id.contains('#')) ({
                    detailRow = null
                    if (r.id.contains('#')) {
                        scope.launch {
                            withContext(Dispatchers.IO) { Storage.setSplit(context, r.id.substringBefore('#'), null) }
                            refreshRows()
                            Toast.makeText(context, "Split removed", Toast.LENGTH_SHORT).show()
                        }
                    } else splitFor = r
                }) else null,
                onDismiss = { detailRow = null },
            )
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
